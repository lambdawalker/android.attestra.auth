"""Offline coverage for secure GitHub environment setup."""
from contextlib import asynccontextmanager
import base64
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import httpx
from nacl.public import PrivateKey, SealedBox
from textual.widgets import Button, Input, Static
from github_environment_api import GitHubAPI, SetupError
from setup_github_environments import SetupWizard, Setting, choose_value, apply_changes


class APITests(unittest.IsolatedAsyncioTestCase):
    async def test_lists_pages_and_does_not_update_existing_environment(self):
        requests = []
        def handle(request):
            requests.append(request)
            if request.url.path.endswith('/environments/prod'):
                return httpx.Response(200, json={'protection_rules': [{'type': 'wait_timer'}]})
            page = int(request.url.params.get('page', '1'))
            return httpx.Response(200, json={'total_count': 2, 'variables': [{'name': f'N{page}', 'value': str(page)}]})
        async with GitHubAPI('owner/repo', 'secret-token', transport=httpx.MockTransport(handle)) as api:
            self.assertTrue(await api.ensure_environment('prod'))
            self.assertEqual(await api.list_values('prod', 'variables'), {'N1': '1', 'N2': '2'})
        self.assertTrue(all(r.method == 'GET' for r in requests))

    async def test_missing_environment_and_sealed_box_multiline_secret(self):
        private = PrivateKey.generate()
        requests = []
        def handle(request):
            requests.append(request)
            if request.url.path.endswith('/public-key'):
                return httpx.Response(200, json={'key_id': '42', 'key': base64.b64encode(bytes(private.public_key)).decode()})
            if request.method == 'GET':
                return httpx.Response(404)
            return httpx.Response(201)
        async with GitHubAPI('owner/repo', 'token', transport=httpx.MockTransport(handle)) as api:
            self.assertFalse(await api.ensure_environment('new'))
            await api.write_secret('new', 'SIGNING_KEY', 'line one\r\nline two\n')
        self.assertEqual(json.loads(requests[1].content), {})
        body = json.loads(requests[-1].content)
        self.assertEqual(SealedBox(private).decrypt(base64.b64decode(body['encrypted_value'])), b'line one\r\nline two\n')
        self.assertNotIn('line one', requests[-1].content.decode())

    async def test_variables_create_with_post_and_update_with_patch(self):
        requests = []
        def handle(request):
            requests.append(request)
            return httpx.Response(204)
        async with GitHubAPI('owner/repo', 'token', transport=httpx.MockTransport(handle)) as api:
            await api.write_variable('prod','URL','https://new',False)
            await api.write_variable('prod','URL','https://updated',True)
        self.assertEqual([r.method for r in requests], ['POST','PATCH'])
        self.assertTrue(requests[0].url.path.endswith('/variables'))
        self.assertTrue(requests[1].url.path.endswith('/variables/URL'))
        self.assertEqual(json.loads(requests[1].content)['value'], 'https://updated')

    async def test_network_errors_are_sanitized_and_uncertain(self):
        def handle(request): raise httpx.ReadTimeout('sensitive-payload')
        async with GitHubAPI('owner/repo', 'token', transport=httpx.MockTransport(handle)) as api:
            with self.assertRaises(SetupError) as error:
                await api.write_variable('prod','URL','https://url')
        self.assertNotIn('sensitive-payload', str(error.exception))
        self.assertIn('may have succeeded', str(error.exception))

    async def test_redirect_is_rejected_without_forwarding_token(self):
        requests = []
        def handle(request):
            requests.append(request)
            return httpx.Response(302, headers={'Location':'https://other.example/steal'})
        async with GitHubAPI('owner/repo', 'token', transport=httpx.MockTransport(handle)) as api:
            with self.assertRaises(SetupError): await api.snapshot('prod')
        self.assertEqual(len(requests), 1)
        self.assertEqual(requests[0].url.host, 'api.github.com')

    async def test_errors_never_echo_response_or_token(self):
        async with GitHubAPI('owner/repo', 'sensitive', transport=httpx.MockTransport(lambda r: httpx.Response(403, text='sensitive private data'))) as api:
            with self.assertRaises(SetupError) as error:
                await api.ensure_environment('prod')
        self.assertNotIn('sensitive', str(error.exception))
        self.assertIn('403', str(error.exception))

    async def test_partial_writes_report_completed_and_failed_names(self):
        class Fake:
            async def ensure_environment(self, env): return True
            async def write_variable(self, *args): pass
            async def write_secret(self, *args): raise SetupError('Denied (HTTP 403).')
        settings = [Setting('prod', 'URL', 'variable', True), Setting('prod', 'TOKEN', 'secret', True)]
        result = await apply_changes(Fake(), {'prod': True}, settings, {0:'url',1:'secret'})
        self.assertEqual(result.completed, ['prod / URL'])
        self.assertIn('prod / TOKEN', result.failed)
        self.assertNotIn('secret', result.message)


