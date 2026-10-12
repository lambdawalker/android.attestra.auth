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
        with patch.dict(os.environ, {}, clear=True), patch('wizard.Prompt.ask', side_effect=['secret-one', 'different', 'secret-two', 'secret-two']):
            env = self.ui.credentials(True)
            self.assertEqual(env['ATTESTRA_KEYSTORE_PASSWORD'], 'secret-two')
            self.assertNotIn('ATTESTRA_KEYSTORE_PASSWORD', os.environ)
        self.assertNotIn('secret-two', self.output.getvalue())

    def test_defaults_do_not_overwrite_public_files(self):
        with tempfile.TemporaryDirectory() as directory, patch.object(wizard, 'ROOT', Path(directory)):
            first = Path(wizard.new_output('qa'))
            first.touch()
            self.assertNotEqual(str(first), wizard.new_output('qa'))

    def test_malformed_associations_fail_closed(self):
        self.assertFalse(signing.approved([None, {'target': None}, {'target': {'sha256_cert_fingerprints': None}}], 'com.example.app', 'AB'))

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
