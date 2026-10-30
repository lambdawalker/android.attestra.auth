# Troubleshooting

Scope: Development/source. Keep [backend operations](https://raw.githubusercontent.com/lambdawalker/go.attestra.aws.auth/main/docs/agents/troubleshooting.md)
and Android/device evidence separate.

| Symptom | Diagnose | Correct response |
| --- | --- | --- |
| Gradle sync fails at generated assets | Compare current `auth/build.gradle.kts`; assets use AGP generated-source registration | Use the committed task/API; do not cast legacy Android source sets or assume a corrupt cache |
| Model/detector conflict | Compare isolated `cardModelAssets` with dependency catalog | Keep model extraction nontransitive; do not add two versions of the shared JitPack identity to runtime |
| Verification link opens browser | Check manifest host, package/certificate, public `assetlinks.json` and installed signing identity | Follow [signing](../android-signing.md); index discovery does not establish App Links |
| Link on another device needs a code | The local A proof is missing | Use B + C; never confirm with B alone or copy proof secrets between logs |
| Settings unavailable | Check index binding, `/v1/app-config`, cache expiry and validation | Retry trusted settings. Do not infer a default successful policy or silently change host |
| Stuck reading, state `finalizing` | Worker file validation and durable ready transition have not completed | Correlate backend `capture.work_*` / `capture.store_swap_failed`; check S3 and DynamoDB permissions, not only the model |
| `ready` with extraction `pending` | Extraction queued/running | Correlate `capture.inference_*` and attempts; retain recovery. HTTP status/parser stage helps distinguish provider rejection from malformed output |
| Extraction `failed` | Service could not extract text | Show empty manual entry, not untrusted partial provider output |
| Save failed or stale revision | Compare returned `reviewed` values and draft revision | Preserve local edits; refresh/reconcile. Do not navigate to finish without matching server acknowledgement |
| Recapture needed after restart | Upload bytes were memory-only | Retake unfinished photos; IDs/checkpoints cannot recreate image bytes |
| Passkey sheet cancelled/unsupported | Catch `PasskeyCancelled` / `PasskeyUnsupported` | Offer supported retry/fallback; do not mark registration successful |
| Blank or missing screenshot | Check required scenario inventory, fixture state and renderer report | Render candidates and inspect; never silently replace reviewed baselines to make validation pass |

## Log safety

Android metadata tag: `AttestraCapture`. Backend diagnostic events include
`capture.dispatch`, `capture.work_started`, `capture.work_state`,
`capture.work_finished`, `capture.extraction_result`, `capture.inference_response`,
`capture.inference_failed`, and `capture.store_swap_failed` when that backend
revision is deployed. Normal Lambda END/REPORT entries alone are not application
success evidence; the worker can report SQS batch-item failures.

Do not log token/password values, email-link secrets, JPEG bytes, signed URLs,
card text, provider response bodies or raw exceptions containing those values.
No current Android log demonstrates the deployed backend revision by itself.
