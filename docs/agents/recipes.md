# Integration recipes

Scope: Development/source. Start with [quickstart](quickstart.md), not a guessed Maven version.

## Add ID capture to onboarding

Use the complete [LiveCaptureFlow](../../app/src/main/java/com/apexfission/android/attestra/auth/LiveCaptureFlow.kt).
It remembers `CaptureClients.api()` and `.uploads()`, binds `SecureCaptureCheckpointStore`
to account/backend, supplies the current access token only for that account, closes
both clients on disposal and connects `IdentityCamera` to `CaptureHost`.

Pass the disclosure reviewed on the existing Continue screen as `acceptedPolicy`;
set `requireReviewedPolicy = true`. Never silently accept a different policy version
just to avoid another user acknowledgement. `allowNewCapture` reflects fresh runtime
settings; it does not grant server authorization.

Expected flow: Continue → permission/camera → front/back review → upload → file checks
→ extraction → populated/empty manual review → server-confirmed **Details saved**.
Cancellation, 401, stale consent, invalid evidence and offline requests have distinct
recovery paths. Existing images should resume processing instead of reopening camera.

## Add discovery without leaking credentials

Use `environmentIndexClient()` and inject `EnvironmentCache`; close the client with
its owner. Resolve the environment before creating auth adapters. Reuse the sample's
[EnvironmentViewModel](../../app/src/main/java/com/apexfission/android/attestra/auth/EnvironmentViewModel.kt)
and [backend configuration](../../app/src/main/java/com/apexfission/android/attestra/auth/BackendConfiguration.kt)
for account-binding and runtime refresh responsibilities. Never reuse a bearer-bearing
client for public discovery. See [discovery](../environment-discovery.md).

## Customize screen UI

Reuse state-driven screen composables under `ui/onboarding`; wrap them in
`AttestraAuthTheme`. Supply callbacks that invoke the appropriate controller, not
optimistic success navigation. `OnboardingFrame` accepts optional primary/secondary
labels/actions and content; omitted actions stay absent. Callback execution occurs
from Compose UI interaction; launch suspend work in the owning scope.

Use [the UI catalog](../../auth/src/main/java/com/apexfission/android/attestra/auth/ui/onboarding/OnboardingCatalog.kt)
for visual examples and [screenshots](../screenshots.md) for deterministic fixtures.
The catalog's older identity-decision pages are labeled design mockups, not live outcomes.

## Clear local data

Use the sample's reviewed local reset flow. `SecureAuthStorage.clearLocalUser()`
does not revoke tokens or remove remote passkeys/evidence. Remote evidence deletion
requires capture cancellation through the authenticated API. Do not invoke an
infrastructure reset to test a single device's sign-out path.

## Troubleshoot without sensitive logs

Filter Logcat by `AttestraCapture` for capture metadata. Use the same capture ID
and UTC time in the backend worker/dispatcher logs. A `finalizing` record has not
reached extraction; investigate validation, storage and transaction errors first.
A `ready/pending` record is in extraction. See [troubleshooting](troubleshooting.md).
