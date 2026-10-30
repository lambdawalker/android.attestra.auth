# Working on Attestra Android auth

Read [consumer agent entry](docs/agents/index.md) to integrate this library;
this file is for contributors. Apply the Android and system-integrated rules in
[agents.prompts](https://github.com/lambdawalker/agents.prompts).

- `:auth` owns Android APIs and implemented UI; `:app` owns the runnable host and
  `:identity-mock` is a debug-only, non-UI transport fixture. Keep mocks out of release.
- `design.attestra` owns system requirements; the Go backend owns HTTP contracts.
  Do not change intended architecture to hide an implementation defect.
- Read [IMPORT.md](IMPORT.md) before choosing dependency versions. Auth has no
  confirmed published artifact; do not invent Maven/JitPack coordinates or infer
  a release from the sample APK version. Dependency libraries have independent releases.
- Preserve API 28, compile SDK 37, Java 17 bytecode and the pinned Gradle/Kotlin/AGP
  toolchain. A new artifact publication needs separate authorization.
- Preserve local-proof + email-link confirmation, server-authoritative completion,
  capture disclosure/version consent, account/environment isolation and private uploads.
- Call controllers from their UI owner; retain and close owned clients/controllers.
  Never log passwords, tokens, image bytes or personal card fields.
- Every composable must be listed in `docs/screenshots/coverage.json`. Provide an
  offline preview or document its runtime delegation to a covered visual component.
  Never start a camera, Keystore, network request or navigation service in a preview.
- Screenshot candidates, reviewed acceptance and baseline validation are separate
  commands; ordinary validation must not overwrite accepted images.
- Canonical consumer guides are `docs/agents/`; Spanish prose lives in
  `docs/es/agents/` and records reviewed canonical hashes in `sites/translations.json`.
  Never refresh translation hashes without reviewing meaning and executable fences.
- Update API/feature coverage, relevant guides, screenshots and tests with behavior.
  Do not hand-edit generated `sites/dist/`, API inventory or candidate manifests.

## Checks

```sh
uv run --with rich==14.1.0 python -m unittest discover -s scripts/signing
python3 scripts/docs/inventory.py --check
python3 scripts/screenshots/suite.py coverage
python3 -m unittest discover -s scripts/screenshots -p 'test_*.py'
uv run --with Markdown==3.7 --with Pygments==2.18.0 python sites/test_build.py
uv run --with Markdown==3.7 --with Pygments==2.18.0 python sites/build.py
bash gradlew :auth:testDebugUnitTest :identity-mock:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

See [screenshot commands](docs/screenshots.md) and [site maintenance](sites/README.md).
Use a task branch and push a PR by default. Do not merge, publish artifacts, change
release tags, or deploy production without corresponding authorization.
