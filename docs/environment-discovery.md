# Environment discovery

The public schema-v1 endpoint is `https://index.attestrabond.com/v1/environments`. The app defaults to environment `dev`; build with `-PattestraEnvironment=qa` or another registered name. Environment selection is fixed per build, not a user-controlled production server picker.

The response contains `schemaVersion` and `environments`, each with `id`, `configuration`, `configHash`, `revision`, and `updatedAt`. Configuration supplies `apiUrl`, `awsRegion`, `cognitoUserPoolId`, `cognitoClientId`, and `features.idCapture`. Existing email, passkey, sign-in and capture clients append their existing paths to this API base. Cognito values are exposed to library consumers and included in the app's saved-session binding; the demo continues authenticating through the Go API rather than calling Cognito directly.

The index capture flag is retained for compatibility but no longer enables capture in this app. Both modes fetch [runtime settings](runtime-configuration.md) from the resolved API.

## Index discovery versus a fixed API URL

These are alternative configuration modes. A custom domain is usable in either mode; DNS naming and index discovery are separate concerns.

| Mode | Configuration | Result |
| --- | --- | --- |
| Index discovery (default) | Leave `attestraApiBaseUrl` unset; environment defaults to `dev` | Fetch that environment's API URL, Cognito IDs and region from the index |
| Fixed API URL | Set `attestraApiBaseUrl=https://dev.api.attestrabond.com` or an AWS `https://…execute-api.us-east-2.amazonaws.com` URL | Skip the index and use the fixed API URL |

The `execute-api` hostname is AWS API Gateway's direct address. It is not the index endpoint. A fixed URL is useful for explicit testing or installations without an index; use discovery for normal Attestra builds.

For the default dev build on Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

Remove any old `attestraApiBaseUrl` and `attestraLinkHost` overrides from project or user-level Gradle properties (including `%USERPROFILE%\.gradle\gradle.properties`) to use the defaults. Do not pass `attestraConfigFile` when you want discovery: exported files contain a fixed API URL. Rebuild and reinstall after changing build properties. Updating an existing installation can clear its saved authentication when its backend binding changes.

The default `dev` value lives in `buildSrc/src/main/kotlin/attestra.endpoints.gradle.kts`, rather than an active project property, so a QA/prod export can still supply its own environment. Command-line `-PattestraEnvironment=qa` explicitly selects QA.

## Library use

```kotlin
val client = environmentIndexClient()
try {
    val resolved = EnvironmentIndex(client, yourEnvironmentCache)
        .resolve(environment = "qa")
    val config = resolved.entry.configuration
    // Construct EmailVerificationApi, ReturnAuthApi and CaptureApi with config.apiUrl.
    // Resolve AppConfiguration separately before enabling new capture.
} finally {
    client.close()
}
```

`EnvironmentCache` stores public JSON, not tokens. The resolver has no implicit Android storage dependency. The demo uses private SharedPreferences excluded from backup. Treat the configured HTTPS index as trusted routing authority. No AWS credential, GitHub token, API key or bearer token is sent to the index. `configHash` identifies server configuration; it is not an authentication signature. TLS authenticates the index host.

## Failure and lifecycle rules

- Fetch before starting authentication; show loading, failure and Retry. Retain one resolved configuration for the ViewModel lifetime; never replace a host midway through an active flow.
- On transport failure, HTTP 429 or 5xx, use a validated cache for the exact index URL/environment only, up to 24 hours. Show that cached settings are in use. Backend authentication/authorization still applies; cached flags cannot bypass it.
- Missing environments, malformed/unsupported documents and other HTTP errors invalidate the cache and block access. A removed environment does not silently fall back to old settings after a successful index response.
- HTTPS is required, with no credentials, query or fragment. The built-in discovery client disables redirects and sets timeouts. Injected clients must preserve these constraints and must not carry authentication headers.
- When index/environment, API URL, region, Cognito pool or client changes, the demo clears local authentication/proofs before using the new configuration. Upgrading a legacy installation clears its unbound saved auth once. Capture-only flag changes do not force sign-out. Applications embedding `:auth` must apply equivalent isolation before constructing their authenticated clients.
- Explicit `attestraApiBaseUrl` (including an exported configuration file) selects static mode. It never serves as an automatic fallback for failed discovery.

The index does not currently publish App Link hostnames or capture document-type policy. The manifest uses `attestraLinkHost` (default `<environment>.attestrabond.com`); capture obtains its supported document policy through the existing capture API. Discovery does not enable parsing or identity validation.

## Verification

`EnvironmentIndexTest` covers the backend response shape, environment/cache isolation, cache expiry, environment removal, invalid documents, HTTP failure handling and cancellation. Run `bash gradlew :auth:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`. No real identity or email is needed. Device smoke testing should cover launch, temporary offline restart, retry and a verified email App Link for the selected environment.
