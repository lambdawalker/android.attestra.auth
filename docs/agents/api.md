# API selection and contracts

Scope: Development/source. Exact declarations: [generated API inventory](api-inventory.md).
Source and tests outrank prose; service serialization belongs to the
[backend](https://raw.githubusercontent.com/lambdawalker/go.attestra.aws.auth/main/docs/agents/index.md).

## Preferred integration surface

| API / import package | Use for | Preconditions, effects and errors |
| --- | --- | --- |
| `EmailOnboardingHost` · `com.apexfission.android.attestra.auth.email` | Interactive auth/onboarding | Activity-scoped Compose owner, trusted API origin, validated runtime policy and link callbacks. Owns its remembered client/storage/controller; closes its client on disposal. Default ID slot is unavailable UI. |
| `parseVerificationLink` · `...email` | Validate incoming Android URI origin | HTTPS, expected host, `/verify-email`, default/443 port. Returns null on origin mismatch; the controller still validates proof format. Never trust arbitrary intent links. |
| `EmailFlowController` · `...email` | Custom email UI | Inject `EmailVerificationGateway`, `AuthStorage`; optional proof/clock inputs. Mutex-serialized suspend mutations; sessions are stored only after backend success. `screen` is observed Compose state. Do not equate request acceptance with verification. |
| `EmailVerificationApi` / `EmailHttpClient` · `...email` | Email HTTP transport | Caller owns the client; HTTPS origin. Returns structured session or `EmailApiError`; do not retry confirmation blindly without controller recovery. |
| `ReturnAuthApi` · `...passkey` | Existing-user email/passkey/password sign-in, refresh, status | Caller supplies/owns client and session handling. Error status does not imply proof or password may be logged. Account completion comes from server status. |
| `PasskeyRegistrationApi` / `AndroidPasskeyManager` · `...passkey` | Platform passkey enrollment and sign-in | Registration needs bearer authorization; manager needs Activity context and a configured provider. `PasskeyCancelled` and `PasskeyUnsupported` are distinct from success. Backend completion must follow platform response. |
| `EnvironmentIndex` · `...discovery` | Trusted public environment discovery | Caller owns anonymous client/cache. Cache key isolates index URL/environment; see discovery guide for bounded fallback and invalidation. |
| `AppConfiguration` / `OnboardingPolicy` · `...discovery` | Runtime requirements | Validate policy and environment binding. Invalid settings block new admission. `next()` uses server facts; local skips never bypass a required step. |
| `CaptureHost` / `CaptureController` · `...capture` | Stateful two-sided capture and review | Remember controller per account/backend. Host owns clients and checkpoint namespace; UI host disposes controller. `startCaptureOnEntry` defaults false, `requireReviewedPolicy` false: live sample deliberately passes true for both and carries reviewed disclosure. |
| `CaptureApi` / `CaptureClients` · `...capture` | Capture status, mutation and private PUT adapters | Separate anonymous upload client; validate returned S3 origin/headers. API client bearer token must belong to bound account. Caller closes both clients. |
| `IdentityCamera` · `...identity.capture` | Permission-gated detector integration | Real camera/device, model assets and explicit user entry. Transfers front/back JPEG arrays to callback. No successful identity decision is emitted. |
| `SecureAuthStorage` · `...email` | Local installation data | Keystore-backed encryption; unreadable values are absent. `clearLocalUser` is local-only. Host manifest must exclude sensitive preferences from backup. |

## Capture mutation details

`continueToCapture()` resumes an existing record before creating evidence.
`capturedBoth(front, back)` takes ownership of both arrays; unfinished upload bytes
remain only in memory for retry. `finalizeCapture()` records a stable operation
intent first; `restore()` reconciles uncertain responses. `retry()` is for eligible
backend failures, not a bypass for invalid/expired evidence. `cancel()` requests
backend deletion; `close()` only disposes local resources and is not deletion.

`editCard()` changes a local draft. `saveReview()` validates nonempty bounded fields
and supported origin, sends the recorded revision, then consumes server acknowledgement.
Conflict/retry paths retain dirty values. The finish screen requires the exact stored
fields and origin, not just `id_captured`. Metadata diagnostics may include capture ID,
state, revision and HTTP status; do not attach raw exceptions or personal values.

## UI-only versus runtime API

`ui.onboarding.*` screen composables accept callbacks and state; those callbacks do
not implement authentication. `OnboardingBusinessActions` is a UI-catalog placeholder.
Do not use its empty hooks as a production integration. `IdentityDetails` and older
identity-result mockups describe UI prototypes; they are not the capture HTTP model.
`CaptureHostContent`, `CardReviewForm`, sample entry screens and configuration locals
are internal rendering/host seams, not supported external integration contracts.

`CardData`, `CaptureRecord`, `CapturePolicy`, checkpoint and request models are
serializable transport/state types. Do not construct a forged `ready` or `reviewed`
record to make UI finish. Keep untrusted card text as text; validation does not establish
identity truth. See [capture guide](../identity-capture.md) for exact state handling.
