# QA & Testing

## Choose the right build for the test

Verified Android App Links require an approved signing certificate, including the **email-verification onboarding link**. This requirement is not limited to passkeys. Every developer or tester exercising these flows needs an installed APK signed by a certificate approved for the target environment.

| Test | Build and environment requirements |
|---|---|
| UI catalog and isolated unit tests | Ordinary debug builds are sufficient; no deployed domain association is needed. |
| Email link opening the app | Approved signing certificate, matching manifest hostname, published association, and device-side App Links verification. |
| Passkey registration and sign-in | Approved signing certificate and credential association, compatible relying-party configuration, and a configured device credential provider. |
| ID capture or another flow reached through onboarding | Use the approved build when the test includes the email App Link; capture itself does not introduce an App Links requirement. |

Use **signedDebug** for local end-to-end tests. The variant appears after Gradle validates the configured keystore, alias and passwords. A variant name alone is not proof of identity: verify the actual APK and installed app. In this project, configuring the persistent key also signs debug and release with that key; without it, ordinary debug uses its default identity. See [build configuration](build-configuration.md).

## Personal developer identities

Each developer keeps a personal keystore and password. Teammates do not need to share one private key. Dev and QA can approve multiple certificate fingerprints for the same application ID, `com.apexfission.android.attestra.auth`.

1. Run `signing.bat` on Windows or `./signing.sh` on Linux/macOS. Generate a key once; use Export for an existing key.
2. Select the intended environment. The wizard exports public JSON and copies it to `~/.attestra/associations/<environment>/developer-associations.json`.
3. In the Go repository, run setup for that environment. For an already-completed environment, use `setup.bat -force-setup`. Review the discovered package and fingerprints before approving them. Setup merges additions without removing existing approvals and deploys the public association file. This requires the backend domain-associations feature.
4. Configure signing in Android Studio using the [signing guide](android-signing.md), sync Gradle and select **signedDebug** in Build Variants. Select the correct environment through the documented Gradle settings or exported environment properties.
5. Build and install that variant on the intended device. Use signing wizard option 3 to check the APK against the published association, then check device verification below.

Only the public JSON is transferred to the environment maintainer or another computer. Keep private keys and passwords private. Production must have its own approved production signing identities; do not copy the development allowlist into production. Removing an entry withdraws its association after caches refresh; it does not prevent APK signing or revoke backend sessions.

## Check the exact hostname

For dev, the email link is `https://dev.attestrabond.com/verify-email?...` and the association must be published at:

```text
https://dev.attestrabond.com/.well-known/assetlinks.json
```

For QA, use `qa.attestrabond.com`. Publishing on the apex `attestrabond.com` does not substitute for the subdomain's association. The built manifest hostname, link hostname, application ID and installed signing certificate must agree. Index discovery cannot change the build-time manifest hostname.

The file must return HTTPS 200, JSON content type, and no redirects. It includes `delegate_permission/common.handle_all_urls` for App Links and `delegate_permission/common.get_login_creds` for credentials. DNS/hosting readiness and Android device verification are separate checks; successful backend setup does not prove an already-installed app has reverified the domain.

## Verify on a device (Android 12+)

In PowerShell, locate ADB and select the intended device:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices -l
$serial = "emulator-5554" # replace with the first-column serial of your device
$package = "com.apexfission.android.attestra.auth"
& $adb -s $serial shell pm get-app-links --user cur $package
```

If your SDK is elsewhere, use Android Studio's Android SDK settings to locate it. Install SDK Platform-Tools if necessary. Enable USB debugging and accept the phone's authorization prompt. Always use `-s` when multiple devices or emulators are attached.

Check that the domain is `verified`, link handling is allowed, and the signature matches the published fingerprint. If installation happened before the DNS or association file was ready, reset verification and retry:

```powershell
& $adb -s $serial shell pm set-app-links --package $package 0 all
& $adb -s $serial shell pm verify-app-links --re-verify $package
# Wait a few minutes, then inspect again:
& $adb -s $serial shell pm get-app-links --user cur $package
```

Check **Settings → Apps → Attestra → Open by default → Open supported links** if handling is disabled. The signing wizard verifies an APK file and the website; it does not verify what is installed on the selected phone.

Test routing without using a real verification token:

```powershell
& $adb -s $serial shell am start -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d 'https://dev.attestrabond.com/verify-email'
```

This checks whether Android routes to the app; without a request and proof, it cannot confirm an email. Do not add a package or component restriction to this routing test: that would bypass normal link resolution. Then request a fresh signup email and tap its link on the intended device.

## Troubleshooting and acceptance checks

| Symptom | Check next |
|---|---|
| Browser opens; wrong fingerprint is installed | Rebuild/install the intended signed variant using the approved personal key. |
| Expected domain is absent from `get-app-links` | Inspect the built/merged manifest and environment/link-host settings. Rebuild for the intended environment. |
| Domain is present but not verified | Check device connectivity, public DNS/HTTPS/JSON, package and certificate; then reverify. |
| Domain is verified but email opens the browser | Check Open supported links, mail-client in-app browsing, and whether the mail service rewrites the URL onto another host. |
| Browser fallback returns an error | Association hosting only serves the well-known file; it does not provide a `/verify-email` web page. Website fallback is a separate deployment. |
| APK cannot replace the installed app | Check application ID and signing identity. Switching certificates may require uninstalling, which removes local app data. |

Before considering onboarding verified, exercise a fresh same-device email, resend/newest-email behavior, expired/invalid links, app restart, and relevant passkey/OTP flows. Cross-device email confirmation needs a deployed browser fallback; do not count publishing assetlinks.json as implementing that page. Record the environment, variant, installed fingerprint, Android version, device verification output and observed result. Do not include live email proofs, OTPs or passwords in reports.

Official references: [Verify Android App Links](https://developer.android.com/training/app-links/verify-applinks) and [Android Debug Bridge](https://developer.android.com/tools/adb).
