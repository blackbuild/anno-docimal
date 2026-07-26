# AnnoDocimal release runbook

Issue [#45](https://github.com/blackbuild/anno-docimal/issues/45) owns this procedure. It is a release-control document, not release authorization. A maintainer must separately authorize an exact RC or final release before any protected environment, credential, tag, registry, GitHub Release, or Pages writer is used. The issue remains open until its first authorized release has produced the required evidence.

This runbook releases one complete, immutable product. It preserves the dedicated Pages-writer App, the separate Pages environments, and the protected-authorization boundary from #71. It does not implement #53, execute #44, or authorize any remote mutation.

## Release identity and freeze

Select a projected final version, for example `1.0.0`, from the approved product and compatibility change set. An RC is the exact immutable version `<final>-rc.<N>`, for example `1.0.0-rc.1`; the final is exactly `<final>`. The immutable Git tag is always `v<version>`, made only for the same exact source commit that produced the public product.

Before an actual release, update a clean local checkout of `master`, then record the exact inputs:

```shell
git fetch origin master --tags
git switch master
git pull --ff-only origin master
test -z "$(git status --porcelain)"
git rev-parse HEAD
git ls-remote --exit-code origin refs/heads/master
```

The two 40-character revisions must match. Capture that SHA as `<full-source-sha>`; do not use an abbreviated SHA or a moving branch name for a release input. A protected workflow must likewise receive the explicit SHA, verify that it is on current `master`, and build that object rather than re-resolving a branch tip.

The active changelog section is projected before the RC as `## <version> (unreleased)`. Keep it stable during RC validation. Each RC gets its own immutable tag, GitHub prerelease, and release-evidence record; it does not fragment `CHANGES.md`. Only after accepting the RC may the documentation-only exception change that heading to `## <version> — YYYY-MM-DD` and reconcile the GitHub Release text. A substantive source, dependency, build, signing, publication, or workflow change requires a new RC. Immediately after the final release, create the next projected `(unreleased)` section before further user-visible work.

Issue #53 is deliberately late: apply it only after the RC inputs freeze and immediately before the first actual RC build. It is not part of this rehearsal. If it changes the frozen inputs, restart the freeze and use the resulting exact master SHA; never quietly substitute a different source object.

## Preconditions and safe stop points

Complete these checks while no protected environment has been selected. This is the first safe stop point: failures here have no external release effect and may be corrected and re-run locally.

1. Required CI for the candidate SHA is clean, including Java 17, Groovy 3/4/5, Gradle compatibility, capture/projection contracts, and the clean publication-and-consumer smoke check. Review required CI findings; a green aggregate alone is not release approval.
2. `CHANGES.md`, migration guides, README/user documentation, supported-API/compatibility material, and generated Javadocs are release-ready. The GitHub Release body is copied or derived from the exact final `CHANGES.md` section; it is never edited as an independent source of truth.
3. Run the local product audit and signing configuration inspection. It checks the complete unsigned product, the `com.blackbuild` Central namespace binding used by Nexus staging, and that signing/staging/Plugin Portal tasks are configured, but it neither reads credentials nor creates signatures. The artifact group is the narrower `com.blackbuild.annodocimal`; do not rely on automatic leaf-group profile discovery.
4. Confirm the authorized RC or final protected environment contains only its scoped Maven Central, Plugin Portal, signing, and release-record credentials. Confirm the dedicated `annodocimal-pages-writer` environment and App credentials remain available only to the protected canonical Pages writer. Do not print, copy, inspect, or test a secret value in a command or evidence record.
5. Confirm the proposed tag `v<version>`, all Maven coordinates, both Plugin Portal marker coordinates, Pages snapshot paths, and the GitHub Release record are absent. A release must not replace an existing public version.

The protected release workflow itself needs an unprivileged preflight job. It strictly accepts only `rc` or `final`, maps that accepted value to exactly the corresponding protected environment, and exports that selected environment as a job output. The publishing job consumes that output. Invalid input must reach neither protected environment nor secret; do not use a fallback expression that could select the final environment before rejecting an unexpected stage.

## Protected authorization workflow

`Publish protected AnnoDocimal release` (`.github/workflows/publish-protected-release.yml`) is manually dispatched with
the exact `stage`, `version`, full lowercase `revision`, and an existing
`pending/<version>/<full-source-sha>` documentation path. The unprivileged preflight rejects any mismatched stage/version
or source build identity, non-current-master SHA, existing `v<version>` tag or GitHub Release record,
missing/mismatched pending manifest, or an already occupied public `/<version>/` Pages target. It maps only `rc` to `release-candidate` and `final` to
`final-release`; there is no default environment.

Those two reviewer-gated, credential-bearing environments are separate from both `annodocimal-pages-writer` and the
credential-free `github-pages` service environment. The publishing job alone receives `SONATYPE_USERNAME`,
`SONATYPE_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD`, `GRADLE_PUBLISH_KEY`, and `GRADLE_PUBLISH_SECRET`; it maps them
only to Gradle's scoped publication inputs. Ordinary CI, preflight, release rehearsal, public resolve-back, and the Pages
writer/service jobs have read-only repository access and none of those release secrets. Checkout credentials are never
persisted.

After reviewer approval, the publishing job repeats the exact-master and pending-handoff checks, requires Java 17, and
runs `publishCompleteProduct`. That Gradle entry point rejects a non-matching protected stage/version authorization or a
composite build, runs `check` and the six-coordinate/two-marker product gate before remote tasks, then stages/releases
the Maven Central product and publishes both Plugin Portal markers. It does not create a tag, GitHub Release, or public
Pages snapshot. The exact protected workflow is not a replacement for the existing protected Pages dispatches: first
stage pending documentation, then publish the product, then complete #44 public resolve-back, then create the exact tag
and CHANGES-derived GitHub Release, and only then dispatch the proof-gated public Pages handoff.

This repository workflow defines names and code only. A maintainer must separately create and reviewer-protect
`release-candidate` and `final-release`, add only the listed secrets to their matching environment, and
retain the existing Pages environment/ruleset setup. It does not create or configure environments, secrets, credentials,
rulesets, Pages, tags, releases, uploads, or workflow runs.

## Complete product and release evidence

Every RC and final uses one version across all six Maven Central coordinates:

| Coordinate | Role |
| --- | --- |
| `com.blackbuild.annodocimal:anno-docimal-annotations` | Documentation carrier annotations |
| `com.blackbuild.annodocimal:anno-docimal-apt` | Java annotation processor |
| `com.blackbuild.annodocimal:anno-docimal-ast` | Groovy AST capture and supported authoring API |
| `com.blackbuild.annodocimal:anno-docimal-global-ast` | Global Groovy AST capture service |
| `com.blackbuild.annodocimal:anno-docimal-generator` | Standalone source-projection generator |
| `com.blackbuild.annodocimal:anno-docimal-gradle-plugin` | Shaded Gradle plugin implementation |

The corresponding Gradle Plugin Portal markers are `com.blackbuild.annodocimal.base-plugin:com.blackbuild.annodocimal.base-plugin.gradle.plugin:<version>` and `com.blackbuild.annodocimal.groovy-plugin:com.blackbuild.annodocimal.groovy-plugin.gradle.plugin:<version>`. Each marker must point to the same-version implementation coordinate `com.blackbuild.annodocimal:anno-docimal-gradle-plugin:<version>`.

For each public version, retain a #45 release-evidence record containing the source SHA, version/tag, change-history section used for the GitHub Release, successful CI and local rehearsal links, registry/Plugin Portal transaction links, signature and propagation evidence, the #44 resolve-back evidence link, the exact immutable documentation/Javadocs URLs, and the public-artifact proof URL that authorizes a mutable Pages advance.

## RC ordering

1. Freeze the RC inputs and pass all preconditions. Apply #53 at its required late point, then repeat the exact-SHA and clean-CI checks. Stop safely here if any input or approval is missing.
2. Use the unprivileged preflight to validate `rc`, `<final>-rc.<N>`, and `<full-source-sha>` before selecting the RC environment. Run the complete-product Gradle guard before any remote mutation.
3. Render, crawl, and have the protected Pages writer add exactly one pending candidate evidence tree at `/pending/<version>/<full-source-sha>/`. It is unlisted, immutable, single-use evidence. It has no root status record and must not advance an alias or public status.
4. From the protected RC environment, create one Maven Central staging deployment and record its transaction ID. Upload the signed six-coordinate product, then inspect the staged POMs/Gradle metadata, sources, Javadocs, signatures, module identities, descriptors, and all expected files before closing it. Close/validate the staging transaction, record its validation result, and release/promote that exact transaction only after the complete set passes; then publish both Plugin Portal markers for that exact version and record its transaction/result. This is the second safe stop point: before Central promotion no public registry version exists, so a failed staging transaction may be abandoned and rebuilt from unchanged inputs. Do not make a tag, GitHub prerelease, public Pages snapshot, alias, or status claim during a partially completed registry operation.
5. Wait for Maven Central and Plugin Portal propagation, then hand off to #44 for a credential-free public resolve-back from only the real public repositories. It must resolve all six coordinates and both plugin markers at the exact RC version, and retain POM/Gradle metadata, signatures, sources/Javadocs, module identities, descriptors, and representative Gradle/Maven consumer evidence. Local, composite, flat-directory, and project fallbacks are prohibited. #44 is not a blocker for drafting or non-publishing rehearsal; it is a required post-public-RC delivery condition before #45 advances the RC's public release record or Pages status.
6. After that proof succeeds, create and verify `v<version>` at `<full-source-sha>`, create the GitHub prerelease from the projected `CHANGES.md` section, and record the public-artifact proof in #45.
7. Only with that recorded proof, have the protected writer publish the immutable public RC snapshot and aggregated Javadocs at `/<version>/`, then write its status record and proof-gated `/preview/` alias. The workflow must confirm the tag, exact source, and public proof before this mutable advance. Never rewrite the snapshot or its manifest.

At the end of step 3, the pending evidence is durable but not public. At the end of step 4, the version may already be burned; do not treat the GitHub prerelease or alias as the source of release identity.

## Final ordering and successor record

1. Accept an RC only with its complete #44 public resolve-back and #45 evidence. If the final requires a substantive change, create and validate the next RC instead. For the permitted documentation-only changelog date/release-note adjustment, freeze a new exact final SHA on current `master`.
2. Repeat preflight, complete-product guard, and a final pending evidence render at `/pending/<version>/<full-source-sha>/`; use `release_stage=final`. It remains unlisted and advances no status.
3. Publish all six signed final artifacts and both exact Plugin Portal markers from the final protected environment; wait for propagation and run the same credential-free public resolve-back for the exact final. Treat its evidence as a release gate, not as an optional consumer test.
4. Create and verify the final `v<version>` tag and GitHub Release from the dated `CHANGES.md` section. Record the final public-artifact proof and the exact RC it succeeds in #45.
5. Only after that proof, use the protected Pages writer to add the immutable final snapshot and aggregated Javadocs at `/<version>/`. Advance `stable`, `/<maintained-line>/`, and the final status/successor record only in this proof-gated operation, supplying the exact superseded RC. The RC snapshot remains immutable; the successor root record is the permitted mutable statement that the final supersedes it.
6. Publish an older tagged historical source, if required, only as `archived` at its own `/<version>/` route. `/archive/` is a discovery index, never an alternate snapshot namespace or a version alias.

The immutable documentation locations are therefore exactly `/pending/<version>/<full-source-sha>/` before public proof and `/<version>/` after it. `/archive/` is discovery only; `/preview/`, `/stable/`, and `/<maintained-line>/` are writer-owned aliases that move only after a #45-recorded public-artifact proof. Current Javadocs are inside each product snapshot, never a mutable alias substitute.

## Recovery and version consumption

Record each failed step, exact SHA/version, attempted registry transaction, visible coordinates, pending/public Pages paths, and whether a tag, GitHub Release, or alias/status record exists. Preserve logs and do not delete a public artifact or rewrite immutable evidence to make a retry appear clean.

| Condition | Recovery |
| --- | --- |
| Local checks, unprivileged preflight, or artifact-only rendering fails before any protected writer or registry action | **Safe retry.** Correct the local source or configuration, refreeze inputs, and re-run. A substantive correction needs a new RC version. |
| Pending writer action fails before its immutable tree exists | **Safe retry.** Diagnose the protected writer; retry the same exact request only after proving the path was not created. |
| Pending tree exists but later validation fails | The pending evidence is single-use and must not be overwritten. Reuse it only for the same unchanged release input while completing remaining checks; otherwise refreeze and use a new version/source identity. |
| Any Maven coordinate, Plugin Portal marker, or its implementation becomes publicly visible while the complete cross-registry product is absent or inconsistent | This is a **burned version**. Record the partial state, do not overwrite or republish it, and cut a new RC or final version after correcting the cause. Partial cross-registry publication always burns the version. |
| All artifacts are public but public resolve-back has not yet completed | Do not tag, create a release record, or advance Pages. Propagation/resolve-back may be retried without changing artifacts; a verification-discovered product defect requires a new version. |
| Tag or GitHub Release creation fails after proof | Retry only the idempotent release-record operation against the exact existing tag and CHANGES-derived text. Never move or recreate a conflicting tag. |
| Immutable snapshot write fails before remote commit | Retry only after proving the path is absent. If it exists, read back its manifest; never overwrite it. |
| Public snapshot exists but proof-gated aliases or status/successor record fail | **Safe retry.** Re-run only the guarded mutable advance with the same recorded proof and manifest. Do not render or replace the immutable snapshot. |

## Local non-publishing rehearsal

From a clean checkout, exercise the release-control seam without credentials or external release actions:

```shell
./gradlew releaseRehearsal -Prelease.version=1.0.0-rc.1
```

The task requires a syntactically exact RC or final `release.version`, a matching configured Gradle build version, and a clean worktree. It validates those inputs before Gradle executes the audit or renderer, then records the derived `v<version>`, the exact `git rev-parse HEAD` source revision, branch, and build version in `build/release-rehearsal/evidence.md`. It composes root `check` (including the unsigned six-coordinate/two-marker publication audit and signing-configuration inspection) with `renderLocalDocumentation` (credential-free presentation render and crawl). It writes only local build outputs and cannot tag, upload, dispatch, publish documentation, change environments, or mutate GitHub.

`ReleaseRehearsalContractTest.demonstrates the local non-publishing rehearsal` is the documentary happy path for this command.

This rehearsal proves the runbook's local evidence seam. It does not exercise protected credentials, signatures, registry staging, Plugin Portal upload, tags, GitHub Releases, workflow dispatch, Pages deployment, aliases, status, environments, or #44's public-consumer execution. Those remain explicit external release conditions and require the authorized RC/final procedure above.
