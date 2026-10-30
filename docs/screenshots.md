# Preview coverage and screenshots

The library and demo app both have executable Compose screenshot suites. Every
owned composable maps to an offline scenario or an explicit runtime delegation in
[coverage.json](screenshots/coverage.json). [scenarios.json](screenshots/scenarios.json)
is the required inventory; missing scenarios, missing variants, unknown images and
zero-image UI modules fail candidate generation. `:identity-mock` is transport-only.

## Separate operations

### 1. Render candidates

Use **Screenshot candidates** on a PR, or manually run **Render documentation
screenshots** with `mode=candidates`. Local equivalent:

```sh
./gradlew :auth:updateDebugScreenshotTest :app:updateDebugScreenshotTest
python3 scripts/screenshots/suite.py candidates
```

Open `docs/generated/screenshots/index.html`. The artifact includes PNGs and a
manifest recording scenario, source fixture, source commit, relevant input hashes,
viewport, theme, locale, font scale, output dimensions and renderer provenance.
Candidate files are not approved baselines and do not change the committed archive.

### 2. Inspect and accept deliberately

Inspect all affected images for blank states, clipping, wrong controls, unreadable
text, themes and mislabelled prototype outcomes. Then, and only then:

```sh
python3 scripts/screenshots/suite.py accept --directory docs/generated/screenshots --reviewed-by "Reviewer and review context"
```

This writes `docs/screenshots/baselines.zip` and `baselines.json`, and synchronizes
three curated guide images plus `documentation.json`. Commit them together. The ZIP
is durable accepted reference data, not an expiring CI artifact or a second authored
image set. The manifest ties each curated image to its scenario and accepted pixels.
A new commit with unchanged relevant inputs does not require recapture.

### 3. Validate without updating

```sh
python3 scripts/screenshots/suite.py coverage
python3 scripts/screenshots/suite.py restore
./gradlew :auth:validateDebugScreenshotTest :app:validateDebugScreenshotTest
python3 scripts/screenshots/suite.py verify-docs
```

Normal CI restores approved references and runs comparison only. It never calls an
update/accept task. Changed relevant inputs require new candidates and an intentional
review. Candidate generation runs separately and uploads its own clearly labelled
artifact; comparison reports/diffs are uploaded even on failure. On Windows use
`gradlew.bat` and `python`.

## Configuration and scope

The established AGP 9.4.1 / screenshot alpha16 setup is preserved with the standalone
plugin; native test-suite migration belongs with a future compatible AGP change.
Most fixtures use 393 × 852 dp, English, font scale 1.0, renderer platform fonts and
light/dark themes. A narrow 320 × 740 dp, font-scale 1.3 error fixture checks readability.
Pixel dimensions and dependency/input hashes are recorded. The host renderer controls
capture timing; permission autoplay is disabled. Runtime clocks/services are not
started. Synthetic sample pixels illustrate legacy front/back confirmation only.

Production-source previews remain IDE conveniences. Screenshot fixtures in the
separate source sets are discoverable wrappers for runtime-owned visual content.
The coverage manifest distinguishes direct, parent-helper, runtime-delegation and
nonvisual (`FLAG_SECURE`) cases; it does not claim to render live services.

Camera streams, Android system dialogs, focus, inference, navigation, privacy-window
behavior and lifecycle/process restoration require separate device/backend tests.
Legacy identity-decision mockups are marked `legacy-design`, not live verification.
The UI pictured is English even when the surrounding guide is Spanish.

Publication-only Gradle additions are delimited by `BEGIN MAVEN PUBLICATION ONLY` / `END MAVEN PUBLICATION ONLY` in the root/auth build files and `gradle.properties`. They are excluded from visual render-input hashes. Android configuration, dependencies, toolchain and UI sources remain fingerprinted; the original reviewed images and provenance are retained. Do not put visual/build-runtime changes inside these publishing blocks.
