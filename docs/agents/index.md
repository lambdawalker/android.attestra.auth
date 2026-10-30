# Attestra Android auth

**Scope: Development (`main`), source integration; no confirmed auth package release.**
Use `:auth` to host interactive email confirmation, passkey/password sign-in,
policy-controlled onboarding and two-sided card capture against the matching
Attestra backend. Use `:app` as the complete runnable host. This is not a backend,
a generic OAuth SDK, background authentication service, or identity-verification engine.

Read [installation](../../IMPORT.md), then [quickstart](quickstart.md).


## Implemented UI walkthrough

These English-language screens are component previews with synthetic data, not
captures of real account success. Start with email, wait for document extraction,
and finish only after the server acknowledges saved details.

| Start | Processing | Saved |
| --- | --- | --- |
| <img src="../assets/screenshots/email-start-light.png" alt="Email setup; synthetic light-theme preview" width="210"> | <img src="../assets/screenshots/read-document-dark.png" alt="Document reading; synthetic dark-theme preview" width="210"> | <img src="../assets/screenshots/details-saved-light.png" alt="Saved details; synthetic light-theme preview" width="210"> |

See [demos and recovery scenarios](demos.md) for actions and expected outcomes.

On narrow screens, scroll the image walkthrough horizontally.

| Task | Read | Preferred API / source |
| --- | --- | --- |
| Embed interactive onboarding | [Quickstart](quickstart.md) | `EmailOnboardingHost`, sample `MainActivity` |
| Resolve an environment safely | [Concepts](concepts.md) | `EnvironmentIndex`, `AppConfiguration` |
| Consume individual auth operations | [API](api.md) | Email and return-auth clients; host owns clients |
| Capture and review both ID sides | [Recipes](recipes.md) | `CaptureHost`, `CaptureController`, `IdentityCamera` |
| Recover from processing or save errors | [Troubleshooting](troubleshooting.md) | Capture status and confirmed `reviewed` record |
| Reuse screen UI without services | [Demos](demos.md) | UI catalog and screenshot fixtures |
| Find exact declarations | [API inventory](api-inventory.md) | Generated source signatures and source links |
| Change this repository | [Contributor rules](../../AGENTS.md) | [Maintenance](../../sites/README.md) |

## Critical invariants

- Resolve and validate the backend binding before creating authenticated clients.
  Never send an existing account's credentials to a newly selected host.
- Confirm email with local proof A + email token B, or code C + B. Never confirm
  with B alone or treat a UI transition as authentication.
- Only server-confirmed status may complete onboarding. Captured images, extracted
  text and saved review data do not assert identity validity.
- Retain controllers per flow, call them from the UI owner, and close owned clients.
  `CaptureHost` closes its controller on disposal; do not reuse a closed controller.
- Accepted JPEG arrays transfer to the capture controller; do not reuse or log them.
  Interrupted uploads may require retaking photos after process death.
- Keep token/password/image/personal-data values out of logs and screenshots.

[Concepts](concepts.md) · [API](api.md) · [Recipes](recipes.md) ·
[Limitations](limitations.md) · [Troubleshooting](troubleshooting.md) ·
[Migration](migration.md) · [Coverage](coverage.md) · [Agent evaluations](evaluation.md)

## System ownership

The [central agent entry](https://raw.githubusercontent.com/lambdawalker/design.attestra/main/docs/agents/index.md)
owns architecture and shared requirements. The
[backend agent entry](https://raw.githubusercontent.com/lambdawalker/go.attestra.aws.auth/main/docs/agents/index.md)
owns service operations; its
[capture contract](https://raw.githubusercontent.com/lambdawalker/go.attestra.aws.auth/main/docs/id-capture.md)
is authoritative for HTTP serialization. Follow links as raw Markdown; prototype
timers are not evidence of successful backend behavior.
