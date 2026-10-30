# Migration and compatibility

Scope: Development/source. No released auth version boundary is claimed.
Use immutable source commits or reviewed PR history for source migration evidence;
there is no historical published API catalog to select yet.

| Older pattern | Current implementation | Action |
| --- | --- | --- |
| Capture-only confirmation / separate side uploads in host UI | Detector two-sided capture, automatic upload/finalization and extraction review | Use the current `LiveCaptureFlow`; do not generate new code around obsolete `/identity/v1/submissions` drafts |
| Treating `id_captured` as saved form or identity approval | Separate evidence, extraction, reviewed fields and account status | Finish only on the matching server `reviewed` object; do not claim identity validation |
| Cast legacy Android library source sets for model assets | `androidComponents.onVariants` generated assets | Use the checked-in `PrepareCardModelAssets` registration |
| Static API URL only | Trusted index discovery or explicit static origin, followed by runtime settings | Preserve account/backend isolation; static mode is an explicit alternative, not a discovery-failure fallback |
| Old screenshot smoke workflow regenerated references during validation | Candidate generation and reviewed baseline acceptance are separate | Follow [screenshot commands](../screenshots.md); do not treat candidates as approved regression evidence |

Dependency libraries have independent publication histories. Auth source integration
pins compatible detector/model/permission choices in its own catalog; the sample's
APK version does not version those libraries or establish an auth release.

For later auth releases, retain confirmed code/documentation identities and exact
installation facts before changing the latest pointer. A missing translation must
fall back to the canonical guide for the selected scope, never a newer API version.
