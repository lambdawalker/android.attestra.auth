# Quickstart

Scope: Development/source. First read [installation](../../IMPORT.md).
The smallest verified integration is the repository's `:app` host; it includes
App Links, environment isolation, foreground refresh, secure storage and capture.

## Build and run

```sh
git clone https://github.com/lambdawalker/android.attestra.auth.git
cd android.attestra.auth
./gradlew :app:assembleDebug -PattestraEnvironment=qa
./gradlew :app:installDebug -PattestraEnvironment=qa
```

Connect an API-28-or-newer device/emulator with USB debugging. Start **Attestra**.
Use **Open onboarding** for the matching deployed environment, **Open UI catalog**
for offline visual states, or **Test ID capture** in a debug build for synthetic
backend outcomes with a real camera. Camera and Credential Manager require device
checks; a rendered preview cannot exercise them.

Do not use real identity documents for screenshots or routine fixture tests.
The offline capture demo does not verify identity or contact the production service.

## Embed the host UI

The existing `:app` dependency is `implementation(project(":auth"))`.
This is the host call shape (a fragment, not a standalone Activity):

```kotlin
EmailOnboardingHost(
    apiBaseUrl = config.apiUrl,
    onboarding = config.onboardingPolicy(),
    onRetryConfiguration = environment::refreshRuntimeConfiguration,
    link = incomingLink,
    onLinkConsumed = ::clearIncomingLink,
    identityContent = { passkeyAdded, policy, exit ->
        AccountIdentityFlow(passkeyAdded, policy, exit)
    },
    onPasskeyDeferred = {
        destination = EntryScreen.Home; clearIncomingLink()
    },
)
```

For a complete compiled implementation and imports, read
[MainActivity](../../app/src/main/java/com/apexfission/android/attestra/auth/MainActivity.kt),
[EnvironmentViewModel](../../app/src/main/java/com/apexfission/android/attestra/auth/EnvironmentViewModel.kt)
and [LiveCaptureFlow](../../app/src/main/java/com/apexfission/android/attestra/auth/LiveCaptureFlow.kt).
`AccountIdentityFlow` is sample-host wiring, not an exported auth-library symbol.
Use the same responsibilities in your host instead of importing sample internals.
The default `identityContent` shows an unavailable message, not a capture implementation.

## Required host responsibilities

1. Resolve the trusted environment/index, fetch valid runtime settings and retain
   them for this backend/account binding. See [discovery](../environment-discovery.md)
   and [runtime configuration](../runtime-configuration.md).
2. Run content inside `AttestraAuthTheme` and an Activity-scoped Compose owner.
   Credential Manager needs the Activity context; storage uses application context.
3. Accept only the configured HTTPS `/verify-email` origin. Parse with
   `parseVerificationLink(uri, expectedHost)`; consume each link after handling.
4. Configure manifest App Links, the installed package and certificate association.
   See [signing](../android-signing.md). Discovery does not configure App Links.
5. Route exits and account-status refreshes. Do not mark onboarding complete locally.

## Verify

```sh
./gradlew :auth:testDebugUnitTest :identity-mock:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

Then verify a real email link, code fallback, cancelled passkey sheet, capture
retakes, processing failure/manual entry and save/re-entry on a device against the
matching backend. Local compilation is not proof of email delivery or AWS readiness.
