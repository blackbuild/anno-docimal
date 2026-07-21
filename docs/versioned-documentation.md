# Versioned documentation and Javadocs

The repository owns the canonical source documentation. GitHub Pages publishes an additional immutable presentation of
one exact AnnoDocimal release commit; it never replaces the repository documentation or creates a wiki-owned source of
truth.

## Exact snapshots

The release workflow accepts only a full lowercase Git commit SHA, an exact `MAJOR.MINOR.PATCH` or
`MAJOR.MINOR.PATCH-rc.N` version, and the matching `release` or `public-rc` stage. It checks out that commit, generates
the site and all six supported Java API Javadocs, and records the following in
`/<version>/source-manifest.json`:

- the exact source and renderer commits;
- the version and publication stage;
- a digest for every Javadoc input; and
- the rendered file inventory and hashes.

Every Markdown page has renderer-owned immutable-snapshot chrome and every snapshot has a `version-status.md` record.
The renderer rejects a dirty source worktree, an abbreviated or unresolved revision, non-exact version/stage pairs,
missing module Javadocs, unsafe output paths, unresolved local Markdown links, and non-empty output directories.

The protected `github-pages` deployment adds exactly one previously absent `/<version>/` tree to the `gh-pages` branch.
It never rewrites an existing snapshot and creates no mutable development, stable, or preview alias. The release runbook
owned by [issue #45](https://github.com/blackbuild/anno-docimal/issues/45) authorizes the release, decides any release
metadata links, and records recovery or supersession; this renderer has no artifact-publication authority.

After the protected snapshot is live, the release record must link to its immutable Pages URL,
`https://blackbuild.github.io/anno-docimal/<version>/`. A real link is intentionally added only by the authorized
release record for that version; no unreleased version is advertised from this repository landing page.

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
  -PdocumentationObjectDirectory="$PWD" \
  -PdocumentationOutputDirectory="$PWD/build/versioned-documentation"
```

This command compiles Javadocs for `anno-docimal-annotations`, `anno-docimal-apt`, `anno-docimal-ast`,
`anno-docimal-global-ast`, `anno-docimal-generator`, and `anno-docimal-gradle-plugin`, then produces
`build/versioned-documentation/1.0.0-rc.1/`. The documentary happy path is
`VersionedDocumentationDocumentaryTest.demonstrates an immutable exact-site rehearsal`.
