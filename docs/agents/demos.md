# Runnable demos and UI evidence

Scope: Development/source. Build [the complete sample](../../app/src/main/java/com/apexfission/android/attestra/auth/MainActivity.kt)
using [quickstart](quickstart.md).

| Entry / scenario | Prerequisites and actions | Expected result / failure | Source |
| --- | --- | --- | --- |
| Open onboarding | Matching backend/settings; request email, follow link or enter code, register passkey or permitted password | Server-confirmed auth/account state; recover cancelled sheets and stale settings | `MainActivity`, `EmailOnboardingHost` |
| Open UI catalog | Debug or release sample; choose each screen | State-driven UI only, no backend mutation. Older identity-decision screens are design mockups | `OnboardingCatalog` |
| Test ID capture | Debug sample and real camera; sample card front/back, retake/confirm | Synthetic backend outcomes, processing/manual review and save; no real identity decision | Debug `IdentityRuntime`, `MockCaptureServer` |
| Live ID capture | Signed-in matching account, current disclosure and policy | Private upload, async checks/extraction, manual fallback, exact server-confirmed save | `LiveCaptureFlow`, `CaptureHost` |
| Screenshots | Host renderer, no emulator/service/credentials; run named fixture suite | Synthetic component-preview images and machine manifest; failure if scenarios are missing | [Screenshot maintenance](../screenshots.md) |

Inspect **loading/error/recovery**, both capture sides, retake, required/optional
policy, failed upload, failed extraction, failed save and re-entry in relevant demos.
A static image is not proof that App Links, camera focus or platform permissions work.

## Reviewed examples

![Email setup, synthetic light-theme preview](../assets/screenshots/email-start-light.png)

![Reading a document, synthetic dark-theme preview](../assets/screenshots/read-document-dark.png)

![Server-confirmed saved-details screen, synthetic light-theme preview](../assets/screenshots/details-saved-light.png)

Images show implemented components with synthetic inputs, not real account results.
See the capture manifest for hashes, source fixtures and renderer/toolchain provenance.
