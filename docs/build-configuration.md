# App build configuration

`app/build.gradle.kts` declares the app's identity, SDK levels, dependencies and UI build features. Feature configuration is in precompiled Kotlin script plugins, automatically compiled by Gradle from `buildSrc`:

| Plugin / script under `buildSrc/src/main/kotlin` | Responsibility |
|---|---|
| `attestra.endpoints.gradle.kts` | Environment selection, index/static API URL, App Link host, exported Cognito/capture settings and BuildConfig fields |
| `attestra.signing.gradle.kts` | Optional persistent signing identity for debug and release |

The app applies `id("attestra.endpoints")` and `id("attestra.signing")`. Each convention applies the Android application plugin idempotently. AGP's version still has one source: `gradle/libs.versions.toml`, imported by `buildSrc/settings.gradle.kts`. The other Android modules use unversioned plugin IDs because buildSrc supplies AGP on the shared build classpath.

## Endpoint settings

Every public setting uses the same precedence: **Gradle project property → selected exported file → script default**. Gradle project properties include `-P` and normal Gradle properties files. An explicit empty value remains an override; required export fields still reject blanks.

`attestraConfigFile` is a Gradle property pointing to a backend-generated UTF-8 `.properties` file. Relative paths resolve from the repository root. The file must exist and retain the existing required environment/API/host/region/Cognito/capture fields.

| Property | Default without export/override |
|---|---|
| `attestraEnvironment` | `dev` |
| `attestraIndexUrl` | `https://index.attestrabond.com/v1/environments` |
| `attestraApiBaseUrl` | Empty: discover using the index |
| `attestraLinkHost` | `<environment>.attestrabond.com` |
| `attestraAwsRegion` | Empty |
| `attestraCognitoUserPoolId` | Empty |
| `attestraCognitoClientId` | Empty |
| `attestraCaptureEnabled` | `false` |
| `attestraCaptureDocumentType` | `sample_card` |

Do not store secrets in this exported file: these values become app configuration. A nonblank API URL bypasses index discovery. Existing URL, environment and boolean validation is preserved.

```powershell
.\gradlew.bat :app:assembleDebug '-PattestraEnvironment=qa'
.\gradlew.bat :app:assembleDebug '-PattestraConfigFile=D:/config/qa.properties'
```

`attestraCaptureEnabled` and `attestraCaptureDocumentType` remain accepted for export compatibility. Live app behavior comes from [runtime settings](runtime-configuration.md), including in fixed API mode.

## Signing settings

Signing inputs use **process environment variable → root `local.properties` → default**, independently for each setting. Use the same uppercase names in both sources. An explicitly empty environment value overrides the file; an empty keystore path disables registered signing. Signing values are never read from `-P`, `gradle.properties`, or the endpoint export.

| Environment variable / local.properties key | Behavior |
|---|---|
| `ATTESTRA_KEYSTORE_PATH` | Enables registered signing; relative paths resolve from the repository root |
| `ATTESTRA_KEYSTORE_PASSWORD` | Required when the path is supplied |
| `ATTESTRA_KEY_ALIAS` | Defaults to `attestra` |
| `ATTESTRA_KEY_PASSWORD` | Defaults to the store password |

For Android Studio, add the following to the repository-root `local.properties`, preserving any existing `sdk.dir` line. This file is already ignored by Git. Use forward slashes in Windows paths (or double each backslash); Java properties interpret single backslashes as escapes.

```properties
ATTESTRA_KEYSTORE_PATH=C:/Users/D/.attestra/signing/dev.p12
ATTESTRA_KEYSTORE_PASSWORD=your-keystore-password
ATTESTRA_KEY_ALIAS=attestra
# Optional if different from the store password:
# ATTESTRA_KEY_PASSWORD=your-key-password
```

File-stored passwords are plaintext; you can keep only the path/alias here and supply passwords through the environment instead. Never commit the file. Resync Studio after changes. Registered signing also requires `org.gradle.configuration-cache=false` in your user Gradle properties for IDE builds; `--no-configuration-cache` applies to command-line builds.

Without a nonblank keystore path, debug keeps its normal Android debug key and release remains unsigned. With a path, both use `registered`. Missing file/password fails clearly. Pass `--no-configuration-cache`; the convention refuses registered signing with configuration caching enabled. Keep keystores outside the repository and obtain secrets from a trusted local secret provider/session. No passwords are committed by this change.

```powershell
# After supplying the signing variables in the current process:
.\gradlew.bat --no-configuration-cache :app:assembleDebug '-PattestraEnvironment=dev'
```

Studio must inherit these variables when launched; typing them in the embedded terminal doesn't update the IDE process. The signing convention does not integrate the separate signing-vault plugin or install its dependencies. Register the chosen certificate in the environment's assetlinks.json before testing App Links/passkeys.

## Adding another feature

Add a focused `attestra.<feature>.gradle.kts` under the same buildSrc source directory and apply its ID in the app. Keep inputs, validation and generated Android configuration together. Do not add credentials to the public endpoint resolver. Do not duplicate dependency/plugin versions; use the shared catalog in the buildSrc build script.

CI runs `scripts/verification/verify_conventions.py`: default/index mode, exported values, explicit overrides, invalid inputs, signing-cache rejection and a real APK signed with a disposable certificate. This does not deploy infrastructure or register that certificate with any environment.

## Signed debug variant

When the configured keystore exists and its store password, alias and key password unlock a private key with a certificate, sync creates **signedDebug**. Select it in Android Studio's Build Variants window, or run:

```powershell
.\gradlew.bat --no-configuration-cache :app:assembleSignedDebug '-PattestraEnvironment=dev'
```

It is debuggable, shares the debug capture-mock source and dependencies, and resolves library dependencies through their debug variants. It keeps the same application ID so approved App Links/passkey associations still match. It does not select a server environment by itself.

Without a configured path the variant is absent. Invalid configured credentials fail sync with an actionable error; they do not silently fall back to the standard debug key. Correct the values and resync. Existing debug/release signing behavior is unchanged. Validation supports Java-detectable JKS/PKCS12 files and checks credentials locally without uploading them.
