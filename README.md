# Attestra Android authentication

The `:auth` module implements the [email confirmation design](https://github.com/lambdawalker/design.attestra/tree/main/auth/onboarding/email-confirmation) through Ktor and the [Go auth backend](https://github.com/lambdawalker/go.attestra.aws.auth). The design repository owns the cross-platform [onboarding flow and screens](https://github.com/lambdawalker/design.attestra/blob/main/auth/onboarding/README.md); this README covers Android setup, App Links, local state, and testing. Passkey registration and ID capture remain follow-up features.

## Configure the deployment

Build with two Gradle properties; neither URL is hardcoded in the auth module:

```bash
bash gradlew :app:assembleDebug \
  -PattestraApiBaseUrl=https://YOUR-API-ID.execute-api.REGION.amazonaws.com \
  -PattestraLinkHost=app.your-domain.com
```

To retrieve the actual values after deploying the [Go auth stack](https://github.com/lambdawalker/go.attestra.aws.auth), use PowerShell:

```powershell
cd D:\dev\go.attestra.aws.auth\infra
pulumi stack select dev
pulumi stack output apiUrl
pulumi config get appOrigin
```

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

The launcher opens a choice screen in the existing `MainActivity`: **Start onboarding** runs the live email flow, and **Open UI catalog** previews every screen without calling the backend. With no API URL, Start onboarding explains how to configure `attestraApiBaseUrl`; the catalog still works. The system Back action returns to the choice screen, or to the catalog list when previewing an individual screen. A valid HTTPS `/verify-email` App Link opens live onboarding directly, including when the catalog is currently visible. The manifest accepts links only for the configured host, and the activity checks the incoming origin again before handling one. Visiting a link never calls the backend with B alone.

### Verify Android App Links on a device

The manifest's `android:autoVerify="true"` filter matches `https://<attestraLinkHost>/verify-email`. Serve `https://attestrabond.com/.well-known/assetlinks.json` as public JSON without redirects. Its `package_name` must match this app's Gradle `applicationId`, **`com.apexfission.android.attestra.auth`** (not `com.apexfission.android`), and `sha256_cert_fingerprints` must include the certificate of the **installed build**. A debug APK and a release or Play-signed APK may use different certificates. To find the fingerprint for your build, run `.\gradlew.bat :app:signingReport` or inspect the installed package's `Signatures` in the command below. For example:

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
# Wait for verification to finish, then check for `attestrabond.com: verified`:
& $adb -s $serial shell pm get-app-links com.apexfission.android.attestra.auth
& $adb -s $serial shell am start -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d 'https://attestrabond.com/verify-email?request_id=test&b=test'
```

The last command only checks routing; `test` values cannot confirm an email. Then open a **fresh signup email** on that device to test confirmation. If the domain shows `1024` instead of `verified`, first compare the listed `Signatures` fingerprint against the public `assetlinks.json`, then check its HTTPS response, JSON content type, and redirect behavior. If the email opens a browser despite a verified domain, check whether the mail client rewrites the tapped URL onto another host.

## Android integration

Follow the [email proof protocol](https://github.com/lambdawalker/design.attestra/blob/main/auth/onboarding/email-confirmation/architecture.md) and [screen inventory](https://github.com/lambdawalker/design.attestra/blob/main/auth/onboarding/email-confirmation/README.md) in the design repo. On Android, `:auth` creates A, sends its S256 challenge through Ktor, and saves A with the request ID in Keystore encrypted preferences. App Links use matching local A for confirmation; on another device the app presents the manual-code screen. The app persists a successful session before opening passkey setup.

The private preferences file is excluded from cloud backup and device transfer. No A, C, B, or Cognito token is logged or put into a new navigation URL. The original link URL is cleared from the activity after parsing. The app keeps only one local pending signup at a time; opening a link on a different device falls back to code entry. Session storage is in `SecureAuthStorage` for future passkey and login integration.

When confirmation succeeds but no session can be issued, the app displays the existing email sign-in recovery screen. Its sign-in action remains a callback until the login feature is implemented. The passkey screen also stays separate: tapping Create passkey explains that the passkey integration is still pending, and it never marks a passkey as registered.

## Integration points

The `:auth` module owns the Ktor client, controller, protected storage, and email host. `OnboardingBusinessActions` now belongs only to the UI gallery; its callbacks remain empty for passkey and ID preview screens. The gallery's example email/status values are illustrative. The live flow never navigates to success merely because a button was tapped.

The design submodule is a pinned reference, not a second copy of the design maintained in this repository. Initialize it with `git submodule update --init` (use an HTTPS URL override when SSH access is unavailable). Follow `design.attestra/auth/onboarding/README.md` from the pinned commit for the corresponding flow and screen inventory; the links above point to the latest design on GitHub, which can be newer than the pinned version.
