# Android identity integration

The system flow and policy remain owned by [design.attestra](https://github.com/lambdawalker/design.attestra/tree/main/auth/onboarding/id-capture). Its ID-capture and verification documents were still **proposed**, and [the Go backend](https://github.com/lambdawalker/go.attestra.aws.auth) had no ID upload, extraction, submission, or decision endpoints when this implementation was written. Existing Android screens were catalog previews; the capture action was a placeholder.

This change connects the Android flow and proposes a [wire contract](http-contract.md) for backend implementation. It does not deploy or implement the identity backend, OCR, document authenticity checks, selfie/liveness, or an external identity provider.

## Try it without a deployment

Build and install a **debug** app. Launch opens the test menu; saved onboarding does not automatically open identity test options. Open **Test ID capture** to choose a mock scenario explicitly. No API URL or sign-in is needed. Start the sample check, select **Scan government ID**, capture and review the front, then turn the card over and capture/review the back. Camera permission appears only when it is needed; an existing grant opens the scanner directly. Use sample cards, not a real person's ID. Review the synthetic fields and submit. The persistent Local Mock banner has been removed; the test configuration explains the simulation and all results remain labeled simulated.

The catalog's **Confirm your ID → Scan government ID** action also opens capture directly, without the scenario chooser or another Confirm screen.

The default scenario accepts submission as pending. **Check status** advances the mock: first status read completes the automated report, the second returns the provider/policy result. `Stays pending` stays pending regardless of reads. There is no indefinite blocking poll. Leave and reopen to retrieve the current result.

| Scenario | Behavior |
| --- | --- |
| Approved | Pending → automated approval → provider/policy approval |
| Stays pending | Submission remains pending; leave and return later |
| Rejected | Unsuccessful result, no client recapture action |
| Inconclusive | Distinct inconclusive result, recapture permitted |
| Unreadable | Retake before submitting; never a provider rejection |
| Submission fails once | First submit returns 503; Retry checks status before replaying the same body |
| Accepted · response lost | Mock saves acceptance then throws an I/O error; Retry discovers the accepted record without another submit |

**Discard saved sample check** on the explicit test configuration screen clears the local demo checkpoint so another sample can be started. Ordinary exit/reopen preserves status recovery. Scenario selection is persisted by the mock service, including across activity recreation. This reset exists only in the debug harness.

The test menu's local reset clears saved session/onboarding state, identity checkpoints, and mock files on this device. It does not delete a remote account or registered passkeys.

The same mock integration is available after live email/passkey onboarding in debug builds. From **Confirm your ID**, **Scan government ID** proceeds directly to permission handling and capture, with no scenario chooser or duplicate start screen. Existing checkpoints are reconciled first so pending/accepted checks are not silently replaced. Each authenticated Cognito issuer/subject gets a separate namespace; the standalone demo is isolated from all real accounts. Actual auth tokens are never forwarded to the mock service. Release builds have neither mock transport nor the offline entry: they display an unavailable screen until the real service is wired.

## Module boundaries and dependencies

- `:auth` (minSdk 28) contains the models, Ktor client, controller, encrypted identifier checkpoint, Compose host, camera adapter, and photo review. Capture lives in `com.apexfission.android.attestra.auth.identity.capture`; its gateway and capture slot remain injectable. It depends on the confirmed Permissions `permission~v0.2.3` and bundled detector model `tfmodel~v0.1.2` JitPack artifacts. The latter exports `com.apexfission.android.carddetector:core:0.1.0`. **Do not add another detector artifact alongside it.** Published core `v0.1.0` was inspected for API compatibility.
- `:identity-mock` (minSdk 28, matching its `:auth` dependency) is a local Ktor MockEngine server. The demo includes it only with `debugImplementation`. Requests exercise the same HTTP serialization/status handling as the real API client; no TCP listener or AWS service is started.
- `:app` now requires minSdk 28 because it includes real capture. Compile SDK 37 and Java 17 accommodate the published libraries. The existing Gradle, AGP, Kotlin versions and target SDK remain unchanged.

Permissions are requested only when entering capture, and the permission library bypasses its permission screens when camera access is already granted. Denial stays in the permission UI; Settings recovery is supplied by the permission library. Only camera permission is requested. The `:auth` manifest declares camera optional so camera-less devices can still use authentication and display a capture-unavailable message.

## State and lifecycle

Each scan contains exactly two explicit sides. The card detector detects a card, not its front/back or authenticity; the user confirms the requested side and readability on the photo review screen. Each side/retake gets a fresh detector ViewModel store so a retained front crop cannot become the back capture. Worker callbacks recycle detection/capture bitmaps, compress off the UI thread, reject late/duplicate callbacks, and transfer bounded JPEG bytes to the host.

The controller serializes operations, validates submission ID/evidence-version bindings, and keeps extracted and corrected fields distinct. A lost acceptance response enters reconciliation; a failed status lookup never triggers blind resubmission. An accepted result can be recaptured only when the service explicitly allows it. Acceptance and automated approval are never interpreted as policy approval.

Raw images and editable identity fields live only in memory. Photos are limited to 2048 pixels on the longest edge and 4 MiB each; encoded buffers are wiped on upload/discard. Android process/activity recreation discards an unfinished draft and asks for recapture. Accepted submissions resume through their identifier. A checkpoint is saved before draft creation, so a lost response can still be looked up. There is no attempt to silently reconstruct sensitive evidence after process death.

Identifier checkpoints use the existing AES-GCM/Keystore protected preferences, excluded from backup. Namespaces include environment and Cognito issuer/subject. The mock service saves only identifiers, state, scenario, upload-presence flags, and an idempotency fingerprint in an atomic file under `noBackupFilesDir`. It never saves image bytes, extracted names, or corrections. Production metadata belongs on the backend; the mock file is only a development surrogate. The debug identity screen sets FLAG_SECURE while open. Do not add raw request/response logging to the identity client.

## Connect a deployed service later

Use `IdentityApi(httpsOrigin, httpClient) { currentAccessToken }` with the normal JSON/timeout configuration, `SecureIdentityCheckpointStore(storage, httpsOrigin, issuerAndSubject)`, and `IdentityController`. Mount `IdentityHost` and pass `IdentityCamera` through its capture slot, as the debug app does. Obtain a valid authenticated session before entering and handle `SignInRequired` through host sign-in. Token refresh remains the host's responsibility.

Replace the release `AccountIdentityFlow` binding only after the service implements the contract and provider/consent/retention policy is approved. Never inject the mock as a fallback after a real request fails, never mix mock checkpoints with a live environment, and never use a client result to authorize restricted features. Server authorization must evaluate current applicable evidence and policy.

## Verification

```bash
bash gradlew :auth:testDebugUnitTest :identity-mock:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

The controller tests exercise direct scan entry, accepted/failed-status checkpoint preservation, correction attribution, accepted-versus-approved state, response-loss reconciliation, draft/accepted restart, forbidden retry, cancellation, and stale versions. Mock transport tests exercise HTTP serialization, idempotent draft creation/submission, immutable accepted evidence, distinct outcomes, bad credentials, and restart retrieval. CI runs these alongside existing auth tests and both app variants.

In the implementation environment, these commands were attempted but blocked before compilation: the Gradle 9.6.0 distribution could not be downloaded (`Network is unreachable`). No passing compile/test or device results are claimed.

Before merging, run CI and test on an API-28+ device: grant, deny, deny again, Settings-return; no camera; front/back confirmation and retake; rotate/kill during scan/review/submit; leave while a callback/upload is in flight; restart after acceptance; all scenarios; switch accounts; verify release has no mock entry or mock dependency. Confirm camera closes between sides and after exit, and no document remains in screenshots, task previews, backups, or app logs. Card detection requires a representative sample document and physical-device validation.
