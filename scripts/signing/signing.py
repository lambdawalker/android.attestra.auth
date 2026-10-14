#!/usr/bin/env python3
# /// script
# requires-python = ">=3.10"
# dependencies = ["rich==14.1.0"]
# ///
"""Local signing setup and public association verification; no private-key upload."""
import argparse
import base64
import getpass
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import urllib.request

RELATIONS = {"delegate_permission/common.handle_all_urls", "delegate_permission/common.get_login_creds"}


def run(args, env=None):
    result = subprocess.run([str(a) for a in args], env=env, capture_output=True, text=True, timeout=120)
    if result.returncode:
        raise RuntimeError(f"{Path(str(args[0])).name} failed (exit {result.returncode}); check the path, alias, password and installed tools")
    return result.stdout


def fingerprint(value):
    value = value.strip().replace(":", "").upper()
    if not re.fullmatch(r"[0-9A-F]{64}", value):
        raise ValueError("Expected a SHA-256 certificate fingerprint")
    return ":".join(value[i:i+2] for i in range(0, 64, 2))


def certificate_fingerprint(output):
    # JVM startup warnings can precede stdout; hash only the PEM certificate bytes.
    match = re.search(rb"-----BEGIN CERTIFICATE-----\s*(.*?)\s*-----END CERTIFICATE-----", output, re.S)
    if not match:
        raise ValueError("keytool did not return a PEM certificate")
    der = base64.b64decode(re.sub(rb"\s+", b"", match[1]), validate=True)
    return fingerprint(hashlib.sha256(der).hexdigest())


def password_env():
    env = os.environ.copy()
    if not env.get("ATTESTRA_KEYSTORE_PASSWORD"):
        env["ATTESTRA_KEYSTORE_PASSWORD"] = getpass.getpass("Keystore password (hidden): ")
    if len(env["ATTESTRA_KEYSTORE_PASSWORD"]) < 6:
        raise ValueError("Keystore password must have at least six characters; use a long unique password")
    return env


