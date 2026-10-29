# Two-sided capture implementation plan

Approved approach: use detector 0.1.4 for front/back capture, review and retakes; preserve server consent, upload retries, checkpoints and explicit finalization.

- Update dependencies, isolating model assets from the old core.
- Add paired upload admission and retain both JPEGs for retry; test ordering and cleanup.
- Replace the live camera adapter and connect both live/demo hosts.
- Run unit tests, assemble debug APK, check conventions and review the diff before opening a PR.

Device verification: camera permission, cancel at each stage, auto capture toggle, torch and independent retakes. Activity recreation intentionally restarts unsubmitted capture.
