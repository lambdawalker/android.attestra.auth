# Proposed identity HTTP contract v1

**Status: Android mock contract, not implemented in the Go backend.** The backend remains authoritative for subject binding, evidence versions, validation, extraction, and final policy decisions. This is a handoff proposal, not a claim of a deployed API. It extends the central [ID-capture architecture](https://github.com/lambdawalker/design.attestra/blob/main/auth/onboarding/id-capture/architecture.md).

All routes are beneath an HTTPS origin, with `Authorization: Bearer <access_token>`. The server derives Cognito `sub` from the validated token; no client-supplied subject is accepted. Every lookup/mutation must enforce ownership. Unauthorized/missing/expired credentials return 401; disallowed access returns 403. Unknown IDs return 404. Error bodies are not shown or logged by Android.

The client allocates a canonical UUID `submission_id` before the first request and persists that ID. A new two-sided capture gets a new UUID and a server-assigned monotonically increasing `evidence_version` for the account. The version stays fixed for that UUID. All mutating calls are retry-safe and reject conflicting bodies/versions with 409. Authentication/rate limits and actual binary validation belong to the server.

| Method and route | Request | Response |
| --- | --- | --- |
| `PUT /identity/v1/submissions/{id}` | `{}` | 201 for a new draft, 200 for an existing owned draft/result; `IdentityRecord` |
| `PUT /identity/v1/submissions/{id}/assets/front?evidence_version=N` | JPEG, `Content-Type: image/jpeg`, ≤4 MiB | 204 |
| `PUT /identity/v1/submissions/{id}/assets/back?evidence_version=N` | Same bounds | 204 |
| `POST /identity/v1/submissions/{id}/extraction` | `{"evidence_version":N}` | 200, `{"readable":true,"extracted":{...}}` or `readable:false` |
| `POST /identity/v1/submissions/{id}/submit` | Version, original extraction, corrections; `Idempotency-Key: {id}` | 202 accepted/pending or 200 for identical already-accepted submission; `IdentityRecord` |
| `GET /identity/v1/submissions/{id}` | No body | 200 `IdentityRecord`; 404 absent |

The current prototype proxies bounded binary uploads through the identity API to keep transport replaceable. The central AWS design proposes scoped S3 uploads; implement that behind the gateway before production if needed. Enforce actual file type, decoder safety, checksum, size, and asset count on the backend; never trust filename/MIME or detector output. Accepted evidence is immutable. Draft uploads should be keyed uniquely by subject/submission/version/side. The mock checks only JPEG signature/size and presence; it cannot prove readability.

Extraction currently has a bounded synchronous 200 response and a 15-second client request timeout. It is synthetic in the mock: `SAMPLE PERSON`, `TEST` jurisdiction, and `SAMPLE-0001`. For long-running production OCR, extend this contract with an extraction-job status rather than increasing the loader indefinitely. No provider deployment should assume that this mock's fixtures are real OCR.

Example record:

```json
{
  "submission_id": "e347bb6a-fdbd-4ed0-a4b7-8ee7f0e27914",
  "evidence_version": 1,
  "accepted": true,
  "decision": "pending",
  "autoReport": "approved",
  "thirdParty": "pending",
  "can_retry": false
}
```

Each status supports `not_started`, `pending`, `approved`, `rejected`, `inconclusive`, or `error`. Unknown enum values fail closed as status retrieval errors. An unaccepted draft must have `decision:not_started`. `decision` is the explicit server policy outcome; Android does not compute it by combining tracks. `can_retry` determines whether the UI can offer a new evidence capture after an accepted submission. A readable extraction and HTTP acceptance cannot unlock restricted features.

Submit shape (all identity strings are UTF-8):

```json
{
  "evidence_version": 1,
  "extracted": {
    "fullName": "SAMPLE PERSON",
    "dateOfBirth": "1990-01-02",
    "address": "123 Example Street, Sample City",
    "documentNumber": "SAMPLE-0001",
    "issuingCountry": "TEST",
    "documentType": "Sample two-sided ID",
    "expirationDate": "2030-01-01"
  },
  "corrected": {
    "fullName": "Corrected sample name",
    "dateOfBirth": "1990-01-02",
    "address": "123 Example Street, Sample City",
    "documentNumber": "SAMPLE-0001",
    "issuingCountry": "TEST",
    "documentType": "Sample two-sided ID",
    "expirationDate": "2030-01-01"
  }
}
```

The server must compare `extracted` against its stored extraction or reference it by an immutable extraction ID; never treat client-supplied extraction as authoritative. Corrections remain separately attributable. Current UI validation only requires nonblank name, date of birth, and address. Accepted document types, date parsing, jurisdiction policy, and legal consent remain backend/product decisions.

On ambiguous submit failure, Android calls GET first. An accepted record ends reconciliation without resubmitting. An explicit unaccepted record permits replay of the exact stored submission body. A failed GET does not permit replay. After process death, only the ID/version survives; an accepted record resumes, while an unaccepted draft requires a new capture because images/corrections were discarded.

Backend implementation must authenticate/deduplicate provider callbacks, bind each result to its immutable evidence version, reconcile retries transactionally, prevent stale decisions replacing newer evidence, apply retention/deletion rules, and enforce restricted-feature authorization independently of the client. Those workflows are outside this Android mock implementation.
