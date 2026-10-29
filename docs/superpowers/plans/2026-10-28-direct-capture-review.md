# Direct capture and review implementation plan

Approved design: Continue opens the camera for new captures. Confirming both photos uploads and finalizes automatically. Existing captures route to processing or the review form. Failed extraction opens an empty, savable form.

1. Backend: allow review for ready captures whose extraction is complete or failed. Preserve ownership, expiry, data validation, revision checks and idempotency; test failure/manual-save and prohibited states.
2. Android controller: resume/start into camera, automatically finalize successful paired uploads/retries, resume selected uploads through server validation, create empty drafts on failed extraction, preserve edits and saved reviews.
3. Android UI: enable immediate entry in live flow, show dedicated loading and review screens, retain recovery actions for transport/policy/image errors. Poll only while processing and foregrounded; don't equate network errors with extraction failures.
4. Add controller, screen-routing and mock HTTP regression coverage, run available tests and CI, document rollout (backend first), review both branches and open PRs.

Constraints: photo confirmation/retakes remain; server policy changes still require explicit acknowledgement; file-validation failures still require recovery, not manual identity submission. No deployments. Existing protected memory and upload ownership apply.
