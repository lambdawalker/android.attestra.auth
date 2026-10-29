# Capture HTTP contract

The old `/identity/v1/submissions` draft is retired. Android implements `/onboarding/id/...` with direct S3 PUT, asynchronous finalization and status recovery.

The canonical implementation contract is [go.attestra.aws.auth/docs/id-capture.md](https://github.com/lambdawalker/go.attestra.aws.auth/blob/main/docs/id-capture.md). The debug `MockCaptureServer` implements that shape locally. See [Android capture](../identity-capture.md) for use and limitations.

Review saves use `POST /onboarding/id/captures/{id}/review` with `expected_revision`, edited `data` and `origin_country`. Only the returned or reconciled matching `reviewed` object confirms persistence. See [save and finish](../identity-capture.md#save-and-finish).
