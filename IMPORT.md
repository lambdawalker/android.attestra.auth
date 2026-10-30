# Integrate Attestra auth from source

**Development/source only. No confirmed stable auth package is available.**
The repository has no auth publication configuration or GitHub release catalog
at this audit. The sample APK's `versionName = "1.0"` is not a library release.
No Maven Central/JitPack installation coordinate is advertised for `:auth`.

## Supported workspace

Clone this repository and build its existing Gradle workspace. The runnable
`:app` consumes `:auth` with:

```kotlin
implementation(project(":auth"))
```

Use [quickstart](docs/agents/quickstart.md) and the complete compiled host in
[MainActivity](app/src/main/java/com/apexfission/android/attestra/auth/MainActivity.kt).
Do not copy the `auth/` directory into an unrelated Gradle build without adapting
its root convention plugins, `buildSrc` model-asset task and dependency catalog.
That standalone transplant is not a verified distribution path.

## Toolchain and dependencies

Android API 28 minimum; compile SDK 37; Java 17 bytecode. The wrapper, AGP, Kotlin,
Compose and dependency pins are authoritative in [the catalog](gradle/libs.versions.toml),
[wrapper](gradle/wrapper/gradle-wrapper.properties) and [auth build](auth/build.gradle.kts).
The pinned detector and model tags share a JitPack artifact identity. The build
resolves model assets in an isolated, nontransitive configuration; do not install
the model tag alongside the detector as competing runtime dependencies.

## Future release evidence

Before adding an auth package, record confirmed destination, exact coordinates,
version, immutable code/documentation identities and public artifact evidence in
`sites/versions.json`. Preserve old contracts and installations. The current site
rejects unimplemented release snapshots rather than silently rendering current
code under a historical version. Package publication and finalization automation
are not implemented here and are outside this documentation task.
