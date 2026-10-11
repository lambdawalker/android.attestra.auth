#!/usr/bin/env python3
"""Local signing setup and public association verification; no private-key upload."""
import argparse
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
    result = subprocess.run([str(a) for a in args], env=env, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(f"{Path(str(args[0])).name} failed (exit {result.returncode}); check the path, alias, password and installed tools")
    return result.stdout


def fingerprint(value):
    value = value.strip().replace(":", "").upper()
    if not re.fullmatch(r"[0-9A-F]{64}", value):
        raise ValueError("Expected a SHA-256 certificate fingerprint")
    return ":".join(value[i:i+2] for i in range(0, 64, 2))


def password_env():
    env = os.environ.copy()
    if not env.get("ATTESTRA_KEYSTORE_PASSWORD"):
        env["ATTESTRA_KEYSTORE_PASSWORD"] = getpass.getpass("Keystore password (hidden): ")
    if len(env["ATTESTRA_KEYSTORE_PASSWORD"]) < 6:
        raise ValueError("Keystore password must have at least six characters; use a long unique password")
    return env


def sdk_tool(name):
    home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    suffix = ".bat" if os.name == "nt" and name == "apksigner" else ".exe" if os.name == "nt" else ""
    paths = list((Path(home)/"build-tools").glob("*/" + name + suffix)) if home else []
    paths.sort(key=lambda p: tuple(int(n) for n in re.findall(r"\d+", p.parent.name)), reverse=True)
    found = str(paths[0]) if paths else shutil.which(name + suffix)
    if not found:
        raise ValueError(f"Install Android SDK Build Tools and set ANDROID_HOME, or pass --{name}")
    return found


def export(args, generate=False):
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+", args.package):
        raise ValueError("Invalid Android application ID")
    path = Path(args.keystore).expanduser().resolve()
    if Path(args.output).exists():
        raise ValueError("Output exists; choose a new path to avoid replacing an approval file")
    env = password_env()
    if generate:
        if path.exists():
            raise ValueError("Keystore already exists; use export to preserve its signing identity")
        path.parent.mkdir(parents=True, exist_ok=True)
        run(["keytool", "-genkeypair", "-keystore", path, "-storetype", "PKCS12", "-alias", args.alias,
             "-keyalg", "RSA", "-keysize", "3072", "-validity", "3650", "-dname", "CN=Attestra Development",
             "-storepass:env", "ATTESTRA_KEYSTORE_PASSWORD", "-keypass:env", "ATTESTRA_KEYSTORE_PASSWORD"], env)
        path.chmod(0o600)
    if not path.is_file():
        raise ValueError("Keystore does not exist")
    # Fingerprint the DER certificate bytes, independent of keytool's display language.
    result = subprocess.run(["keytool", "-exportcert", "-keystore", str(path), "-alias", args.alias,
                             "-storepass:env", "ATTESTRA_KEYSTORE_PASSWORD"], env=env, capture_output=True)
    if result.returncode:
        raise RuntimeError("Certificate export failed; check keystore, password and alias")
    digest = fingerprint(hashlib.sha256(result.stdout).hexdigest())
    document = [{"packageName": args.package, "sha256CertFingerprints": [digest]}]
    with open(args.output, "x", encoding="utf-8") as output:
        json.dump(document, output, indent=2)
        output.write("\n")
    print(f"Public fingerprint: {digest}\nPublic association file: {args.output}")
    print("Request approval for the intended environment. Keep the keystore and password private.")


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
        target = item.get("target", {})
        if target.get("namespace") == "android_app" and target.get("package_name") == package:
            values = {fingerprint(v) for v in target.get("sha256_cert_fingerprints", [])}
            if digest in values:
                relations.update(item.get("relation", []))
    return RELATIONS <= relations


def verify(args):
    expected = fingerprint(args.expected_sha256)
    certs = run([args.apksigner or sdk_tool("apksigner"), "verify", "--print-certs", args.apk])
    hashes = re.findall(r"Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F]+)", certs)
    if len(hashes) != 1 or fingerprint(hashes[0]) != expected:
        raise ValueError("APK signing certificate differs from the approved expected fingerprint")
    badging = run([args.aapt or sdk_tool("aapt"), "dump", "badging", args.apk])
    match = re.search(r"^package: name='([^']+)'", badging, re.M)
    if not match or match[1] != args.package:
        raise ValueError("APK application ID differs from the expected package")
    if not approved(fetch(args.url), args.package, expected):
        raise ValueError("Published associations do not approve this APK for both App Links and credentials; deploy approved configuration first")
    print(f"Verified APK signature, package and published associations: {args.package}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    for name in ("generate", "export"):
        p = commands.add_parser(name)
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
        if args.command == "verify-apk": verify(args)
        else: export(args, args.command == "generate")
    except (ValueError, RuntimeError, OSError) as error:
        print(f"Signing setup stopped: {error}", file=sys.stderr)
        return 1
    return 0

if __name__ == "__main__":
    sys.exit(main())