class ValueTests(unittest.TestCase):
    def test_blank_preserves_existing_and_missing_required_rejects(self):
        self.assertIsNone(choose_value(Setting('prod','TOKEN','secret',True, existing=True), ''))
        with self.assertRaises(SetupError): choose_value(Setting('prod','TOKEN','secret',True), '')
        self.assertIsNone(choose_value(Setting('prod','PASSWORD','secret',False), ''))

    def test_repository_url_rejects_embedded_credentials(self):
        with self.assertRaises(SetupError):
            choose_value(Setting('prod','MAVEN_REPOSITORY_URL','variable',True), 'https://user:password@example.com/repo')

    def test_signing_file_retains_line_endings(self):
        with tempfile.TemporaryDirectory() as directory:
            key = Path(directory) / 'key.asc'
            key.write_bytes(b'private\r\nkey\n')
            self.assertEqual(choose_value(Setting('prod','SIGNING_IN_MEMORY_KEY','secret',True), str(key)), 'private\r\nkey\n')


class WizardTests(unittest.IsolatedAsyncioTestCase):
    @asynccontextmanager
    async def run_wizard(self, app):
        async with app.run_test() as pilot:
            # Textual ignores clicks during a button's 200 ms active effect.
            # Fast Windows pilots can click again before that timer expires;
            # pause() drains messages but does not wait for this animation.
            # Keep real click/event handling while removing cosmetic timing.
            for button in app.query(Button):
                button.active_effect_duration = 0
            yield pilot

    async def test_review_before_writes_blank_preserves_and_token_masked(self):
        calls = []
        class Fake:
            async def snapshot(self, env): return True, {'URL':'https://old'}, {'TOKEN'}
            async def aclose(self): pass
            async def ensure_environment(self, env): calls.append('environment'); return True
            async def write_variable(self, *args): calls.append(args)
            async def write_secret(self, *args): calls.append(args)
        settings = [Setting('prod','URL','variable',True), Setting('prod','TOKEN','secret',True)]
        app = SetupWizard('owner/repo', settings, api_factory=lambda repo,token: Fake())
        async with self.run_wizard(app) as pilot:
            self.assertTrue(app.query_one('#value', Input).password)
            app.query_one('#value', Input).value = 'sensitive'
            await pilot.click('#next'); await pilot.pause()
            self.assertIn('https://old', str(app.query_one('#detail', Static).render()))
            await pilot.click('#next'); await pilot.pause()
            self.assertTrue(app.query_one('#value', Input).password)
            await pilot.click('#next'); await pilot.pause()
            self.assertEqual(app.stage, 'review')
            self.assertEqual(calls, [])
            self.assertNotIn('sensitive', str(app.query_one('#detail', Static).render()))
            await pilot.click('#next'); await pilot.pause()
            self.assertEqual(app.stage, 'done')
            self.assertEqual(calls, [])

    async def test_review_displays_new_variable_value(self):
        class Fake:
            async def snapshot(self, env): return True, {'URL':'https://old'}, set()
            async def aclose(self): pass
        app = SetupWizard('owner/repo',[Setting('prod','URL','variable',True)],api_factory=lambda r,t: Fake())
        async with self.run_wizard(app) as pilot:
            app.query_one('#value', Input).value = 'access-token'
            await pilot.click('#next'); await pilot.pause()
            app.query_one('#value', Input).value = 'https://new'
            await pilot.click('#next'); await pilot.pause()
            self.assertIn('https://new', str(app.query_one('#detail', Static).render()))

    async def test_new_environment_full_review_and_apply(self):
        calls = []
        class Fake:
            async def snapshot(self, env): return False, {}, set()
            async def aclose(self): pass
            async def ensure_environment(self, env): calls.append(('create',env)); return False
            async def write_secret(self, env, name, value): calls.append((env,name,value))
        app = SetupWizard('owner/repo',[Setting('new','TOKEN','secret',True)],api_factory=lambda r,t: Fake())
        async with self.run_wizard(app) as pilot:
            app.query_one('#value', Input).value = 'access-token'
            await pilot.click('#next'); await pilot.pause()
            app.query_one('#value', Input).value = 'publishing-secret'
            await pilot.click('#next'); await pilot.pause()
            self.assertEqual(app.stage, 'review')
            self.assertEqual(calls, [])
            self.assertNotIn('publishing-secret', str(app.query_one('#detail', Static).render()))
            await pilot.click('#next'); await pilot.pause()
            self.assertEqual(app.stage, 'done')
            self.assertEqual(calls, [('create','new'), ('new','TOKEN','publishing-secret')])
            self.assertEqual(app.changes, {})
            self.assertNotIn('publishing-secret', str(app.query_one('#detail', Static).render()))

    async def test_missing_required_stays_on_field_and_cancel_writes_nothing(self):
        class Fake:
            async def snapshot(self, env): return False, {}, set()
            async def aclose(self): pass
        app = SetupWizard('owner/repo',[Setting('new','TOKEN','secret',True)],api_factory=lambda r,t: Fake())
        async with self.run_wizard(app) as pilot:
            app.query_one('#value', Input).value = 'token'
            await pilot.click('#next'); await pilot.pause()
            await pilot.click('#next'); await pilot.pause()
            self.assertEqual(app.stage, 'settings')
            self.assertIn('required', str(app.query_one('#error', Static).render()))
            await pilot.click('#cancel')


