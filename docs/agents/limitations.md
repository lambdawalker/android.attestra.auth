# Limitations and security boundaries

Scope: Development/source. No confirmed stable auth artifact or historical manual
exists. The [installation reference](../../IMPORT.md) records the source-only path.

- Android API 28 is the minimum for library and demo. Actual Credential Manager
  behavior depends on provider/device configuration and server association.
- The library requires the matching Attestra HTTP service. It is not a generic
  OAuth/OpenID client or a standalone identity-verification service.
- Email confirmation, passkey registration, image validation, text extraction,
  manual review and identity verification are separate facts. Extraction can fail;
  manual fields can be incorrect. Saving fields is not approval.
- ID photos are sensitive. The live sample uses `FLAG_SECURE`; a custom host must
  apply equivalent window policy and backup exclusions. Preview captures contain
  synthetic fixture data and do not establish device privacy guarantees.
- Pending JPEG upload data is memory-only. Process death can require recapture.
  Checkpoints preserve identifiers and operation intent, not images or signed URLs.
- Client-side password strength is a heuristic, not a breach or safety guarantee.
  Supported password policy uses printable ASCII without whitespace. The server
  enforces requirements independently.
- Trusted public configuration routes requests but contains no secret authentication
  authority. A configuration hash is not a signature. Never change an API binding
  midway through a session without clearing the incompatible local authentication.
- `OnboardingBusinessActions` is an intentionally empty catalog seam. Older
  identity-validation screens are prototypes and are not returned by the live flow.
- Tests and host-side screenshots do not prove camera focus, system permission
  dialogs, App Links, lifecycle/process restoration, email delivery or deployed AWS
  permissions. Those remain separate device/backend smoke checks.

## Release/deployment audit

Only Android verification, signing and documentation screenshot workflows existed
before this task. This repository does not upload an auth package, finalize a registry
release or maintain a historical release catalog. Do not add a 15-minute registry
wait or invent a release tag merely to publish a documentation correction.

The new documentation build is separately retryable and cannot publish packages.
Hosting configuration remains an owner step until an actual deployment is verified.
