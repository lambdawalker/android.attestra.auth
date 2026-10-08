# Android identity capture implementation plan

> Execute inline using superpowers:executing-plans. The user requested implementation against mock endpoints; no deployment is required.

**Goal:** Make the existing optional identity flow usable with real front/back card capture and deterministic mock HTTP endpoints.

**Architecture:** Keep orchestration and an injectable HTTP gateway in `:auth`. Put API-28 camera dependencies in `:identity-capture`. Supply a persistent, local Ktor MockEngine service through debug-only app wiring; release builds cannot mistake mock decisions for identity verification.

**Tech stack:** Kotlin, Compose, Ktor, Apexfission permissions 0.2.3, bundled detector model 0.1.2.

**Spec:** https://github.com/lambdawalker/design.attestra/tree/main/auth/onboarding/id-capture and ../../../verification/identity/architecture.md in that repository.

## Constraints and decisions

- Implement the approved surrounding flow, preserving extracted values separately from corrections.
- Propose an authenticated versioned HTTP contract; the current Go deployment has no identity routes.
- Only a policy decision is approval. Preserve independent autoReport and thirdParty states.
- Mock OCR uses conspicuously synthetic fields; no real OCR/provider assertion, no liveness claim.
- No raw evidence in preferences, bundles, backups, logs, or mock-server persistence. Captures are memory-only and are discarded on exit/recreation; server IDs survive restart.
- Keep auth minSdk 24; scanner and demo app require minSdk 28. Compile SDK 37 and Java 17 for the published capture dependencies.
- Use confirmed published coordinates, not unpublished main-branch APIs.

## Review focus

Duplicate submission after timeout; account switching; process death after acceptance; callbacks after camera disposal; stale retained front image reused for back. Tests cover API/controller cases; camera behavior requires device validation.

### Task 1: Contract and orchestration

- [ ] Add gateway/controller tests for review, acceptance without approval, reconcile-before-retry, separate corrections, resume, cancellation, version matching, and forbidden retry.
- [ ] Run `bash gradlew :auth:testDebugUnitTest` (baseline blocked: Gradle download network unavailable).
- [ ] Implement `identity/IdentityModels.kt`, `IdentityApi.kt`, `IdentityController.kt`, checkpoint interface, and secure checkpoint adapter.

### Task 2: Mock HTTP service

- [ ] Add `:identity-mock` with deterministic scenarios and contract tests using the real HTTP client.
- [ ] Implement account-scoped persistent mock state, idempotency and upload/submit/status routes. Never persist image bytes or personal fields.

### Task 3: Capture and UI wiring

- [ ] Add `:identity-capture` using published model and permission APIs; recycle every bitmap, own a scanner-session ViewModelStore, isolate each side.
- [ ] Connect existing review/error/success screens, add bounded pending/status and capture-error states, and preserve edits during retries.
- [ ] Add debug-only offline entry with scenario selector; connect authenticated onboarding to the same mock flow, release to unavailable state.

### Task 4: Verification and handoff

- [ ] Document HTTP contracts, mock scenarios, integration and device test steps.
- [ ] Run unit/build checks, inspect diff, request whole-branch code review, fix findings.
- [ ] Push feature branch and open a draft PR with exact validation limits. Do not merge or deploy.

## Execution record

- Implemented all four modules/wiring/contracts in the feature branch. Native execution used a new isolated checkout and feature branch; the existing central spec remains system authority.
- Ruling: use a bounded synchronous mock extraction and API-proxied uploads for the draft contract; production S3 upload/OCR/provider policy remains a documented backend handoff.
- Ruling: keep auth API 24 and isolate detector requirements in API-28 capture/demo modules.
- Test-first sources were written and execution attempted; Gradle bootstrap is blocked by restricted network, so no red/green claim is made.
- Whole-branch independent review found missing sample reset and lost scenario selection. Both were corrected with regression test sources; test execution has the same environmental blocker.
- Additional lifecycle review drops personal state on close and ignores late extraction responses.
- `git diff --check` passes. Android build/tests and physical camera verification remain pending; draft PR reports that explicitly.
