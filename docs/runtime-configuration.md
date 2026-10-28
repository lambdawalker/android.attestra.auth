# Runtime app settings and onboarding

Deploy the matching backend and Android client together. Publish settings through the existing admin console review, history and rollback flow. Runtime settings never change the API host, Cognito identity, App Links, signing key, or package name.

Both fixed API and index-discovered builds fetch `GET <apiUrl>/v1/app-config` without credentials on launch and foreground. The additive public schema remains version 1:

```json
{
  "schemaVersion": 1,
  "environment": "qa",
  "revision": 1,
  "updatedAt": "2026-10-28T00:00:00Z",
  "features": {"idCapture": true},
  "capture": {"documentType": "driver_license", "jurisdiction": "", "purpose": "Onboarding"},
  "onboarding": {
    "idCapture": "required",
    "passkey": "required",
    "password": "disabled",
    "passwordPlacement": "hidden",
    "passwordPolicy": {
      "minLength": 12, "maxLength": 128,
      "lowercase": "allowed", "uppercase": "allowed",
      "numbers": "allowed", "symbols": "allowed"
    }
  }
}
```

Each enrollment requirement is `required`, `optional`, or `disabled`. Password placement is `with_email`, `after_verification`, or `hidden`; disabled password and hidden placement must occur together. New defaults require ID capture and passkeys and disable password enrollment. Existing documents without `onboarding` retain legacy behavior: capture is optional when `features.idCapture` is true and disabled otherwise, passkeys are required, and passwords are disabled. Explicit malformed onboarding fails closed. The compatibility capture flag must match the new requirement. An enabled capture requires a real document type and purpose; jurisdiction can be empty for a generic policy.

Password lengths are 8–128 inclusive, with minimum no greater than maximum. Lowercase, uppercase, numbers, and printable ASCII punctuation each support `allowed`, `required`, or `disallowed`; at least one class must be enabled. Whitespace and non-ASCII characters are unsupported. UI requirements and a local Weak/Fair/Strong strength estimate accompany the masked field. The estimate considers length and character diversity and penalizes repeated, common, and sequential values; it is independent of policy compliance and is not a breach check or guarantee. The server validates the configured rules before assigning the password in Cognito.

## Session and completion authority

`EmailOnboardingHost` receives the validated, unexpired `OnboardingPolicy` and a settings retry callback. Missing or expired configuration blocks onboarding completion. Required steps cannot be skipped; optional steps can be skipped in memory; disabled steps are omitted. Stored legacy `idCheckDeferred` values are never completion evidence.

`POST /auth/status` returns `passkey_registered`, `password_registered`, `id_captured`, and `onboarding_complete`. Missing fields default to false. The app requires both server completion and all locally known required facts before showing completion. It refreshes status after password/passkey enrollment and every capture exit, including cancellation/back. A signed-in session or a capture callback alone cannot complete setup. Document capture does not assert identity verification.

`POST /auth/password/set` sends bearer access authorization and `{password}` only after email verification. A with-email password is bound to the normalized signup email and request ID, then automatically submitted only after that request verifies the matching session account. An optional blank entry skips password setup for this journey. The password lives only in Compose `remember` memory, never saved state, storage, URLs, proofs or logs. It is cleared on email changes, back/exit, sign-in session changes, disabled policy and submission. Process death or a cross-device email confirmation prompts for it again. Setup rechecks the policy after verification. A status 401 attempts token refresh once and then returns to sign-in if authorization remains expired.

Returning users can choose password sign-in using `POST /auth/password/start` (`{email}`), then `POST /auth/password/complete` (`{email,session,password}`). Password sign-in remains available when new password enrollment is disabled. Email OTP remains available for recovery.

## Cache and capture lifecycle

Settings are cached privately for at most 15 minutes. Only transport errors, HTTP 429 and 5xx can use a still-valid cache. Invalid, mismatched, oversized or unsupported responses remove the cached setting and pause setup. Cache expiry disables new admission, including after device sleep. Existing capture records remain server-owned; the backend independently enforces current policy on each new capture.

The capture API returns a content-based `policy_version`. The controller sends it as `expected_policy_version` and persists it with the create operation key. Changed disclosure requires another Start action. HTTP 409 `policy_changed` rejects stale policy; an idempotent replay retains the original record's policy.

## Verification

Run `bash gradlew :auth:testDebugUnitTest :identity-mock:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`.

Device checks: exercise all three requirements and password placements; reject invalid password classes and lengths; cancel required capture and confirm setup remains incomplete; recover from process death during email confirmation; change email after entering a password; sign in with an existing password after enrollment is disabled; publish a stricter policy mid-session; expire settings offline and retry; inspect saved state/logs for absence of password values. Credential Manager, camera, process recreation and real backend/Cognito checks require a device and deployed matching backend.
