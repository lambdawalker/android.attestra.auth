# Runtime app settings

Install an app built with this change once. Afterward, publish settings at `https://admin.attestrabond.com` and reopen the app to refresh them. Changing `attestraCaptureEnabled` in an old APK cannot update that installation.

Both fixed API and index-discovered builds fetch `GET <apiUrl>/v1/app-config` without credentials on launch and foreground. The environment remains selected at build time. Runtime settings never change the API host, Cognito identity, App Links, signing key, or package name.

The public schema is:

```json
{"schemaVersion":1,"environment":"qa","revision":1,"updatedAt":"2026-10-27T00:00:00Z","features":{"idCapture":true},"capture":{"documentType":"driver_license","jurisdiction":"US","purpose":"Onboarding"}}
```

The backend supplies revision-zero deployment defaults until an administrator publishes an override. Later deployments must preserve that override. An enabled policy requires a real document type, jurisdiction and purpose.

Settings are cached privately for at most 15 minutes. Only transport errors, HTTP 429 and 5xx can use a still-valid cache. Invalid, mismatched, oversized or unsupported responses disable new capture and remove the cached setting. Cache expiry disables admission, including after the device sleeps. Existing captures remain accessible for status and cancellation. The backend independently enforces its current policy on each new capture.

The capture API returns a content-based `policy_version`. The controller sends it as `expected_policy_version` and persists it with the create operation key. If the disclosure changes before creation, the app displays the changed policy and requires another Start action. The server rejects a stale policy with HTTP 409 `policy_changed`; an idempotent replay of an already-created record retains that record's original policy.

## Verification

Run `bash gradlew :auth:testDebugUnitTest :identity-mock:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`. Device checks: publish enabled/disabled policies, background and reopen, go offline longer than 15 minutes, verify status and cancellation while disabled, and change purpose while the Start screen is open.
