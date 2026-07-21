# Versioned documentation and Javadocs

The repository owns the canonical source documentation. GitHub Pages publishes an additional immutable presentation of
one exact AnnoDocimal release commit; it never replaces the repository documentation or creates a wiki-owned source of
truth.

## Exact snapshots

The release workflow accepts only a full lowercase Git commit SHA, an exact `MAJOR.MINOR.PATCH` or
`MAJOR.MINOR.PATCH-rc.N` version, and the matching `archived`, `release`, or `public-rc` stage. It renders the exact
source object with the reviewed renderer revision. RC and final snapshots generate all six supported Java API Javadocs
and record the following in `/<version>/source-manifest.json`:

- the exact source and renderer commits;
- the version and publication stage;
- a digest for every Javadoc input; and
- the rendered file inventory and hashes.

Every Markdown page has renderer-owned immutable-snapshot chrome and every snapshot has a `version-status.md` record.
Every RC page also has a fixed link to `status/<rc-version>.json`. That record may later name the final release that
superseded the RC, but the RC tree and its source manifest are never changed. The successor record is accepted only by
a final render with an explicit exact RC predecessor; #45 must supply its release proof before requesting that update.

Historical releases live at `/archive/<version>/`. They are final-version snapshots with `Archived (legacy)` chrome,
may be rendered from README-only historic tags, and never receive current Javadocs. Ordinary immutable RC and final
snapshots remain at `/<version>/`.

RC and final renders require a versioned Season/logo manifest. A public RC may select a candidate manifest. A final
render must select `docs/branding/annodocimal-current.json`, whose logo digest is checked against the exact source
commit. That manifest is the then-current AnnoDocimal identity validation input. Issue #73 may replace it through an
accepted future manifest, but retaining the current identity is valid and does not block an RC.

The renderer rejects a dirty source worktree, an abbreviated or unresolved revision, non-exact version/stage pairs,
missing RC/final Javadocs, a non-current final branding manifest, invalid logo digest, unsafe output paths, unresolved
local Markdown links, and non-empty output directories.

The protected `github-pages` deployment adds exactly one previously absent `/<version>/` or
`/archive/<version>/` tree to the `gh-pages` branch. It may update only the root `status/` records, never a snapshot or
its manifest, and creates no mutable development, stable, or preview alias. The release runbook owned by
[issue #45](https://github.com/blackbuild/anno-docimal/issues/45) authorizes the release, preserves the proof before a
successor record is written, decides release metadata links, and records recovery or supersession; this renderer has no
artifact-publication authority.

After the protected snapshot is live, the release record must link to its immutable Pages URL,
`https://blackbuild.github.io/anno-docimal/<version>/`. A real link is intentionally added only by the authorized
release record for that version; no unreleased version is advertised from this repository landing page.

The protected workflow defaults to a non-publishing rehearsal: it verifies a tagged exact source/version/stage tuple and
uploads the complete site artifact, but skips the protected deployment job. A release authority must explicitly select
deployment after that rehearsal evidence is accepted.

Before the first authorized deployment, a maintainer must create the otherwise empty `gh-pages` branch, configure GitHub
Pages to serve that branch, and protect the `github-pages` environment with the release approvers. Those remote settings
are intentionally not created by this repository build.

## Local rehearsal

Build a non-publishing exact-site rehearsal with explicit inputs:

```shell
revision="$(git rev-parse HEAD)"
./gradlew renderVersionedDocumentation \
  -PdocumentationRevision="$revision" \
  -PdocumentationRendererRevision="$revision" \
  -PdocumentationVersion=1.0.0-rc.1 \
  -PdocumentationStage=public-rc \
  -PdocumentationBrandingManifest=docs/branding/annodocimal-current.json \
  -PdocumentationCurrentBrandingManifest=docs/branding/annodocimal-current.json \
  -PdocumentationObjectDirectory="$PWD" \
  -PdocumentationOutputDirectory="$PWD/build/versioned-documentation"
```

This command compiles Javadocs for `anno-docimal-annotations`, `anno-docimal-apt`, `anno-docimal-ast`,
`anno-docimal-global-ast`, `anno-docimal-generator`, and `anno-docimal-gradle-plugin`, then produces
`build/versioned-documentation/1.0.0-rc.1/`. The documentary happy path is
`VersionedDocumentationDocumentaryTest.demonstrates an immutable exact-site rehearsal`.

An archive rehearsal instead uses `-PdocumentationStage=archived` and a final successor update additionally uses
`-PdocumentationSuccessorOf=1.0.0-rc.1` with `-PdocumentationVersion=1.0.0` and
`-PdocumentationStage=release`. The latter produces `status/1.0.0-rc.1.json`; it does not write inside the prior RC
snapshot.
