"""Small, non-logging GitHub environment API client; secrets stay in memory."""
from base64 import b64decode, b64encode
import re
from urllib.parse import quote

import httpx
from nacl.public import PublicKey, SealedBox


class SetupError(Exception):
    """A safe, human-readable error containing no remote response body."""


class GitHubAPI:
    def __init__(self, repository, token, *, transport=None):
        if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repository):
            raise SetupError('Repository must be owner/name.')
        self.root = f'/repos/{repository}'
        self.client = httpx.AsyncClient(
            base_url='https://api.github.com', transport=transport, timeout=30,
            follow_redirects=False,
            headers={'Authorization': f'Bearer {token}', 'Accept': 'application/vnd.github+json',
                     'X-GitHub-Api-Version': '2022-11-28'},
        )

    async def __aenter__(self): return self

    async def __aexit__(self, *_): await self.aclose()

    async def aclose(self):
        self.client.headers.pop('Authorization', None)
        await self.client.aclose()

    def environment_path(self, environment):
        return f'{self.root}/environments/{quote(environment, safe="")}'

    async def request(self, method, path, *, allow_missing=False, **kwargs):
        try:
            response = await self.client.request(method, path, **kwargs)
        except httpx.HTTPError:
            raise SetupError('GitHub could not be reached. A write may have succeeded; rerun to inspect current state.') from None
        if allow_missing and response.status_code == 404:
            return None
        if not 200 <= response.status_code < 300:
            hints = {401: 'Check the token.', 403: 'Check token permissions, organization approval/SSO, and rate limits.',
                     404: 'Check repository access and environment availability.',
                     422: 'Check the setting and repository environment support.'}
            raise SetupError(f'GitHub request failed (HTTP {response.status_code}). '
                             + hints.get(response.status_code, 'Retry after checking GitHub availability.'))
        if not response.content: return {}
        try:
            return response.json()
        except ValueError:
            raise SetupError('GitHub returned an unexpected response.') from None

    async def list_values(self, environment, kind):
        result = {}
        page = 1
        while True:
            data = await self.request('GET', f'{self.environment_path(environment)}/{kind}',
                                      params={'per_page': 30, 'page': page})
            rows = data.get(kind, [])
            for row in rows:
                result[row['name']] = row.get('value') if kind == 'variables' else None
            if not rows or len(result) >= data.get('total_count', len(result)):
                return result
            page += 1

    async def snapshot(self, environment):
        exists = await self.request('GET', self.environment_path(environment), allow_missing=True)
        if exists is None: return False, {}, set()
        variables = await self.list_values(environment, 'variables')
        secrets = await self.list_values(environment, 'secrets')
        return True, variables, set(secrets)

    async def ensure_environment(self, environment):
        # Recheck immediately before creation. Never PUT an existing environment:
        # omitted protection fields can reset existing protection settings.
        path = self.environment_path(environment)
        if await self.request('GET', path, allow_missing=True) is not None:
            return True
        await self.request('PUT', path, json={'wait_timer': 15} if environment == 'delayed-docs' else {})
        return False

    async def write_variable(self, environment, name, value, exists=False):
        path = f'{self.environment_path(environment)}/variables'
        if exists: path += '/' + quote(name, safe='')
        await self.request('PATCH' if exists else 'POST', path, json={'name': name, 'value': value})

    async def write_secret(self, environment, name, value):
        path = f'{self.environment_path(environment)}/secrets'
        key = await self.request('GET', path + '/public-key')
        try:
            encrypted = SealedBox(PublicKey(b64decode(key['key']))).encrypt(value.encode('utf-8'))
        except (ValueError, KeyError, TypeError):
            raise SetupError('GitHub returned an invalid encryption key.') from None
        await self.request('PUT', path + '/' + quote(name, safe=''),
                           json={'key_id': key['key_id'], 'encrypted_value': b64encode(encrypted).decode('ascii')})
