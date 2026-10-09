# Onboarding ID capture

Implements the capture-only contract in [go.attestra.aws.auth/docs/id-capture.md](https://github.com/lambdawalker/go.attestra.aws.auth/blob/codex/id-capture-s3/docs/id-capture.md), following [design.attestra](https://github.com/lambdawalker/design.attestra/tree/main/auth/onboarding/id-capture). Parsing and standalone identity validation are not implemented. The previous combined verification mock is replaced.

`:auth` owns `capture/CaptureApi`, `CaptureController`, `CaptureHost` and encrypted account/environment checkpoint storage. The existing `auth.identity.capture` package owns permission handling, bundled card detection, front/back preview, retake and JPEG conversion. Following the latest repository consolidation, `:auth`, the demo app and debug-only `:identity-mock` require API 28. The existing published Apexfission permission and card-detector dependencies are reused.

## Live flow

Configure `-PattestraApiBaseUrl=https://YOUR_API_HOST` and the existing sign-in/link settings. Both debug and release authenticated onboarding use the real capture API. Capture can remain disabled server-side until policy and deployment checks are complete. A policy supplies the document type, purpose, jurisdiction, limits and retention. The initial UI handles only a front/back JPEG card policy.

The flow obtains an account-bound capture, acquires and previews each side, registers checksum/size, PUTs directly to private S3, and requests asynchronous finalization. A dedicated S3 client has no auth or logging plugin and follows no redirects. The app never receives AWS credentials or document read URLs. API authorization always uses the current account's access token. Unverified JWT claims are used only for local cache partitioning, never server authorization.

“Document captured” means uploaded files have passed server file checks. No extraction, approval, rejection, or feature unlock is produced. Finalization polls every five seconds for one minute in the foreground, then allows manual refresh. Continue later preserves server work. Explicit cancellation calls the backend and requests deletion; it is asynchronous.

Images stay in protected memory, are wiped after upload/disposal, and are never written to gallery, preferences, checkpoints, mock state or backups. Capture windows use `FLAG_SECURE`. Permission denial and preview retakes keep the camera flow recoverable. The encrypted no-backup checkpoint contains capture/create/finalize/retry identifiers only. Create/finalize/retry intent is saved before sending. On process restart the app discovers current server status; finalizing/ready captures recover without recapture. Unfinished uploads conservatively require both sides to be captured again because the API does not expose verified per-slot upload completion. Expired authentication returns to sign-in, keeping the account-scoped checkpoint.

## Offline test flow

The existing startup test menu and local-user reset remain available. Its direct capture entry opens the camera, while the scenario demo offers failure cases; both are debug-only. It uses two in-process Ktor MockEngine clients with the same API/S3 contract; no network deployment, real token, or image persistence. It is conspicuously labelled LOCAL CAPTURE MOCK and intended for sample cards. Scenarios: ready, pending checks, invalid image, one failed upload, lost finalize response, and retryable file-check failure. “New sample capture” clears the mock session and checkpoint. Release includes only live capture.

## Verification

Run `bash gradlew :auth:testDebugUnitTest :identity-mock:testDebugUnitTest :app:assembleDebug :app:assembleRelease`. Tests cover create-response loss, stable upload retry and byte cleanup, finalize-response recovery, late camera callbacks, SHA-256 encoding, HTTP mock persistence and API-token separation from uploads.

Device gate: camera permission denial/permanent denial, no camera, preview/retake each side, rotations/process death, leaving during upload/finalization, account switching, offline/expired signed URL retry, sign-in recovery, and clear/glare/blur sample-card captures. Live integration additionally requires the Go/S3 smoke gate. No emulator/device result is implied by JVM tests.
