# Signing identities and domain associations

Each developer testing verified App Links (including email-verification onboarding) or real passkeys needs an approved signing identity and keeps a personal private signing key. Dev/QA can approve multiple developer fingerprints for the same package; teammates do not need to share keys or passwords. Ordinary UI development can retain its normal debug key. Only public certificate fingerprints are approved for each environment. No build automatically grants trust to a newly encountered certificate.

See [QA & Testing](qa-testing.md) for variant selection, device verification and troubleshooting.

## Guided setup

Install [uv](https://docs.astral.sh/uv/getting-started/installation/), then run from this repository:

```sh
uv run scripts/signing/signing.py
```

Root launchers are also available (install `uv` first):

```powershell
.\signing.bat
```

```sh
./signing.sh
```

Both forward arguments, for example `signing.bat export --keystore ... --output ...` or `./signing.sh --help`. They locate the tool relative to the launcher and preserve your current directory for relative input/output paths.

`uv` installs the pinned Rich terminal UI dependency in an isolated environment. The menu offers key generation, public fingerprint export, APK verification, and Android Studio guidance. All choices are visible; Enter accepts defaults and Ctrl+C cancels the current action. Run in an interactive terminal, including Android Studio's Terminal window.

The default environment is `dev`; enter `qa` or a custom name when prompted. Defaults include `~/.attestra/signing/<environment>.jks`, alias `attestra`, the app's application ID, and `https://<environment>.attestrabond.com/.well-known/assetlinks.json`. After a successful wizard export, a public copy is also saved to `~/.attestra/associations/<environment>/developer-associations.json`. Older copies are preserved with a content-hash suffix when replaced. Go setup on the same computer discovers this file and displays the package/fingerprints for explicit approval; existing approvals are preserved. For an already-complete setup, run `setup.bat -force-setup` to review the new file. On another computer, transfer only the public JSON or select its path manually. Explicit CLI commands opt in with `--environment dev`. No private keys or passwords are copied.

The public JSON defaults to `developer-associations-<environment>.json` in the checkout, with a numbered suffix if it already exists. Verification defaults to the signedDebug APK; any path or URL can be overridden.

Generation requires JDK `keytool` on PATH. APK verification also requires Android SDK Build Tools (`ANDROID_HOME` or `ANDROID_SDK_ROOT`, or tools on PATH). SDK discovery checks `ANDROID_HOME`, `ANDROID_SDK_ROOT`, the repository’s `local.properties` (`sdk.dir`, including escaped Windows paths), standard Windows/macOS/Linux SDK locations, and PATH. If discovery fails, the wizard explains how to install Build-Tools and asks for the SDK root folder. It remembers that selection for the current session and retries invalid paths. Ctrl+C returns to the menu. Verification is read-only: it reads the published assetlinks.json and never uploads or publishes associations. Missing tools and failed actions return to the menu. Existing keys and output files are never overwritten. If key generation succeeds but export fails, choose Export to recover using the same key.

Passwords are hidden and confirmed for new keys. You can reuse `ATTESTRA_KEYSTORE_PASSWORD` from the invoking process; the wizard never writes passwords or modifies your shell environment. It does not automatically edit Gradle configuration or approve/upload fingerprints. Keep the key and password in secure storage. Selecting an environment only sets defaults; it does not grant trust there.

For automation, the explicit commands below retain their arguments and can still run with standard-library Python. Use `ATTESTRA_KEYSTORE_PASSWORD` to avoid a password prompt. `wizard` is also an explicit command. Running without a command in a noninteractive process fails with usage guidance.

## Keystore password and key password

New wizard-generated keys use **JKS** and request two hidden, confirmed values: the **keystore password** (`ATTESTRA_KEYSTORE_PASSWORD`) and **key password** (`ATTESTRA_KEY_PASSWORD`). These correspond to Android Studio's separate signing fields. Supply both when they differ; Gradle still falls back to the store password when the key password is omitted. A vault plugin's unlock PIN/passphrase is a third, separate credential and is not managed by this script.

The wizard uses JKS because `keytool` PKCS12 generation does not support distinct store/key passwords. Existing `.p12` keys continue to export with their keystore password; exporting a public certificate does not need the private key password. Existing files are never converted or replaced. Do not generate a replacement identity just to change password handling: a new key changes the fingerprint and needs approval.

For CLI generation, use a `.jks` path (or `--store-type JKS`) and provide both password environment variables through your secret manager/session. `.p12`/`.pfx` paths retain PKCS12 generation by default; distinct passwords are rejected before creation. If no key password is provided to CLI generation, it retains the previous same-password behavior. Password values never appear in arguments or public JSON. The Go deployment tools need no change.

## Generate once, export before building

Requires Python 3 and JDK `keytool`. From this repository on Windows:

```powershell
python scripts/signing/signing.py generate --keystore "$env:USERPROFILE/.attestra/signing/dev.p12" --output developer-associations.json
```

The password is requested without echo. Use a long unique password and keep it in your password manager. Store the keystore securely outside the checkout. Generation refuses to replace existing keys. For an existing keystore, use `export` instead of `generate`, with the same arguments and optional `--alias`.

The output contains only `packageName` and `sha256CertFingerprints`. Submit it to the environment maintainer for approval. Maintainers merge approved fingerprints into the backend's `androidAssociations` configuration, then run setup/deploy. Do not commit private keystores, passwords or environment files containing passwords. Protect the local file with your OS account permissions; chmod alone is not a Windows ACL policy.

## Build using your persistent key

Signing is configured by `buildSrc/src/main/kotlin/attestra.signing.gradle.kts`, applied by the app. See [build configuration](build-configuration.md) for input sources and precedence. Do not duplicate the signing configuration in `app/build.gradle.kts`.

Set these inputs in your process environment or repository-root `local.properties` (same uppercase names; environment values win per setting). See the [local.properties example](build-configuration.md#signing-settings).

Set `ATTESTRA_KEYSTORE_PATH`, `ATTESTRA_KEYSTORE_PASSWORD`, and optionally `ATTESTRA_KEY_ALIAS` (default `attestra`) and `ATTESTRA_KEY_PASSWORD` (defaults to the store password). You can supply passwords through your local secret manager/session, or the ignored local.properties file; the latter stores them as plaintext. Never check them in or put them in shell history. Then:

```powershell
.\gradlew.bat --no-configuration-cache :app:assembleDebug '-PattestraEnvironment=dev'
```

After Gradle validates the keystore credentials, the **signedDebug** variant also appears in Android Studio. Build it with `:app:assembleSignedDebug --no-configuration-cache`; see [signed debug variant](build-configuration.md#signed-debug-variant).

Both debug and release builds use the selected key when its path is supplied. Without it, normal debug signing and the existing unsigned release behavior remain. Disable the Gradle configuration cache when supplying signing secrets. The fingerprint comes from the certificate and stays the same across builds; a different key may require reinstalling the app, losing local app data.

## GitHub signed builds

The manual **Build approved Android APK** workflow uses a GitHub environment (`dev`, `qa`, etc.) and uploads an APK only after signature/package/public association verification succeeds. Protect environments that can access signing secrets and restrict permitted branches to trusted reviewed code. It is not triggered by pull requests.

Environment secrets:

- `ANDROID_KEYSTORE_BASE64`: base64 of the persistent CI keystore; encoding is not encryption, so store it as a secret.
- `ANDROID_KEYSTORE_PASSWORD` and optional `ANDROID_KEY_PASSWORD`.

Environment variables:

- `ANDROID_KEY_ALIAS` (defaults to `attestra`).
- `ANDROID_SIGNING_SHA256`: reviewed SHA-256 certificate fingerprint.
- `ATTESTRA_LINK_HOST`: exact environment app hostname, such as `qa.attestrabond.com`.
- Optional `ATTESTRA_INDEX_URL`; defaults to the shared index.

CI disables Gradle caching for this job and removes the downloaded keystore on exit. It does not upload keys to AWS, update approved fingerprints or publish to Play. For Play-distributed applications, register the **Play app signing** certificate, not merely the upload certificate; this APK workflow verifies directly distributed APKs.

Local verification uses Android SDK Build Tools (`ANDROID_HOME`, or explicit `--apksigner` and `--aapt` paths):

```powershell
python scripts/signing/signing.py verify-apk --apk app/build/outputs/apk/debug/app-debug.apk --expected-sha256 YOUR_APPROVED_FINGERPRINT --url https://dev.attestrabond.com/.well-known/assetlinks.json
```

The tool checks the APK signature, application ID and both association relations on the public HTTPS endpoint. It does not certify a successful Credential Manager ceremony or instant device-side re-verification. Test real passkey registration/sign-in and App Links on a device after deployment.

## Backend ownership

[Backend association hosting](https://github.com/lambdawalker/go.attestra.aws.auth/blob/feat/android-domain-associations/docs/android-associations.md) owns DNS, HTTPS, public files, revocation updates and teardown. Removing a fingerprint withdraws its association; it does not stop its owner signing APKs or revoke existing sessions. Private key rotation and backend session revocation remain separate procedures.

## Startup association warning

The demo app checks its actual installed signing certificates against the build-time link host's public `assetlinks.json` on startup. An unapproved package/certificate or missing relation produces a long toast naming the affected feature (passkeys, verified App Links, or both). This runs asynchronously and does not block onboarding. The check and notification survive activity rotation without repeating; a new activity ViewModel lifetime runs a fresh check.

Network, HTTP, or malformed-response failures produce an **approval unknown** toast instead of claiming that the certificate is unapproved. Successful website matching remains advisory: it does not establish Android's device-side verification state, confirm relying-party compatibility, or guarantee a successful passkey ceremony. Use the README’s device App Links checks if routing still fails. The check does not upload keys, fingerprints, or email tokens.
