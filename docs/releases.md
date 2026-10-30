# Publish Attestra auth

This tooling adapts the destination registry, Python terminal wizard and journaled
release protocol from `android.apexfission.carddetector` to **`:auth` only**.
The demo `:app` and debug `:identity-mock` are never Maven publications.
No auth package has been published or confirmed by this change.

## Before the first release

1. Choose the actual project license. Commit its text and set `POM_LICENSE_NAME`
   and `POM_LICENSE_URL` in `gradle.properties`. They are deliberately blank:
   the reference's Apache license does not grant a license to this project.
   JitPack must read this public metadata from tagged source, not GitHub secrets.
2. Review the proposed Central identity `io.github.lambdawalker:attestra-auth`.
   Verify ownership of `io.github.lambdawalker` in the Central Portal. Edit the
   authoritative `GROUP`/`POM_ARTIFACT_ID` before the first tag if necessary;
   adjust the allowed artifact in `scripts/module_release.py` when renaming.
3. Create a Central Portal publishing user token (username/password pair).
   Generate a GPG signing key, distribute its public key as Central requires,
   and export the private key to an ASCII-armored file outside this repository.
4. Run the setup wizard below. It creates `maven-central`, `jitpack`, and
   `delayed-docs`; the latter gets a 15-minute wait timer when newly created.
5. Check repository Actions permissions and tag/main rules. The automatic
   `GITHUB_TOKEN` needs contents write for immutable tags, journals and confirmed
   installation commits. Protected branches/rulesets may reject bot writes;
   do not use a bypass token. Preserve the journals and reconcile through the
   repository's approved review process if an atomic finalization push is blocked.

## Configure environments

Use Python 3.12+ from the repository root. On Windows `python`/`py` can be used.

```sh
python -m pip install -r scripts/requirements-setup.txt
python scripts/setup_github_environments.py
```

With uv, dependencies can remain isolated:

```sh
uv run --with-requirements scripts/requirements-setup.txt python scripts/setup_github_environments.py
```

The default repository is `lambdawalker/android.attestra.auth`; `--repo owner/name`
is available for forks. The wizard **asks for a GitHub token** in a masked field.
Use a fine-grained token restricted to this repository with Administration and
Environments read/write, and authorize any required organization SSO. A classic
`repo` token also works. This setup token stays in memory and is never saved or
installed as a publishing credential. Do not paste it into a PR or commit it.

| Environment | Variables | Secrets |
| --- | --- | --- |
| `maven-central` | `MAVEN_REPOSITORY_URL`, default `https://repo.maven.apache.org/maven2` (public verification endpoint) | `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_IN_MEMORY_KEY`, optional `SIGNING_IN_MEMORY_KEY_PASSWORD` |
| `jitpack` | None | None; public JitPack requires no publishing token |
| `delayed-docs` | None; new environment has a 15-minute wait timer | None |

The signing key prompt takes a **file path**, reads the full multiline key, and
encrypts the secret with GitHub's public key before upload. Blank preserves an
existing value. Required missing values cannot be skipped. The review page shows
setting names and nonsecret changes before any writes. Partial or uncertain writes
are reported by name so rerunning can inspect the actual state safely.
Existing environments and protection rules are preserved. In particular, an existing
`delayed-docs` timer is not changed: inspect it in Settings > Environments and set
15 minutes if needed. Timer availability depends on your GitHub plan and repository
visibility. Required reviewers or branch restrictions may introduce additional gates;
the wizard does not remove them. No live repository environments were configured or
inspected as part of adding these scripts.

The public registry is `publishing/repositories.yml`. After changing it run:

```sh
python scripts/publishing_config.py generate
python scripts/publishing_config.py check
```

## Publish

After review and merge, open **Actions → Publish Attestra auth → Run workflow**
on `main`. Select `maven-central` or `jitpack`. There is one optional `version`
field; the module is fixed internally.

- First blank version selects `0.1.0`.
- Blank with unchanged release inputs reuses the latest semantic version and
  its original source commit. If this destination already confirms it, it is a no-op.
- Blank with changed release inputs increments the latest global patch, e.g.
  `0.1.9` becomes `0.1.10`, even when the selected destination missed releases.
- An explicit stable `X.Y.Z` must be newer than every reserved/confirmed identity.
  It is a new version request, never a recovery shortcut. Prereleases and snapshots
  are not stable releases under this policy.

Inputs include auth main source/resources, packaged README/agent guides, buildSrc,
build scripts, dependency catalog, wrapper/toolchain, license, release helpers and
registry. Demo/mock-only sources, generated IMPORT and confirmation records are
excluded. Protocol/script changes conservatively allocate a new identity.

The workflow validates an unsigned publication before reserving `auth/vX.Y.Z` and
an annotated `release-pending/...` journal atomically. Tags are immutable. A Central
upload-started marker is pushed before the plugin upload; an unresolved attempt
blocks another upload. Maven Central credentials are exposed only to preflight and
the signed upload step. Public JitPack never receives Central secrets.

