"""CI-only signature integration test using an ephemeral certificate and a fake website."""
import argparse
import json
import subprocess
from unittest.mock import patch
import signing

parser = argparse.ArgumentParser()
parser.add_argument('--associations', required=True)
parser.add_argument('--apk', required=True)
args = parser.parse_args()
with open(args.associations, encoding='utf-8') as source:
    approved = json.load(source)[0]
digest = approved['sha256CertFingerprints'][0]
namespace = argparse.Namespace(apk=args.apk, package=approved['packageName'], expected_sha256=digest,
                               url='https://qa.example.invalid/.well-known/assetlinks.json', apksigner=None, aapt=None)
document = [{'relation': sorted(signing.RELATIONS), 'target': {'namespace': 'android_app',
            'package_name': namespace.package, 'sha256_cert_fingerprints': [digest]}}]
# Certificates are public; retain actual SDK output to diagnose tool-version differences.
result = subprocess.run([signing.sdk_tool('apksigner'), 'verify', '--print-certs', args.apk], capture_output=True, text=True, check=True)
print('apksigner stdout:', repr(result.stdout), flush=True)
print('apksigner stderr:', repr(result.stderr), flush=True)
with patch.object(signing, 'fetch', return_value=document):
    signing.verify(namespace)
    namespace.expected_sha256 = '00' * 32
    try:
        signing.verify(namespace)
        raise AssertionError('Wrong certificate was accepted')
    except ValueError:
        pass
