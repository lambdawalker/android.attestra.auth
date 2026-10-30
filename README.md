# Attestra Android authentication

The `:auth` module implements email confirmation, passkey registration, email/passkey/password sign-in, session restoration, and policy-controlled onboarding with Ktor, protected storage, and Android Credential Manager. `:app` hosts the live flow and UI catalog. The ID capture flow supports front/back capture, direct private S3 uploads, asynchronous file checks, recovery and cancellation. A debug-only capture mock works without deployment. Text extraction and editable review are implemented; identity validation remains a separate future feature. See [Android capture](docs/identity-capture.md) and the [backend contract](docs/identity/http-contract.md).

Build settings are organized as local Gradle convention plugins. See [build configuration](docs/build-configuration.md) for feature scripts, value precedence, and signing inputs.

## Documentation

Start with [installation](IMPORT.md), [consumer guides](docs/agents/index.md),
[quickstart and runnable demo](docs/agents/quickstart.md), or [screenshots](docs/screenshots.md).
The human HTML site is built from the same sources under `sites/`; hosting is not
claimed live until a Pages deployment succeeds. See [site maintenance](sites/README.md).

## Test ID capture without deploying the backend

Install a debug build and choose **Test ID capture** from the entry screen. Use a sample card, select a capture scenario, capture and preview both sides, and follow automatic upload/processing to review and save. The mock can simulate extraction failure for manual entry; it does not approve an identity. No API configuration or sign-in is required for this demo. Mock transport is excluded from release builds.

The `:auth` library includes identity capture and requires **Android 9 (API 28)**, as do the demo app and debug mock module. Builds use compile SDK 37 and Java 17. See the [identity guide](docs/identity/README.md) for lifecycle behavior, recovery scenarios, and remaining device checks.

## Documentation ownership