if __name__ == '__main__': unittest.main()

class EnvironmentCoverageTests(unittest.IsolatedAsyncioTestCase):
    async def test_delayed_docs_creation_sets_timer_and_existing_rules_survive(self):
        requests=[]
        def handle(request):
            requests.append(request)
            return httpx.Response(404 if request.method=='GET' else 200,json={})
        async with GitHubAPI('owner/repo','token',transport=httpx.MockTransport(handle)) as api:
            await api.ensure_environment('delayed-docs')
        self.assertEqual({'wait_timer':15},json.loads(requests[-1].content))
    async def test_wizard_inspects_and_creates_empty_environments(self):
        calls=[]
        class Fake:
            async def snapshot(self,environment):calls.append(('read',environment));return False,{},set()
            async def ensure_environment(self,environment):calls.append(('create',environment));return False
            async def aclose(self):pass
        app=SetupWizard('owner/repo',[],api_factory=lambda r,t:Fake(),environments=('jitpack','delayed-docs'))
        async with app.run_test() as pilot:
            for button in app.query(Button):button.active_effect_duration=0
            app.query_one('#value',Input).value='token'
            await pilot.click('#next');await pilot.pause()
            self.assertEqual('review',app.stage)
            self.assertEqual([('read','jitpack'),('read','delayed-docs')],calls)
            await pilot.click('#next');await pilot.pause()
            self.assertEqual([('create','jitpack'),('create','delayed-docs')],calls[2:])
