import argparse
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from rich.console import Console
import signing
import wizard


class WizardTests(unittest.TestCase):
    def setUp(self):
        self.output = io.StringIO()
        self.ui = wizard.Wizard(Console(file=self.output, width=100, markup=False))

    def test_menu_lists_every_action_and_exits(self):
        with patch('wizard.Prompt.ask', return_value='0'):
            self.assertEqual(self.ui.run(), 0)
        for label in ('Generate', 'Export', 'Verify APK', 'Android Studio', 'Exit'):
            self.assertIn(label, self.output.getvalue())

    def test_cancel_action_returns_to_menu(self):
        with patch('wizard.Prompt.ask', side_effect=['1', '0']), patch.object(self.ui, 'export', side_effect=KeyboardInterrupt):
            self.assertEqual(self.ui.run(), 0)
        self.assertIn('cancelled', self.output.getvalue())

    def test_action_error_can_be_retried(self):
        with patch('wizard.Prompt.ask', side_effect=['2', '0']), patch.object(self.ui, 'export', side_effect=ValueError('Wrong alias')):
            self.assertEqual(self.ui.run(), 0)
        self.assertIn('Wrong alias', self.output.getvalue())

    def test_password_confirmation_retries_without_persisting(self):
        with patch.dict(os.environ, {}, clear=True), patch('wizard.Prompt.ask', side_effect=['secret-one', 'different', 'secret-two', 'secret-two', 'key-secret', 'key-secret']):
            env = self.ui.credentials(True)
            self.assertEqual(env['ATTESTRA_KEYSTORE_PASSWORD'], 'secret-two')
            self.assertEqual(env['ATTESTRA_KEY_PASSWORD'], 'key-secret')
            self.assertNotIn('ATTESTRA_KEYSTORE_PASSWORD', os.environ)
        self.assertNotIn('secret-two', self.output.getvalue())

    def test_defaults_do_not_overwrite_public_files(self):
        with tempfile.TemporaryDirectory() as directory, patch.object(wizard, 'ROOT', Path(directory)):
            first = Path(wizard.new_output('qa'))
            first.touch()
            self.assertNotEqual(str(first), wizard.new_output('qa'))

    def test_malformed_associations_fail_closed(self):
        self.assertFalse(signing.approved([None, {'target': None}, {'target': {'sha256_cert_fingerprints': None}}], 'com.example.app', 'AB'))

    def test_sdk_properties_unescapes_windows_path(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ, {}, clear=True):
            root = Path(directory)
            (root / 'local.properties').write_text(r'sdk.dir=C\:\\Users\\David\\Android\\Sdk' + '\n')
            with patch.object(signing, '__file__', str(root / 'scripts/signing/signing.py')):
                self.assertIn(r'C:\Users\David\Android\Sdk', signing.sdk_candidates()[0])

    def test_sdk_falls_back_from_stale_environment_to_properties(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(os.environ, {'ANDROID_HOME': '/missing-sdk'}, clear=True):
            root = Path(directory)
            sdk = root / 'sdk'
            tools = sdk / 'build-tools/35.0.0'
            tools.mkdir(parents=True)
            suffix = '.bat' if os.name == 'nt' else ''
            tool = tools / ('apksigner' + suffix)
            tool.touch()
            (root / 'local.properties').write_text('sdk.dir=sdk\n')
            with patch.object(signing, '__file__', str(root / 'scripts/signing/signing.py')):
                self.assertEqual(str(tool), signing.sdk_tool('apksigner'))

    def test_sdk_prompt_retries_and_remembers_valid_folder(self):
        with patch('wizard.Prompt.ask', side_effect=['bad', '/sdk']), patch('signing.sdk_tool', side_effect=[ValueError(), ValueError(), '/sdk/apksigner', '/sdk/aapt']):
            self.assertEqual(('/sdk/apksigner', '/sdk/aapt'), self.ui.sdk_tools())
            self.assertEqual('/sdk', self.ui.sdk)
        self.assertIn('SDK root folder', ' '.join(self.output.getvalue().split()))

    def test_public_copy_is_environment_scoped_and_preserves_previous(self):
        with tempfile.TemporaryDirectory() as directory, patch('pathlib.Path.home', return_value=Path(directory)):
            source = Path(directory) / 'public.json'
            source.write_text('[{"packageName":"com.example.app"}]')
            dev = signing.save_public_copy(source, 'dev')
            self.assertEqual(dev, Path(directory) / '.attestra/associations/dev/developer-associations.json')
            original = dev.read_bytes()
            source.write_text('[]')
            qa = signing.save_public_copy(source, 'qa')
            self.assertEqual(original, dev.read_bytes())
            signing.save_public_copy(source, 'dev')
            self.assertEqual(b'[]', dev.read_bytes())
            self.assertEqual(1, len(list(dev.parent.glob('developer-associations-*.json'))))
            with self.assertRaises(ValueError):
                signing.save_public_copy(source, '../prod')

    def test_distinct_jks_passwords_unlock_real_private_key(self):
        with tempfile.TemporaryDirectory() as directory:
            key = Path(directory) / 'test.jks'
            args = argparse.Namespace(keystore=str(key), output=str(Path(directory) / 'public.json'), alias='attestra', package=wizard.PACKAGE, store_type='JKS')
            env = dict(os.environ, ATTESTRA_KEYSTORE_PASSWORD='store-test-password', ATTESTRA_KEY_PASSWORD='key-test-password')
            signing.export(args, True, env)
            command = ['keytool', '-certreq', '-keystore', key, '-alias', 'attestra', '-storepass:env', 'ATTESTRA_KEYSTORE_PASSWORD', '-keypass:env', 'ATTESTRA_KEY_PASSWORD']
            self.assertIn('CERTIFICATE REQUEST', signing.run(command, env))
            with self.assertRaises(RuntimeError):
                signing.run(command, dict(env, ATTESTRA_KEY_PASSWORD='wrong-password'))

    def test_pkcs12_rejects_distinct_passwords_before_creating_key(self):
        with tempfile.TemporaryDirectory() as directory:
            key = Path(directory) / 'test.p12'
            args = argparse.Namespace(keystore=str(key), output=str(Path(directory) / 'public.json'), alias='attestra', package=wizard.PACKAGE)
            env = dict(os.environ, ATTESTRA_KEYSTORE_PASSWORD='store-test-password', ATTESTRA_KEY_PASSWORD='key-test-password')
            with self.assertRaisesRegex(ValueError, 'PKCS12'):
                signing.export(args, True, env)
            self.assertFalse(key.exists())

    def test_real_key_generation_export_and_overwrite_protection(self):
        with tempfile.TemporaryDirectory() as directory:
            key = Path(directory) / 'test.p12'
            output = Path(directory) / 'public.json'
            args = argparse.Namespace(keystore=str(key), output=str(output), alias='attestra', package=wizard.PACKAGE)
            env = dict(os.environ, ATTESTRA_KEYSTORE_PASSWORD='temporary-test-password')
            signing.export(args, True, env)
            original = key.read_bytes()
            with self.assertRaises(ValueError):
                signing.export(args, True, env)
            self.assertEqual(original, key.read_bytes())
            args.output = str(Path(directory) / 'export.json')
            signing.export(args, False, env)
            self.assertEqual(json.loads(output.read_text()), json.loads(Path(args.output).read_text()))


if __name__ == '__main__':
    unittest.main()
