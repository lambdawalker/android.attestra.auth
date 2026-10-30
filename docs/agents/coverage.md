# Feature and documentation coverage

Scope: Development/source. Every row names implemented ownership rather than
claiming a released artifact or deployed backend verification.

| Feature | Human and raw-agent guide | Compiled example / tests | Screenshot coverage |
| --- | --- | --- | --- |
| Email signup/link/code recovery | Quickstart, API, troubleshooting | MainActivity, EmailFlowControllerTest | Email screens and errors |
| Passkey enrollment / returning sign-in | API, concepts, demos | EmailOnboardingHost, passkey tests | Start/error/unsupported and return sign-in |
| Password policy and enrollment | Concepts, runtime configuration | OnboardingPolicy tests, host | Password field/screen |
| Discovery and session isolation | Concepts, environment discovery | EnvironmentIndex and app configuration tests | Sample loading/failure |
| Capture, retry, cancellation | Recipes, identity capture | LiveCaptureFlow, CaptureControllerTest | Library processing/review and sample menu |
| Extraction/manual review / saved details | API, troubleshooting | CaptureDestinationTest / controller tests | Complete/failed review and finish |
| Android Keystore / signing / App Links | Concepts, signing guide | Signing checks, device smoke requirements | Not proof-bearing visual scenarios |
| UI-only catalog and design states | Demos, limitations | OnboardingCatalog | Legacy design states labeled as such |
| Resource ownership and disposal | Concepts, API | Controller tests and host disposal | Parent rendering is not lifecycle verification |
| Preview coverage and provenance | Screenshot guide and coverage manifest | Inventory checks, both UI-module renderers | Explicit symbol/scenario mapping |
| Installation / versions / translations | IMPORT, site maintenance | Site tests and release catalog checks | No fabricated stable release |
