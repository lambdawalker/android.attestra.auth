# Attestra Android authentication

The `:auth` module implements the email confirmation flow from [design.attestra](https://github.com/lambdawalker/design.attestra/tree/main/auth/onboarding/email-confirmation). It talks to the [Go auth backend](https://github.com/lambdawalker/go.attestra.aws.auth) through Ktor. Passkey registration and ID capture remain separate follow-up features.

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

For integration debugging, filter Logcat by `AttestraAuth`, `AttestraEmail`, and `AttestraEmailApi`. These tags report navigation, proof route (automatic or manual), request status, and failure category. On a connected device you can run `adb logcat -s AttestraAuth:D AttestraEmail:D AttestraEmailApi:D`. Logs do not contain email addresses, proof values, request IDs, link URLs, session tokens, response bodies, or exception messages.

## Email flow

The client generates random A locally, sends `base64url(SHA256(A))` to `POST /signup`, and saves A plus the returned request ID in Android Keystore encrypted preferences. The server emails a link containing B and a separate six-digit C. With a matching A, opening the link automatically calls `POST /confirm` with A+B; without A it displays an empty six-digit field and submits B+C only when Verify email is tapped. `POST /resend` rotates the email proofs; the app waits a minute before offering another request. Errors use the existing incorrect-code, attempt-limit, unusable-link, and session-recovery screens. A successful response is stored before opening passkey setup.

The private preferences file is excluded from cloud backup and device transfer. No A, C, B, or Cognito token is logged or put into a new navigation URL. The original link URL is cleared from the activity after parsing. The app keeps only one local pending signup at a time; opening a link on a different device falls back to code entry. Session storage is in `SecureAuthStorage` for future passkey and login integration.

When confirmation succeeds but no session can be issued, the app displays the existing email sign-in recovery screen. Its sign-in action remains a callback until the login feature is implemented. The passkey screen also stays separate: tapping Create passkey explains that the passkey integration is still pending, and it never marks a passkey as registered.

## Screen groups

| Group | Included views |
| --- | --- |
| Email | Start, wait for email, manual six-digit code, wrong code, attempt limit, unusable link, confirmed email with missing session |
| Shared loading | Email confirmation/resend, passkey manager/registration, ID extraction/submission, provider decision pending; `LoadingTask` supplies the title and text |
| Passkey | Start, interrupted/cancelled, unsupported device |
| Optional ID check | Entry, editable document data, unreadable images, submission failure, approved outcome, provisional unsuccessful outcome |

The capture plugin supplies its own camera screens. The unsuccessful identity outcome uses the shared error layout because the design submodule has no mockup for the final provider rejection yet. The six-slot code presentation uses one underlying field for paste and autofill.

## Integration points

The `:auth` module owns the Ktor client, controller, protected storage, and email host. `OnboardingBusinessActions` now belongs only to the UI gallery; its callbacks remain empty for passkey and ID preview screens. The gallery's example email/status values are illustrative. The live flow never navigates to success merely because a button was tapped.

The design submodule is pinned by this repository. Initialize it with `git submodule update --init` (use an HTTPS URL override when SSH access is unavailable). See `design.attestra/auth/onboarding/architecture.md` and `design.attestra/auth/onboarding/ui-reference/README.md` for the flow and screen inventory.