def sdk_candidates():
    """Read only sdk.dir; never expose signing settings from local.properties."""
    candidates = [os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT")]
    properties = Path(__file__).resolve().parents[2] / "local.properties"
    if properties.is_file():
        for line in properties.read_text(encoding="utf-8").splitlines():
            match = re.match(r"^\s*sdk\.dir\s*[=:]\s*(.*)$", line)
            if match:
                # Java properties escape Windows separators, colons and spaces.
                value = re.sub(r"\\(u[0-9a-fA-F]{4}|.)", lambda m: chr(int(m[1][1:], 16)) if m[1].startswith("u") and len(m[1]) == 5 else {"t": "\t", "n": "\n", "r": "\r", "f": "\f"}.get(m[1], m[1]), match[1])
                path = Path(value).expanduser()
                candidates.append(str(path if path.is_absolute() else properties.parent / path))
    if os.environ.get("LOCALAPPDATA"):
        candidates.append(str(Path(os.environ["LOCALAPPDATA"]) / "Android/Sdk"))
    candidates.extend(str(Path.home() / p) for p in ("AppData/Local/Android/Sdk", "Library/Android/sdk", "Android/Sdk"))
    return list(dict.fromkeys(p for p in candidates if p))


def sdk_tool(name, sdk=None):
    suffix = ".bat" if os.name == "nt" and name == "apksigner" else ".exe" if os.name == "nt" else ""
    for home in [sdk] if sdk is not None else sdk_candidates():
        paths = [p for p in (Path(home).expanduser() / "build-tools").glob("*/" + name + suffix) if p.is_file()]
        paths.sort(key=lambda p: tuple(int(n) for n in re.findall(r"\d+", p.parent.name)), reverse=True)
        if paths:
            return str(paths[0])
    found = shutil.which(name + suffix) if sdk is None else None
    if not found:
        raise ValueError(f"Cannot find {name}. Install Android SDK Build-Tools in Android Studio's SDK Manager; configure sdk.dir in local.properties or ANDROID_HOME. CLI users can pass --{name}.")
    return found


def export(args, generate=False, env=None):
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+", args.package):
        raise ValueError("Invalid Android application ID")
    path = Path(args.keystore).expanduser().resolve()
    if Path(args.output).expanduser().exists():
        raise ValueError("Output exists; choose a new path to avoid replacing an approval file")
    output_path = Path(args.output).expanduser().resolve()
    if not output_path.parent.is_dir():
        raise ValueError("Output directory does not exist")
    env = password_env() if env is None else env
    if generate:
        env = env.copy()
        store_type = getattr(args, "store_type", None) or ("PKCS12" if path.suffix.lower() in (".p12", ".pfx") else "JKS")
        env.setdefault("ATTESTRA_KEY_PASSWORD", env["ATTESTRA_KEYSTORE_PASSWORD"])
        if len(env["ATTESTRA_KEY_PASSWORD"]) < 6:
            raise ValueError("Key password must have at least six characters")
        if store_type == "PKCS12" and env["ATTESTRA_KEY_PASSWORD"] != env["ATTESTRA_KEYSTORE_PASSWORD"]:
            raise ValueError("keytool PKCS12 generation requires matching passwords. Use --store-type JKS and a .jks file for separate passwords.")
        if path.exists():
            raise ValueError("Keystore already exists; use export to preserve its signing identity")
        path.parent.mkdir(parents=True, exist_ok=True)
        run(["keytool", "-genkeypair", "-keystore", path, "-storetype", store_type, "-alias", args.alias,
             "-keyalg", "RSA", "-keysize", "3072", "-validity", "3650", "-dname", "CN=Attestra Development",
             "-storepass:env", "ATTESTRA_KEYSTORE_PASSWORD", "-keypass:env", "ATTESTRA_KEY_PASSWORD"], env)
        path.chmod(0o600)
    if not path.is_file():
        raise ValueError("Keystore does not exist")
    # Fingerprint the DER certificate bytes, independent of keytool's display language.
    result = subprocess.run(["keytool", "-exportcert", "-rfc", "-keystore", str(path), "-alias", args.alias,
                             "-storepass:env", "ATTESTRA_KEYSTORE_PASSWORD"], env=env, capture_output=True, timeout=120)
    if result.returncode:
        raise RuntimeError("Certificate export failed; check keystore, password and alias")
    digest = certificate_fingerprint(result.stdout)
    document = [{"packageName": args.package, "sha256CertFingerprints": [digest]}]
    with open(output_path, "x", encoding="utf-8") as output:
        json.dump(document, output, indent=2)
        output.write("\n")
    print(f"Public fingerprint: {digest}\nPublic association file: {args.output}")
    print("Request approval for the intended environment. Keep the keystore and password private.")


def save_public_copy(output, environment):
    """Keep an environment-specific public handoff, never a key or password."""
    if not re.fullmatch(r"[a-z][a-z0-9-]{0,62}", environment):
        raise ValueError("Invalid environment name")
    directory = Path.home() / ".attestra" / "associations" / environment
    directory.mkdir(parents=True, exist_ok=True)
    destination = directory / "developer-associations.json"
    data = Path(output).expanduser().read_bytes()
    if destination.exists() and destination.read_bytes() == data:
        return destination
    # Retain earlier exports, so replacing this handoff never loses a public identity.
    if destination.exists():
        old = destination.read_bytes()
        backup = directory / ("developer-associations-" + hashlib.sha256(old).hexdigest()[:16] + ".json")
        if not backup.exists():
            with backup.open("xb") as stream:
                stream.write(old)
    import tempfile
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(dir=directory, delete=False) as stream:
            temporary = Path(stream.name)
            stream.write(data)
        temporary.replace(destination)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)
    return destination


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def fetch(url):
    from urllib.parse import urlsplit
    u = urlsplit(url)
    if u.scheme != "https" or not u.hostname or u.username or u.password or u.query or u.fragment or u.path != "/.well-known/assetlinks.json":
        raise ValueError("Use the exact HTTPS app-host /.well-known/assetlinks.json URL")
    with urllib.request.build_opener(NoRedirect()).open(url, timeout=20) as response:
        if response.status != 200 or response.headers.get_content_type() != "application/json":
            raise ValueError("Association endpoint must return HTTP 200 application/json")
        data = response.read(1_048_577)
        if len(data) > 1_048_576:
            raise ValueError("Association file is too large")
        return json.loads(data)


