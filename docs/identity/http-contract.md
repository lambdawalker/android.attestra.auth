# Capture HTTP contract

The old `/identity/v1/submissions` draft is retired. Android implements `/onboarding/id/...` with direct S3 PUT, asynchronous finalization and status recovery.

The canonical implementation contract is [go.attestra.aws.auth/docs/id-capture.md](https://github.com/lambdawalker/go.attestra.aws.auth/blob/codex/id-capture-s3/docs/id-capture.md). The debug `MockCaptureServer` implements that shape locally. See [Android capture](../identity-capture.md) for use and limitations.
