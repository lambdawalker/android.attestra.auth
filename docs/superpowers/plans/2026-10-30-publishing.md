# Auth publishing implementation plan

Goal: adapt CardDetector's journaled publishing to the existing single auth library.
Keep API 28, compile SDK 37, Java 17 bytecode, the JDK 25 daemon and dependency pins.

- [x] Adapt and test the environment API/wizard, including empty JitPack and timed delayed-docs environments. Require owner-provided license metadata; preserve existing protections and secrets.
- [x] Adapt destination registry, source identities, reservations, artifact checks and confirmation-based installation generation for auth only. Test version allocation, conflicts, partial outcomes and recovery offline.
- [x] Configure release-only Central and unsigned JitPack publications, with a local verification repository. Keep app and identity-mock unpublished.
- [x] Add manual publishing and recovery workflows, shared mutation lock, delayed Central confirmation, and credential-free CI. Use the project's pinned toolchain.
- [x] Update the release runbook and source-only installation documentation. Validate scripts, workflow structure, site and available build tasks; push a PR without releasing packages.

Review focus: existing environment protection retention; missing license/credentials;
unknown upload outcomes; same-source catch-up across destinations; generated docs
must not advertise pending releases. A live release is outside this task.

Validation: Python publishing/environment tests (26), screenshot tests (8), signing tests (18), site tests (10), site build, docs inventory/example/provenance and registry/import drift checks. Gradle publication and conventions checks blocked by the unavailable pinned distribution download. No packages or release tags were created.