def approved(document, package, digest):
    if not isinstance(document, list):
        return False
    relations = set()
    for item in document:
        if not isinstance(item, dict):
            continue
        target = item.get("target", {})
        if not isinstance(target, dict):
            continue
        fingerprints = target.get("sha256_cert_fingerprints", [])
        relation = item.get("relation", [])
        if not isinstance(fingerprints, list) or not all(isinstance(v, str) for v in fingerprints):
            continue
        if not isinstance(relation, list) or not all(isinstance(v, str) for v in relation):
            continue
        if target.get("namespace") == "android_app" and target.get("package_name") == package:
            values = {fingerprint(v) for v in fingerprints}
            if digest in values:
                relations.update(relation)
    return RELATIONS <= relations


def verify(args):
    expected = fingerprint(args.expected_sha256)
    certs = run([args.apksigner or sdk_tool("apksigner"), "verify", "--print-certs", args.apk])
    hashes = {fingerprint(v) for v in re.findall(r"^(?:V[1-4](?:\.\d+)? )?Signer[^\n]* certificate SHA-256 digest:\s*([0-9a-fA-F:]+)\s*$", certs, re.M)}
    if hashes != {expected}:
        observed = ", ".join(sorted(hashes)) or "no supported signer certificate found"
        raise ValueError(f"APK signing certificate differs from the approved expected fingerprint: expected {expected}; observed {observed}")
    badging = run([args.aapt or sdk_tool("aapt"), "dump", "badging", args.apk])
    match = re.search(r"^package: name='([^']+)'", badging, re.M)
    if not match or match[1] != args.package:
        raise ValueError("APK application ID differs from the expected package")
    if not approved(fetch(args.url), args.package, expected):
        raise ValueError("Published associations do not approve this APK for both App Links and credentials; deploy approved configuration first")
    print(f"Verified APK signature, package and published associations: {args.package}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=False)
    commands.add_parser("wizard", help="guided interactive signing setup (default)")
    for name in ("generate", "export"):
        p = commands.add_parser(name)
        if name == "generate":
            p.add_argument("--store-type", choices=["JKS", "PKCS12"], help="default: PKCS12 for .p12/.pfx; JKS otherwise")
        p.add_argument("--environment", help="also save a public copy under ~/.attestra/associations/<environment>/")
        p.add_argument("--keystore", required=True)
        p.add_argument("--alias", default="attestra")
        p.add_argument("--package", default="com.apexfission.android.attestra.auth")
        p.add_argument("--output", required=True, help="new public JSON file")
    p = commands.add_parser("verify-apk")
    p.add_argument("--apk", required=True)
    p.add_argument("--expected-sha256", required=True)
    p.add_argument("--package", default="com.apexfission.android.attestra.auth")
    p.add_argument("--url", required=True)
    p.add_argument("--apksigner")
    p.add_argument("--aapt")
    args = parser.parse_args()
    try:
        if args.command in (None, "wizard"):
            if not sys.stdin.isatty():
                parser.error("The wizard requires an interactive terminal; use generate, export or verify-apk for automation")
            try:
                from wizard import wizard
            except ModuleNotFoundError as error:
                if error.name != "rich":
                    raise
                raise RuntimeError("Run with uv run scripts/signing/signing.py to install the wizard dependencies") from None
            return wizard()
        if args.command == "verify-apk": verify(args)
        else:
            export(args, args.command == "generate")
            if args.environment:
                print(f"Public setup copy: {save_public_copy(args.output, args.environment)}")
    except (EOFError, KeyboardInterrupt):
        print("\nSigning setup cancelled; existing files are retained.", file=sys.stderr)
        return 130
    except (ValueError, RuntimeError, OSError, subprocess.TimeoutExpired) as error:
        print(f"Signing setup stopped: {error}", file=sys.stderr)
        return 1
    return 0

if __name__ == "__main__":
    sys.exit(main())
