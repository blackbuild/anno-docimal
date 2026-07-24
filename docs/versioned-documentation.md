# Versioned documentation and Javadocs

The repository owns the canonical Markdown source. Public user guides live exclusively under `docs/user/`; maintainer,
agent, ADR, branding, and release-process material elsewhere under `docs/` is deliberately excluded. `docs/user/Home.md`
is the exact-tree landing page; the repository `README.md` and `CHANGES.md` are not site inputs. GitHub Pages publishes
an additional immutable static-HTML presentation of one exact AnnoDocimal commit; it never replaces the repository
documentation or creates a second authoring source.

## Exact snapshots

The release workflow accepts only a full lowercase Git commit SHA, an exact `MAJOR.MINOR.PATCH` or
`MAJOR.MINOR.PATCH-rc.N` version, and the matching `pending`, `public-rc`, `current`, or `archived` status. A `pending`
render additionally requires `documentationReleaseStage=candidate|final`; that property is rejected for every other
status. Product snapshots generate all six supported Java API Javadocs from the same exact source checkout.

Markdown is never the Pages payload. The pinned repository-local CommonMark 0.28.0 renderer produces complete,
dependency-free HTML with renderer-owned chrome and a local stylesheet. It rewrites Markdown and deep-fragment links
to extensionless Pages URLs, escapes authored HTML, and sanitizes unsafe URL schemes. `docs/user/Home.md` becomes
`index.html`; `docs/user/path/page.md` becomes `path/page/index.html`. Authored `docs/user/_Sidebar.md` and `_Footer.md` fragments
supply navigation content and a short product footer without becoming public pages. The renderer still owns their HTML
shell, safety, base-path rewriting, lifecycle/API links, and fallback presentation.

Each exact tree contains `source-manifest.json`, which records:

- the exact source and renderer commits;
- the renderer contract, pinned CommonMark version, and safety modes;
- the version, lifecycle status, and conditional release stage;
- a digest for every Javadoc input; and
- the deployed HTML, asset, and Javadoc inventory and hashes.

The manifest excludes only itself and excludes mutable root status records. Every rendered page links to the local
`status/index.html`. Every public-RC page therefore retains an immutable warning and status link; that local status page
has a fixed link to `/anno-docimal/status/<rc-version>.json`. The separate root record may later name the final release
that superseded the RC, but the RC tree and its manifest are never changed. A successor record is accepted only by a
final render with an explicit exact RC predecessor; #45 must provide release proof before requesting that update.

Pending evidence is immutable, deployed-but-unlisted pre-publication proof. The protected workflow places it at
`/pending/<version>/<revision>/`; it creates no public root status record and advances no public, stable, or line alias.
It is not a public RC or final-release claim, and its path is single-use release evidence rather than a disposable test.

Historical releases live at `/archive/<version>/`. They are final-version snapshots with `Archived (legacy)` chrome,
may be rendered from README-only historic tags, may omit historical branding, and never receive current Javadocs.
Ordinary immutable RC and final snapshots remain at `/<version>/`.

Product renders require a versioned Season/logo manifest. A public RC or pending candidate may select a candidate
manifest. A current or pending final render must select `docs/branding/annodocimal-current.json`, whose logo digest is
checked against the exact source commit. Issue #73 may replace it through an accepted future manifest, but retaining the
current identity is valid and does not block an RC.

The renderer rejects dirty source worktrees, abbreviated or unresolved revisions, non-exact version/status pairs,
misplaced pending release stages, missing product Javadocs, non-current final branding, invalid logo digests, output
collisions, and non-empty output directories. The mandatory JDK-only presentation check serves the artifact under the
real `/anno-docimal/` Pages base path and crawls the renderer-owned output selector, exact-tree landing, every local page
and asset, Javadocs, and fragments. Markdown URLs, missing targets, missing anchors, and base-path escapes fail the check.

