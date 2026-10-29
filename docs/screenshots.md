# Screen screenshots for documentation

The **Render documentation screenshots** GitHub Actions workflow renders the auth
module's `@PreviewTest` fixtures on Linux. Open a completed run, download
`onboarding-screenshots`, and open `docs/generated/screenshots/index.html`.
The gallery includes PNGs and `manifest.json`; copy the selected PNGs into the
relevant documentation assets directory when editing a guide.

The fixtures cover email, passkeys, sign-in, password, ID processing, extracted and
manual card review, saved details, and permission states. The main documentation
fixtures render fixed 393 × 852 dp light/dark English previews with synthetic data.
They never need credentials, a backend, a camera or a real ID.

## Local commands

```sh
./gradlew :auth:updateDebugScreenshotTest
python3 scripts/screenshots/export.py
./gradlew :auth:validateDebugScreenshotTest
```

On Windows use `gradlew.bat` and `python` instead. Use the repository's JDK/SDK
configuration, as for the normal Android build. No emulator is required.

The project pins AGP 9.4.1, so it uses the standalone screenshot plugin alpha16.
The newer `screenshotTests` native suite DSL is documented for AGP 9.5+; migrate
that configuration together with a future AGP upgrade.

## Rendering versus visual regression

CI generates references and validates against those newly rendered files. This is
a **rendering smoke test and documentation export**, not an approved pixel
regression baseline. References and galleries are reproducible CI artifacts and
are not committed. To add pixel regression checks, commit visually reviewed
reference images and run validation without regenerating them first.

Previews in production source files are IDE conveniences. Only fixtures under
`auth/src/screenshotTest` marked `@PreviewTest` belong to this rendering suite.
Live camera feeds, Android system permission prompts and real backend behavior
must still be tested on a device. Older identity-verification result mockups in
the catalog are designs, not evidence that verification is implemented.
