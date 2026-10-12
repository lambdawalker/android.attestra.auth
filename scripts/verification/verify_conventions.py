"""Exercise convention inputs against generated Android files and a signed APK."""
import base64
import re
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]


def gradle(*args, env=None, failure=None):
    result = subprocess.run(["bash", "gradlew", "--no-configuration-cache", *args],
                            cwd=ROOT, env=env, text=True, capture_output=True)
    if failure:
        assert result.returncode != 0 and failure in result.stdout + result.stderr
    elif result.returncode:
        raise RuntimeError(result.stdout + result.stderr)


def config():
    return (ROOT / "app/build/generated/source/buildConfig/debug/com/apexfission/android/attestra/auth/BuildConfig.java").read_text()


with tempfile.TemporaryDirectory() as tmp:
    exported = Path(tmp) / "qa.properties"
    exported.write_text("""attestraEnvironment=qa
attestraApiBaseUrl=https://qa.api.example.com
attestraLinkHost=qa.example.com
attestraAwsRegion=us-east-2
attestraCognitoUserPoolId=pool
attestraCognitoClientId=client
attestraCaptureEnabled=true
attestraCaptureDocumentType=sample_card
attestraIndexUrl=https://index.example.com/v1/environments
""")
    gradle(":app:assembleSignedDebug", failure="assembleSignedDebug")
    gradle(":app:generateDebugBuildConfig")
    assert 'ATTESTRA_ENVIRONMENT = "dev"' in config()
    assert 'AUTH_API_BASE_URL = ""' in config()
    gradle(":app:generateDebugBuildConfig", f"-PattestraConfigFile={exported}")
    assert 'ATTESTRA_ENVIRONMENT = "qa"' in config()
    assert 'AUTH_API_BASE_URL = "https://qa.api.example.com"' in config()
    assert 'CAPTURE_ENABLED = true' in config()
    gradle(":app:generateDebugBuildConfig", f"-PattestraConfigFile={exported}",
           "-PattestraEnvironment=dev", "-PattestraIndexUrl=https://override.example.com/index")
    assert 'ATTESTRA_ENVIRONMENT = "dev"' in config()
    assert 'ATTESTRA_INDEX_URL = "https://override.example.com/index"' in config()
    gradle(":app:generateDebugBuildConfig", "-PattestraCaptureEnabled=maybe",
           failure="attestraCaptureEnabled must be true or false")
    gradle(":app:generateDebugBuildConfig", "-PattestraIndexUrl=http://insecure.example.com",
           failure="attestraIndexUrl must be an HTTPS URL")
    env = os.environ.copy()
    env["ATTESTRA_KEYSTORE_PASSWORD"] = "throwaway-ci-only"
    env["ATTESTRA_KEYSTORE_PATH"] = str(Path(tmp) / "test.p12")
    subprocess.run(["keytool", "-genkeypair", "-keystore", env["ATTESTRA_KEYSTORE_PATH"],
                    "-storetype", "PKCS12", "-alias", "attestra", "-keyalg", "RSA",
                    "-keysize", "2048", "-validity", "1", "-dname", "CN=CI only",
                    "-storepass:env", "ATTESTRA_KEYSTORE_PASSWORD"], env=env, check=True,
                   capture_output=True)
    gradle(":app:signingReport", "--configuration-cache", env=env,
           failure="Registered signing requires --no-configuration-cache")
    missing = env.copy()
    del missing["ATTESTRA_KEYSTORE_PASSWORD"]
    gradle(":app:signingReport", env=missing, failure="Set ATTESTRA_KEYSTORE_PASSWORD")
    for name, value, message in [
        ("ATTESTRA_KEYSTORE_PASSWORD", "wrong-password", "Cannot open signing keystore"),
        ("ATTESTRA_KEY_ALIAS", "missing-alias", "ATTESTRA_KEY_ALIAS must identify"),
        ("ATTESTRA_KEY_PASSWORD", "wrong-password", "Cannot unlock signing key"),
    ]:
        invalid = env.copy()
        invalid[name] = value
        gradle(":app:assembleSignedDebug", env=invalid, failure=message)
    gradle(":app:assembleDebug", env=env)
    local = ROOT / "local.properties"
    original = local.read_bytes() if local.exists() else None
    try:
        prefix = original.decode('utf-8') + '\n' if original is not None else ''
        local.write_text(prefix + f"ATTESTRA_KEYSTORE_PATH={Path(env['ATTESTRA_KEYSTORE_PATH']).as_posix()}\n"
                         + "ATTESTRA_KEYSTORE_PASSWORD=throwaway-ci-only\nATTESTRA_KEY_ALIAS=attestra\n", encoding='utf-8')
        clean_env = {k: v for k, v in os.environ.items() if not k.startswith('ATTESTRA_KEY')}
        gradle(":app:assembleSignedDebug", env=clean_env)
        gradle(":app:signingReport", "--configuration-cache", env=clean_env,
               failure="Registered signing requires --no-configuration-cache")
        # Every environment field must override a conflicting local value.
        local.write_text(prefix + "ATTESTRA_KEYSTORE_PATH=missing-file.p12\n"
                         + "ATTESTRA_KEYSTORE_PASSWORD=wrong\nATTESTRA_KEY_ALIAS=wrong\n"
                         + "ATTESTRA_KEY_PASSWORD=wrong\n", encoding='utf-8')
        override = env.copy()
        override['ATTESTRA_KEY_ALIAS'] = 'attestra'
        override['ATTESTRA_KEY_PASSWORD'] = env['ATTESTRA_KEYSTORE_PASSWORD']
        gradle(":app:assembleDebug", env=override)
    finally:
        if original is None:
            local.unlink(missing_ok=True)
        else:
            local.write_bytes(original)
    sdk = Path(os.environ["ANDROID_HOME"])
    apksigners = sorted((sdk / "build-tools").glob("*/apksigner"))
    output = subprocess.check_output([str(apksigners[-1]), "verify", "--print-certs",
             "app/build/outputs/apk/signedDebug/app-signedDebug.apk"], cwd=ROOT, text=True)
    import hashlib
    cert = subprocess.check_output(["keytool", "-exportcert", "-rfc", "-keystore", env["ATTESTRA_KEYSTORE_PATH"],
             "-alias", "attestra", "-storepass:env", "ATTESTRA_KEYSTORE_PASSWORD"], env=env)
    pem = re.search(rb"-----BEGIN CERTIFICATE-----\s*(.*?)\s*-----END CERTIFICATE-----", cert, re.S)
    assert pem is not None
    der = base64.b64decode(re.sub(rb"\s+", b"", pem[1]), validate=True)
    assert hashlib.sha256(der).hexdigest() in output.lower()
print("Convention defaults, overrides, validation and signed APK verified")