The protected release deployment adds exactly one previously absent `/<version>/`,
`/pending/<version>/<revision>/`, or `/archive/<version>/` tree to `gh-pages`. Pending evidence does not touch root
`status/`. A public deployment may update only those root status records, never a snapshot or manifest, and creates no
mutable development, stable, or preview alias. [Issue #45](https://github.com/blackbuild/anno-docimal/issues/45) owns
release authorization, proof, metadata links, recovery, and supersession; this renderer has no artifact-publication
authority.

The release workflow defaults to artifact-only validation. It renders, crawls, and uploads the complete site but skips
the protected canonical writer job. Public statuses additionally require the matching version tag; pending proof precedes
publication and therefore does not. Render and crawl jobs have only read permission and never receive the canonical
writer App material.

## Protected canonical writer

The `github-pages` environment gates the distinct canonical writer job. Only that job may receive the environment-scoped
dedicated Pages-writer App identifier and private key, and it mints that App's installation token only after the artifact
has been staged. The workflow token remains read-only; the App token is used only for the `gh-pages` push. Before that
push, the writer resolves `refs/heads/master` remotely and refuses to continue unless its exact commit equals the
requested source revision. It also verifies that the staged `source-manifest.json` binds that source, version, and
status. After the push, a fresh remote checkout must resolve to the pushed commit and contain byte-identical manifest
content with the same source/version/status binding. A missing protected-environment value fails the writer before token
minting or canonical mutation. This workflow defines no App, secret, environment, Pages setting, branch rule, or
`gh-pages` branch; those remain separate maintainer-controlled setup.

The documentary contract
`VersionedDocumentationDocumentaryTest.keeps the protected canonical writer separate from artifact-only rendering`
checks this checked-in authority boundary while the release and rehearsal examples below prove rendering behavior.

Before any authorized deployment, a maintainer must create `gh-pages`, configure Pages to serve it, and protect the
`github-pages` environment. This repository configuration does not perform those remote actions.

## Local presentation rehearsal

Render and crawl the current checkout with no credentials and no release identity:

```shell
./gradlew renderLocalDocumentation
```

The renderer-owned selector is written to `build/versioned-documentation/local-review/index.html`. It links the exact
tree at `local-rehearsal/` and visibly identifies both renderer contract and full source SHA without resembling a
release version or consuming a `pending` path. The tree is explicitly labelled as a
non-release rehearsal, creates no root status record, and never uses `pending`. CI runs the same command and uploads the
crawled artifact.

Inspect it in a browser under the real Pages base path with:

```shell
./gradlew previewLocalDocumentation
```

The task prints a loopback `/anno-docimal/` URL and runs until interrupted.

The separate presentation-rehearsal workflow is artifact-only. It renders and crawls the exact local tree, then uploads
the result; it has no Pages write permission, deployment input, protected environment, or Pages-writer App material. It
writes no version status, alias, release record, pending path, or Pages tree. Merging the workflow neither configures
Pages nor dispatches it.

One or more maintainer-authorized platform rehearsals may use that generated artifact to populate a clearly experimental
branch for manual Pages inspection. They are neither release evidence nor protected ledger entries: they use no `pending`
or release-snapshot path, and each exact revision and validation result is recorded in issue #71. If a rehearsal reveals
a problem, correct it and rehearse again on the experimental boundary. Pages and its branch remain unprotected until the
first rehearsal result is verified and accepted; then set the Pages source to `None` and delete the complete experimental
branch. Only after that teardown may a fresh orphan `gh-pages` branch with root `.nojekyll` become the protected canonical
ledger. The workflow never automates either Pages configuration or this deletion.

## Explicit release render

To exercise a release identity locally without publishing, render and crawl it with explicit inputs:

```shell
revision="$(git rev-parse HEAD)"
./gradlew verifyRenderedVersionedDocumentationSite \
  -PdocumentationRevision="$revision" \
  -PdocumentationRendererRevision="$revision" \
  -PdocumentationVersion=1.0.0-rc.1 \
  -PdocumentationStatus=public-rc \
  -PdocumentationBrandingManifest=docs/branding/annodocimal-current.json \
  -PdocumentationCurrentBrandingManifest=docs/branding/annodocimal-current.json \
  -PdocumentationObjectDirectory="$PWD" \
  -PdocumentationOutputDirectory="$PWD/build/versioned-documentation"
```

This compiles all six module Javadocs and produces a local selector plus static HTML under
`build/versioned-documentation/1.0.0-rc.1/`. The protected deployment consumes only the exact tree, not that local
selector. The documentary happy path is
`VersionedDocumentationDocumentaryTest.demonstrates an immutable exact-site rehearsal`.

An archive render instead uses `-PdocumentationStatus=archived`. A final successor update additionally uses
`-PdocumentationSuccessorOf=1.0.0-rc.1`, `-PdocumentationVersion=1.0.0`, and `-PdocumentationStatus=current`; it
produces `status/1.0.0-rc.1.json` without writing inside the prior RC snapshot.

A pending candidate proof uses `-PdocumentationStatus=pending` and `-PdocumentationReleaseStage=candidate`. Its local
artifact remains under `<output>/1.0.0-rc.1/`; only the protected deployment seam maps it to the single-use
`/pending/1.0.0-rc.1/<revision>/` evidence path.
