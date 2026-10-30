# Documentation maintenance

The Android library reuses the parent `design.attestra` site's small Python-Markdown
renderer and visual conventions. No new library runtime or JavaScript package
manager dependency is introduced. Canonical English consumer contracts remain in
`docs/agents/`; HTML and raw Markdown are generated from the same authored files.

## Build and preview

```sh
uv run --with Markdown==3.7 --with Pygments==2.18.0 python sites/test_build.py
uv run --with Markdown==3.7 --with Pygments==2.18.0 python sites/build.py
python3 -m http.server 8000 --directory sites/preview
```

The builder clears `sites/dist/`, validates links/anchors/assets and creates a
`sites/preview/android.attestra.auth` mount for the real project subpath. Open
`http://localhost:8000/android.attestra.auth/`. Windows can use the same Python
commands; install the pinned `sites/requirements.txt` in a virtual environment if
`uv` is unavailable. Never commit generated `sites/dist` or `sites/preview`.

## Hosting

The proposed URL is `https://lambdawalker.github.io/android.attestra.auth/`.
It is **not claimed live**. The Documentation workflow builds read-only on PRs and
main pushes and uploads an artifact. To enable deployment, the owner selects
GitHub Actions in Settings → Pages, sets `DOCS_PAGES_ENABLED=true`, and manually
runs Documentation on main. Only the trusted deployment job has Pages/id-token
permissions. It checks that its SHA is still main before publishing and serializes
deployments. A retry cannot upload a package or redeploy AWS.

## Versions, translations and raw navigation

`sites/versions.json` advertises only Development because auth has no confirmed
published release. No module selector or fictional release history is introduced.
The renderer rejects a nonempty release list until immutable snapshot rendering
is implemented and confirmed installation evidence is supplied. Keep exact source,
documentation, locale and publication identities distinct when adding that support.

Spanish consumer prose is in `docs/es/agents/`, with canonical hashes and review
provenance in `sites/translations.json`. Review meaning and code fences before
refreshing a marker. Missing/stale translations visibly fall back to English for
the same Development build. Detailed legacy operational guides can use that
explicit fallback; do not describe them as fully translated.

Raw entry: `/agents/index.md`; language-scoped guides:
`/raw/en/main/docs/agents/` and `/raw/es/main/docs/agents/`. `llms.txt` and
`versions.json` provide discovery without JavaScript. Raw links stay Markdown;
source links deliberately leave for the audited source ref. The language switch
retains the selected guide, navigation order and Development scope.

## Maintenance checks

```sh
python3 scripts/docs/inventory.py
python3 scripts/docs/inventory.py --check
python3 scripts/screenshots/suite.py coverage
python3 scripts/screenshots/suite.py verify-docs
uv run --with Markdown==3.7 --with Pygments==2.18.0 python sites/test_build.py
uv run --with Markdown==3.7 --with Pygments==2.18.0 python sites/build.py
```

Generated declaration inventory is a retrieval aid, not a replacement for semantic
API contracts. Update [coverage](../docs/agents/coverage.md) and the appropriate
consumer guide when changing APIs. The [screenshot runbook](../docs/screenshots.md)
separates candidates, reviewed acceptance, regression validation and curated media.
An image/input hash mismatch fails documentation validation. New code commits do
not by themselves invalidate unchanged relevant render inputs.

Source behavior owns APIs; `IMPORT.md` owns source installation/release status;
`design.attestra` owns system requirements; the Go backend owns HTTP contracts.
This task audits release tooling only: no auth publishing workflow is invented.