[design.attestra](https://github.com/lambdawalker/design.attestra) owns [system architecture](https://github.com/lambdawalker/design.attestra/blob/main/architecture.md), [onboarding flows and screens](https://github.com/lambdawalker/design.attestra/blob/main/auth/onboarding/README.md), [login](https://github.com/lambdawalker/design.attestra/blob/main/auth/login/architecture.md), and [session recovery](https://github.com/lambdawalker/design.attestra/blob/main/auth/onboarding/resume.md). This README owns Android modules, Gradle configuration, installation, App Links, Credential Manager, storage, tests, and platform gotchas. The [Go repository](https://github.com/lambdawalker/go.attestra.aws.auth) owns backend deployment and exact HTTP contracts. Follow the [shared ownership policy](https://github.com/lambdawalker/design.attestra/blob/main/DOCUMENTATION.md); link to system flows instead of maintaining a second specification.

## Configure the deployment

The checked-in configuration uses index discovery: a normal build resolves `dev` from `https://index.attestrabond.com/v1/environments` by default. To select QA on Windows:

```powershell
.\gradlew.bat :app:assembleDebug '-PattestraEnvironment=qa'
```

Set `attestraIndexUrl` to use another trusted index. The app resolves configuration before creating authenticated clients, and applies the published API URL and ID-capture flag. See [environment discovery](docs/environment-discovery.md) for caching, library integration and session isolation.

App Links still require a build-time manifest hostname. The default is `<environment>.attestrabond.com`; override `attestraLinkHost` for a different domain. Discovery does not configure your website, `assetlinks.json` or Android signing association.

The index and a fixed API URL are alternatives: setting `attestraApiBaseUrl` skips discovery. That URL may use a custom domain (such as `https://dev.api.attestrabond.com`) or AWS API Gateway's direct `execute-api` hostname. See the [configuration comparison and switching instructions](docs/environment-discovery.md#index-discovery-versus-a-fixed-api-url).

### Explicit static configuration

Existing exported `.properties` files and an explicitly supplied `attestraApiBaseUrl` select static mode and bypass discovery. Remove any old `attestraApiBaseUrl` from local Gradle properties to use the index. For a static override:

```bash
bash gradlew :app:assembleDebug \
  -PattestraApiBaseUrl=https://YOUR-API-ID.execute-api.REGION.amazonaws.com \
  -PattestraLinkHost=app.your-domain.com
```

Obtain `apiUrl` and `appOrigin` from the deployed backend using its [stack-output instructions](https://github.com/lambdawalker/go.attestra.aws.auth/blob/main/README.md#configure-the-android-api-url). AWS credentials, Pulumi, and SES setup belong in that repository.

`apiUrl` is the value for `attestraApiBaseUrl`: copy the complete HTTPS origin, for example `https://kop22wur83.execute-api.us-east-2.amazonaws.com`. Do not add `/signup`, `/confirm`, `/verify-email`, or a trailing slash. `appOrigin` is the website that hosts the verification link; use **only its hostname** for `attestraLinkHost` (for example, `attestrabond.com` from `https://attestrabond.com`). The two hosts serve different purposes.

From the Android repository root on Windows, you can pass the values directly to Gradle:

```powershell
.\gradlew.bat :app:assembleDebug `
  '-PattestraApiBaseUrl=https://kop22wur83.execute-api.us-east-2.amazonaws.com' `
  '-PattestraLinkHost=attestrabond.com'
```

Alternatively, put these properties in your local `~/.gradle/gradle.properties` (on Windows, your user profile's `.gradle\gradle.properties`) and run `.\gradlew.bat :app:assembleDebug`:

```properties
attestraApiBaseUrl=https://kop22wur83.execute-api.us-east-2.amazonaws.com
attestraLinkHost=attestrabond.com
```

Use your own stack's `apiUrl` if it differs from this example; when `pulumi stack output apiUrl` is missing, verify the selected stack and finish `pulumi up`. These values can also be passed through CI. The link host must serve `https://HOST/.well-known/assetlinks.json` with the app's package `com.apexfission.android.attestra.auth` and signing certificate SHA-256 to make Android App Links open directly in the app. On other devices or when the app isn't associated, the website must host `/verify-email` and show the manual code screen.

The launcher opens a choice screen in the existing `MainActivity`: **Open onboarding** runs the live email flow, and **Open UI catalog** previews every screen without calling the backend. With no explicit API URL, Open onboarding waits for index discovery and offers Retry if unavailable; the catalog still works. The system Back action returns to the choice screen, or to the catalog list when previewing an individual screen. A valid HTTPS `/verify-email` App Link opens live onboarding directly, including when the catalog is currently visible. The manifest accepts links only for the configured host, and the activity checks the incoming origin again before handling one. Visiting a link never calls the backend with B alone.

### Verify Android App Links on a device

The manifest's `android:autoVerify="true"` filter matches `https://<attestraLinkHost>/verify-email`. Serve `https://<attestraLinkHost>/.well-known/assetlinks.json` (default: `https://dev.attestrabond.com/.well-known/assetlinks.json`) as public JSON without redirects. Its `package_name` must match this app's Gradle `applicationId`, **`com.apexfission.android.attestra.auth`** (not `com.apexfission.android`), and `sha256_cert_fingerprints` must include the certificate of the **installed build**. A debug APK and a release or Play-signed APK may use different certificates. To find the fingerprint for your build, run `.\gradlew.bat :app:signingReport` or inspect the installed package's `Signatures` in the command below. For example:

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.apexfission.android.attestra.auth",
      "sha256_cert_fingerprints": ["<SHA-256 fingerprint of the installed app>"]
    }
  }
]
```

You can keep other relations and fingerprints needed for other builds. If the same domain serves multiple apps, give each application ID its own statement. The website must also serve `/verify-email` for users without the Android app; opening the page alone must not confirm an address.

In Android Studio's PowerShell terminal, use the SDK's `adb.exe` if `adb` is not on `PATH`. Select the intended device serial when multiple devices or emulators are connected:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
$serial = 'emulator-5556' # replace with your device's serial from `devices`
& $adb -s $serial shell pm verify-app-links --re-verify com.apexfission.android.attestra.auth
# Wait for verification to finish, then check for `dev.attestrabond.com: verified`:
& $adb -s $serial shell pm get-app-links com.apexfission.android.attestra.auth
& $adb -s $serial shell am start -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d 'https://dev.attestrabond.com/verify-email?request_id=test&b=test'
```

The last command only checks routing; `test` values cannot confirm an email. Then open a **fresh signup email** on that device to test confirmation. If the domain shows `1024` instead of `verified`, first compare the listed `Signatures` fingerprint against the public `assetlinks.json`, then check its HTTPS response, JSON content type, and redirect behavior. If the email opens a browser despite a verified domain, check whether the mail client rewrites the tapped URL onto another host.

## Android integration

### Temporary confirmation trace

For the current session recovery investigation, filter Logcat by `EmailDebugX` (or run `adb logcat -s EmailDebugX:D`). Start a fresh signup and open its new email link. Note the `/confirm` HTTP status, `trace_id` from the `x-request-id` header, whether session decoding succeeded, and whether encrypted storage completed. A `409 confirmed_sign_in_required` means the backend consumed the proof but did not return a usable session; HTTP 200 followed by a storage exception points to Android. The same email link cannot issue a session twice. Remove the temporary trace statements after diagnosis.