Central uses `:auth:publishAndReleaseToMavenCentral`, then a separate secret-free
environment delay, then public verification for up to 40 minutes. The environment
wait holds no runner or mutation lock; the publishing plugin may itself wait for
Central validation while its runner is active. Publishing and automatic/manual
finalization share one repository-wide job concurrency group. GitHub concurrency
is not a durable FIFO queue; pending runs may be superseded, so journals remain
necessary.

JitPack installs exactly one unsigned release publication in MavenLocal from a
canonical `auth/vX.Y.Z` tag. Its consumer version is **`auth~vX.Y.Z`**, at
`com.github.lambdawalker:android.attestra.auth`; this is distinct from Central's
semantic version string. An artifact request starts the remote build, which is
polled for up to 30 minutes. Provider-reported success must identify the reserved
full commit SHA and all required public artifacts must validate. Provider provenance
is trusted within these limits; tags alone do not prove public availability.

Both destinations require POM, AAR, Kotlin sources and text documentation. Central
also requires Gradle metadata and detached signatures. Public Central artifacts
must match the pre-upload SHA-256 journal; signature armor is checked, **not**
cryptographically verified. JitPack may omit Gradle metadata; its independently
rebuilt ZIP hashes are recorded after verification instead of compared to local
ZIP hashes. Checks validate coordinates, pinned permissions/detector dependencies,
Java 17 auth classes and non-pointer bundled model assets. Public model files are
also compared with source-specific asset hashes stored in the reservation.

## Recovery

Use **Actions → Finalize Attestra auth release** with the same destination and
existing semantic version. It never rebuilds or uploads to Central. It reads the
durable reservation, checks source identity, polls public artifacts and atomically
updates confirmed records, immutable history and generated `IMPORT.md` on main.
It can run during the environment delay; later automatic finalization is a no-op.
Repeated completed/older finalization does not overwrite newer installation facts.
An external JitPack request can start the same tagged build if not cached; recovery
never invokes force-rebuild/delete APIs or moves the canonical tag.

After validation failure before reservation, fix the cause and rerun normally.
After an upload timeout, inspect the retained Actions log and Central Portal;
the request may have succeeded. **Do not delete attempt markers or blindly upload
again.** Partial public artifacts and propagation timeouts retain their journals
and do not change installation recommendations. Rerun the finalizer after checking
the outcome. A definitively rejected Central attempt needs owner reconciliation:
verify the entire artifact set is absent and the Portal deployment cannot publish,
archive the journal, then clear only its pending/upload-started refs through the
approved maintenance process. Never delete or move the canonical source tag.

For a terminal JitPack error at the exact reserved commit, the next Publish run
can archive its pending journal only after confirming every intended artifact is
absent and selecting a newer version/corrected descendant source. Unknown, queued,
source-mismatched or partially public builds keep their journal.

## Installation and documentation

`IMPORT.md` is generated from `docs/templates/IMPORT.md.template` and confirmed
records. It shows only destinations confirming the newest semantic version at the
same source SHA, with Kotlin/Groovy/catalog/Maven examples and JitPack dependency
repository declarations. No proposed coordinate is advertised as available.

```sh
python scripts/module_release.py generate
python scripts/module_release.py verify
```

Equivalent Gradle tasks are `generateImportDocs` and `verifyImportDocs`.
Confirmation archives are `docs/releases/history/auth/X.Y.Z.json`; these preserve
source/documentation identities and destination artifact hashes independently of
latest pointers. The existing guide website remains explicitly Development/main;
these archives are publication evidence, not full historical guide snapshots.
A successful publishing/finalization workflow refreshes the current site from the
latest main, including confirmed IMPORT. It does not depend on a bot push event.

## Verification without publishing

```sh
python -m pip install -r scripts/requirements-setup.txt
python -m unittest discover -s scripts/tests -v
python scripts/publishing_config.py check
python scripts/module_release.py verify
bash gradlew :auth:testDebugUnitTest :auth:assembleRelease :auth:publishAllPublicationsToVerificationRepository -PreleaseModule=auth -PreleaseVersion=0.1.0 '-PPOM_LICENSE_NAME=CI validation fixture only' -PPOM_LICENSE_URL=https://example.invalid/license
```

The CI-only license parameters above do not declare a project license. For the
unsigned JitPack layout add `-PjitpackBuild=true` and set
`RELEASE_REPOSITORY=jitpack` when running `check-local`. Neither verification
repository task uploads packages or reserves tags. The CI matrix verifies both
layouts without credentials and runs Python tooling tests on Linux and Windows.

References: [plugin Central setup](https://github.com/vanniktech/gradle-maven-publish-plugin/blob/0.37.0/docs/central.md),
[JitPack build configuration](https://docs.jitpack.io/building/),
[JitPack API](https://docs.jitpack.io/api/),
[GitHub environments](https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment).
