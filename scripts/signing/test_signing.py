import unittest
from unittest.mock import patch
import signing

class SigningTests(unittest.TestCase):
    def test_export_ignores_jvm_stdout_warnings(self):
        import hashlib
        expected = signing.fingerprint(hashlib.sha256(b'certificate').hexdigest())
        self.assertEqual(expected, signing.certificate_fingerprint(b'[0.1s][warning] JVM warning\n-----BEGIN CERTIFICATE-----\nY2VydGlmaWNhdGU=\n-----END CERTIFICATE-----'))

    def test_fingerprint(self):
        self.assertEqual(signing.fingerprint('ab'*32), ':'.join(['AB']*32))
        with self.assertRaises(ValueError): signing.fingerprint('AB')

    def test_approval_matches_package_certificate_and_relations(self):
        fp=signing.fingerprint('ab'*32)
        doc=[{'relation':list(signing.RELATIONS),'target':{'namespace':'android_app','package_name':'com.example.qa','sha256_cert_fingerprints':[fp]}}]
        self.assertTrue(signing.approved(doc,'com.example.qa',fp))
        self.assertFalse(signing.approved(doc,'com.example.prod',fp))
        self.assertFalse(signing.approved(doc,'com.example.qa',signing.fingerprint('cd'*32)))
        doc[0]['relation']=['delegate_permission/common.handle_all_urls']
        self.assertFalse(signing.approved(doc,'com.example.qa',fp))
        self.assertFalse(signing.approved([], 'com.example.qa',fp))

    def test_rejects_non_https_and_redirect(self):
        with self.assertRaises(ValueError): signing.fetch('http://dev.example.com/.well-known/assetlinks.json')
        self.assertIsNone(signing.NoRedirect().redirect_request(None,None,302,'',{},'https://elsewhere.example'))

if __name__=='__main__': unittest.main()