Follow the [email proof protocol](https://github.com/lambdawalker/design.attestra/blob/main/auth/onboarding/email-confirmation/architecture.md) and [screen inventory](https://github.com/lambdawalker/design.attestra/blob/main/auth/onboarding/email-confirmation/README.md) in the design repo. On Android, `:auth` creates A, sends its S256 challenge through Ktor, and saves A with the request ID in Keystore encrypted preferences. App Links use matching local A for confirmation; on another device the app presents the manual-code screen. The app persists a successful session before checking authoritative enrollment status and routing the configured setup steps.

The private preferences file is excluded from cloud backup and device transfer. No A, C, B, or Cognito token is logged or put into a new navigation URL. The original link URL is cleared from the activity after parsing. The app keeps only one local pending signup at a time; opening a link on a different device falls back to code entry. `SecureAuthStorage` supplies persisted state to the live passkey and sign-in integrations.

The live host connects email-session recovery to `ReturnAuthApi` and passkey actions to `PasskeyRegistrationApi`/`AndroidPasskeyManager`. The UI catalog retains preview callbacks; it is not evidence of a successful backend operation.

## Integration points

The `:auth` module owns the Ktor client, controller, protected storage, and email host. `OnboardingBusinessActions` now belongs only to the UI gallery; its callbacks remain empty for passkey and ID preview screens. The gallery's example email/status values are illustrative. The live flow never navigates to success merely because a button was tapped.

The design submodule is a pinned reference, not a second copy of the design maintained in this repository. Initialize it with `git submodule update --init` (use an HTTPS URL override when SSH access is unavailable). Follow `design.attestra/auth/onboarding/README.md` from the pinned commit for the corresponding flow and screen inventory; the links above point to the latest design on GitHub, which can be newer than the pinned version.

### Passkey registration in the live onboarding flow

The live host uses `PasskeyRegistrationApi` for registration transport and `AndroidPasskeyManager` for Credential Manager calls. `PasskeyCancelled`, `PasskeyUnsupported`, and `PasskeyApiException` select Android recovery screens. The canonical [registration transitions](https://github.com/lambdawalker/design.attestra/blob/main/auth/onboarding/passkey-creation/architecture.md) define when success or recovery is appropriate; exact HTTP shapes stay in the [backend reference](https://github.com/lambdawalker/go.attestra.aws.auth/blob/main/README.md#passkey-registration).

Use the same discovered environment as email verification, or explicitly set `attestraApiBaseUrl` for static mode. The HTTPS `attestraLinkHost` must serve `/.well-known/assetlinks.json` containing `delegate_permission/common.get_login_creds`, the exact app ID, and the installed APK's SHA-256 signing fingerprint. Test on Android 9 or newer with a configured credential provider. Diagnostic messages remain under the `EmailDebugX` tag for now.

### Returning after an unfinished signup

See the canonical [returning-user flow](https://github.com/lambdawalker/design.attestra/blob/main/auth/onboarding/resume.md). Android implements it in `EmailOnboardingHost`, `EmailFlowController`, `ReturnAuthApi`, and `SecureAuthStorage`. The controller restores a pending signup younger than 24 hours, shows an expiry hint after ten minutes, and uses matching local A for links only within its one-hour local window. These client windows do not extend server proof validity. The host retains the existing refresh token when Cognito omits a replacement and clears its local session if restoration fails. There is no unauthenticated account/passkey lookup; the sign-in choice is explicit.

The new Go deployment must be applied before testing these screens. Passkey sign-in uses Credential Manager's `GetPublicKeyCredentialOption`. The existing `EmailDebugX` logs show request outcomes without credential or token contents.

### Policy-controlled onboarding completion

The live host consumes validated runtime settings and refreshes `/auth/status` after each enrollment and capture exit. Required steps cannot be skipped, optional steps allow an in-memory skip, and disabled steps are omitted. Legacy local ID deferral flags never establish completion. Completion requires the server's `onboarding_complete` plus the required enrollment facts. Passwords entered with email are kept only in memory, bound to that signup request and account, and sent after verification; process loss asks again. See [runtime configuration and password contracts](docs/runtime-configuration.md).

## Build, install, and verify

Use the checked-in Gradle wrapper, JDK 17, Android SDK platform 36, and build tools 36.0.0, matching `.github/workflows/verify.yml`. From the repository root:

```bash
bash gradlew :auth:testDebugUnitTest :app:assembleDebug \
  -PattestraApiBaseUrl=https://YOUR-API-ID.execute-api.REGION.amazonaws.com \
  -PattestraLinkHost=app.your-domain.com
bash gradlew :app:installDebug \
  -PattestraApiBaseUrl=https://YOUR-API-ID.execute-api.REGION.amazonaws.com \
  -PattestraLinkHost=app.your-domain.com
```

On Windows use `.\gradlew.bat` with the same tasks and properties. `installDebug` requires a connected device/emulator; the APK is under `app/build/outputs/apk/debug/`. The `:auth` module is consumed by `:app`; Maven Central and JitPack publishing for `:auth` is described in [the release runbook](docs/releases.md). No production app-store release workflow is defined. CI runs auth unit tests and assembles the demo app with placeholder hosts; it does not validate a deployed AWS stack or real domain associations.

Before a release, exercise fresh same-device and cross-device links, app restarts, invalid/expired proofs, registration cancellation, passkey and OTP sign-in, required/optional/disabled routing, password setup/sign-in, and capture cancellation on a device with the installed build's signing association. Follow the design acceptance criteria for expected outcomes. Keep diagnostic trace cleanup separate from documentation changes.


### Import a deployed environment

The backend setup wizard exports `android-config/<environment>.properties`, containing public API, link-host, Cognito, region, and capture settings. Load it directly when building this app:

```powershell
.\gradlew.bat :app:assembleDebug -PattestraConfigFile=D:/dev/go.attestra.aws.auth/android-config/qa.properties
```

```sh
./gradlew :app:assembleDebug -PattestraConfigFile=../go.attestra.aws.auth/android-config/qa.properties
```

Paths are relative to the Android repository root unless absolute. Explicit Gradle properties override file values. Missing or malformed file settings fail the build. Cognito IDs and environment metadata become `BuildConfig` fields; existing backend API authentication remains unchanged. `attestraCaptureEnabled=false` disables the live capture screen; local debug mock capture is separate. In static mode, omitting this property disables live capture. In discovery mode, the index’s `features.idCapture` controls the client flag. The server's document policy remains authoritative; enabling the client does not enable the server.

The file contains public configuration, never AWS keys, GitHub/Cloudflare tokens, or Pulumi passphrases. Re-export and rebuild when switching environments or replacing deployed resources.

## Persistent signing and domain associations

See [Android signing](docs/android-signing.md) to generate/export a developer certificate, configure persistent local/CI signing, and verify the installed-build identity against the published `assetlinks.json`. Fingerprints are reviewed public configuration; private keys are never registered with the public endpoint.

## QA & Testing

Use the [QA & Testing guide](docs/qa-testing.md) for end-to-end onboarding tests, personal signing identities, signedDebug selection, environment-specific App Links and device troubleshooting. Signing approval is required for verified email App Links as well as passkeys. Each developer can keep their own key; dev/QA publish multiple approved public fingerprints.

See [runtime app settings](docs/runtime-configuration.md) for dashboard publishing, refresh and offline behavior.

### Two-sided document camera

The live and debug camera use Card Detector 0.1.4 `TwoSidedCardCapture`:
front, turn-over prompt, back, then review with independent retakes. Auto capture
starts enabled and can be toggled beside the shutter; manual capture remains available.
Side labels guide the user and do not classify or verify a document.

Both reviewed JPEGs are uploaded in order. Retry upload retains unfinished photos
in memory; leaving the flow clears them. Both photos are finalized automatically after upload; pending extraction shows loading and failed extraction opens manual entry. Saving the reviewed details opens a server-confirmed Details saved screen; see [save and finish](docs/identity-capture.md#save-and-finish).
Activity recreation restarts any unsubmitted camera session.

The detector and model share a JitPack artifact ID. `cardModelAssets` resolves
`tfmodel~v0.1.2` separately and copies only assets into the auth library; the
runtime depends exclusively on `carddetector~v0.1.4`, avoiding the old core 0.1.0.
The local Sentinel catalog matches that model release.

### Documentation screenshots

Render offline screen images with the **Render documentation screenshots** workflow.
Its artifact includes a browsable light/dark gallery and PNGs for documentation.
See [screenshot rendering and export](docs/screenshots.md) for commands, coverage,
and the distinction between rendering checks and approved visual baselines.

## Library publishing

See [publishing setup and recovery](docs/releases.md). Run `python scripts/setup_github_environments.py` after installing `scripts/requirements-setup.txt` to configure GitHub deployment environments. Read [IMPORT.md](IMPORT.md) for confirmed installation choices; proposed coordinates are not a release.
