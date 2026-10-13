"""Interactive UI; signing.py remains usable without Rich for automation."""
import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess

from rich.console import Console
from rich.panel import Panel
from rich.prompt import Confirm, Prompt
from rich.table import Table

import signing

ROOT = Path(__file__).resolve().parents[2]
PACKAGE = "com.apexfission.android.attestra.auth"


def new_output(environment):
    candidate = ROOT / f"developer-associations-{environment}.json"
    count = 2
    while candidate.exists():
        candidate = ROOT / f"developer-associations-{environment}-{count}.json"
        count += 1
    return str(candidate)


class Wizard:
    def __init__(self, console=None):
        self.console = console or Console(markup=False)
        self.environment = "dev"
        self.alias = "attestra"
        self.package = PACKAGE
        self.keystore = None
        self.digest = None
        self.sdk = None

    def ask(self, label, default=None, validate=None, password=False):
        while True:
            value = Prompt.ask(label, default=default, password=password, console=self.console)
            if validate is None or validate(value):
                return value
            self.console.print("Please enter a valid value.", style="yellow")

    def context(self):
        environment = self.ask("Environment (dev, qa, or your own name)", self.environment,
                               lambda v: bool(re.fullmatch(r"[a-z][a-z0-9-]{0,62}", v)))
        if environment != self.environment:
            self.keystore = None
            self.digest = None
        self.environment = environment
        self.package = self.ask("Android application ID", self.package,
                                lambda v: bool(re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+", v)))

    def credentials(self, generate):
        env = os.environ.copy()
        self.console.print("The keystore password protects the container; the key password unlocks the signing key. Neither is a separate vault plugin's unlock PIN. Passwords are not saved (minimum six characters).")
        fields = [("ATTESTRA_KEYSTORE_PASSWORD", "Keystore password")]
        if generate:
            fields.append(("ATTESTRA_KEY_PASSWORD", "Key password"))
        for variable, label in fields:
            if env.get(variable) and Confirm.ask(f"Use {variable} from this process?", default=True, console=self.console):
                if len(env[variable]) < 6:
                    raise ValueError(f"{label} must contain at least six characters")
                continue
            while True:
                password = self.ask(label, password=True, validate=lambda v: len(v) >= 6)
                if not generate or self.ask(f"Confirm {label.lower()}", password=True) == password:
                    env[variable] = password
                    break
                self.console.print("Passwords did not match. Try again.", style="yellow")
        return env

    def guidance(self):
        self.console.print(Panel(
            f"ATTESTRA_KEYSTORE_PATH={self.keystore or Path.home() / '.attestra/signing' / (self.environment + '.jks')}\n"
            f"ATTESTRA_KEY_ALIAS={self.alias}\n\n"
            "Provide ATTESTRA_KEYSTORE_PASSWORD through your environment or secret manager before starting Android Studio. "
            "Set ATTESTRA_KEY_PASSWORD to the separate key password. If omitted, Gradle falls back to the store password. Gradle also supports local.properties, but that stores passwords as plaintext.\n\n"
            "Sync Gradle, then select signedDebug in Build Variants. Disable the Gradle configuration cache for signed builds.\n"
            "Submit the public association JSON for environment approval before testing App Links or passkeys.",
            title="Android Studio • next steps", border_style="cyan"))

    def export(self, generate):
        if not shutil.which("keytool"):
            raise ValueError("Install a JDK and put its bin directory (keytool) on PATH")
        self.context()
        default_key = Path.home() / ".attestra/signing" / f"{self.environment}.jks"
        if not generate and not default_key.exists():
            default_key = default_key.with_suffix(".p12")
        if generate:
            self.console.print("New keys use JKS so the keystore and key can have separate passwords. Existing PKCS12 keys can still be exported.")
        path = self.ask("Keystore path", self.keystore or str(default_key),
                        lambda v: bool(v.strip()) and (not Path(v).expanduser().exists() if generate else Path(v).expanduser().is_file()))
        self.alias = self.ask("Key alias", self.alias, lambda v: bool(v.strip()))
        output = self.ask("Public association JSON", new_output(self.environment),
                          lambda v: bool(v.strip()) and not Path(v).expanduser().exists() and Path(v).expanduser().resolve().parent.is_dir())
        self.console.print(Panel(f"Environment: {self.environment}\nPackage: {self.package}\nKey: {path}\nAlias: {self.alias}\nPublic output: {output}", title="Review", border_style="cyan"))
        if not Confirm.ask("Generate key and export public certificate?" if generate else "Export public certificate?", default=True, console=self.console):
            return
        env = self.credentials(generate)
        try:
            signing.export(argparse.Namespace(keystore=path, alias=self.alias, package=self.package, output=output, store_type="JKS"), generate, env=env)
        except (ValueError, RuntimeError, OSError, subprocess.TimeoutExpired):
            if generate and Path(path).expanduser().exists():
                self.console.print("A keystore exists at the selected path. Keep it; choose Export to recover the public JSON.", style="yellow")
            raise
        finally:
            env.pop("ATTESTRA_KEYSTORE_PASSWORD", None)
            env.pop("ATTESTRA_KEY_PASSWORD", None)
        try:
            shared = signing.save_public_copy(output, self.environment)
            self.console.print(f"Public setup copy: {shared}\nGo setup can discover this file on this computer; approval is still required.")
        except OSError as error:
            self.console.print(f"Public export succeeded, but the setup copy could not be saved: {error}. Select the original JSON manually in Go setup.", style="yellow")
        self.keystore = str(Path(path).expanduser().resolve())
        self.console.print("✓ Public signing association exported.", style="green")
        self.guidance()

    def sdk_tools(self):
        try:
            return signing.sdk_tool("apksigner", self.sdk), signing.sdk_tool("aapt", self.sdk)
        except ValueError:
            self.console.print("Android SDK Build-Tools were not found. In Android Studio, open Settings → Languages & Frameworks → Android SDK. Install Android SDK Build-Tools under SDK Tools, then enter the Android SDK Location below.", style="yellow")
        while True:
            path = self.ask("Android SDK folder (Ctrl+C to return to menu)", self.sdk)
            try:
                tools = signing.sdk_tool("apksigner", path), signing.sdk_tool("aapt", path)
            except ValueError:
                self.console.print("That folder does not contain both apksigner and aapt under build-tools/<version>. Select the SDK root folder, or install Build-Tools first.", style="yellow")
                continue
            self.sdk = path
            self.console.print("SDK folder selected for this session.", style="green")
            return tools

    def verify(self):
        self.console.print("Read-only check: verifies the local APK and reads the published assetlinks.json. No data is uploaded or published.")
        apksigner, aapt = self.sdk_tools()
        self.context()
        apk = self.ask("APK path", str(ROOT / "app/build/outputs/apk/signedDebug/app-signedDebug.apk"), lambda v: Path(v).expanduser().is_file())
        def valid_digest(value):
            try:
                signing.fingerprint(value)
                return True
            except ValueError:
                return False
        digest = self.ask("Approved SHA-256 certificate fingerprint", self.digest, valid_digest)
        url = self.ask("Published association URL", f"https://{self.environment}.attestrabond.com/.well-known/assetlinks.json")
        signing.verify(argparse.Namespace(apk=str(Path(apk).expanduser()), expected_sha256=digest, package=self.package, url=url, apksigner=apksigner, aapt=aapt))
        self.digest = digest
        self.console.print("✓ APK signature, package and published associations match.", style="green")

    def run(self):
        self.console.print(Panel("Create a private signing identity, export its public fingerprint, or verify an APK.\nEnter accepts defaults. Ctrl+C cancels an action. Keys and passwords are never uploaded.", title="Attestra • Android signing", border_style="cyan"))
        while True:
            menu = Table(title="Choose an action", show_header=False, box=None, padding=(0, 2))
            for number, label in [("1", "Generate a new signing key"), ("2", "Export an existing key's public fingerprint"), ("3", "Verify APK against published associations (read-only)"), ("4", "Show Android Studio configuration guidance"), ("0", "Exit")]:
                menu.add_row(number, label)
            self.console.print(menu)
            try:
                choice = Prompt.ask("Action", choices=["1", "2", "3", "4", "0"], default="1", console=self.console)
            except (EOFError, KeyboardInterrupt):
                return 0
            if choice == "0":
                return 0
            try:
                if choice in ("1", "2"):
                    self.export(choice == "1")
                elif choice == "3":
                    self.verify()
                else:
                    self.guidance()
            except EOFError:
                return 0
            except KeyboardInterrupt:
                self.console.print("\nAction cancelled. Existing files are retained.", style="yellow")
            except (ValueError, RuntimeError, OSError, subprocess.TimeoutExpired) as error:
                self.console.print(f"Action stopped: {error}\nCorrect the issue and choose an action to retry.", style="red")


def wizard():
    return Wizard().run()
