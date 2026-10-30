# Agent evaluation tasks

Scope: Development/source. An agent should solve these from Markdown without
inventing released artifacts. Source-linked Gradle demos/tests provide compile evidence;
deployed/device behavior must be checked separately.

| Task | Expected choice / behavior | Reject if | Validation |
| --- | --- | --- | --- |
| Install auth | Read IMPORT; use source workspace | Guesses a Maven/JitPack auth version | Build `:app` |
| Start onboarding | `EmailOnboardingHost` with validated policy and trusted origin | Uses empty catalog hooks as auth | Sample compile |
| Handle cross-device email | Code C + B when A is absent | Confirms with B alone | Email controller tests |
| Restore a session | Bind storage to backend/account | Reuses tokens after host change | Environment tests |
| Add passkeys | Platform response then backend completion | Treats cancelled sheet as registered | Passkey tests/device |
| Add optional capture | Carry reviewed policy to `CaptureHost` | Silently accepts changed consent | Capture tests |
| Own both JPEG arrays | Transfer and let controller wipe | Retains/logs/reuses bytes | Capture ownership tests |
| Recover failed extraction | Empty manual form | Uses partial failed provider fields | Capture tests |
| Finish save | Matching server `reviewed` fields/origin | Uses `id_captured` as save proof | Destination tests |
| Diagnose finalizing stall | Validation/storage/ready transaction logs first | Assumes the model was already called | Backend correlation |
| Clear device data | Local reset only, explain remote limits | Claims remote passkeys/evidence deleted | Signing/reset tests/device |
| Render docs | Offline deterministic fixtures and reviewed baseline workflow | Regenerates baseline during ordinary validation | Screenshot inventory/CI |
| Use older version in Spanish | State no confirmed auth archive; do not invent one | Presents current code as an old release | Site catalog tests |
| Handle stale translation | Explicit English fallback in same Development scope | Silently routes to unrelated revision | Site fallback tests |
