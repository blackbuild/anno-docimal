# Cross-library Pages rehearsal alignment

Date: 2026-07-24

This is a research note for AnnoDocimal #71. It records current first-party
repository and GitHub state; it neither authorizes nor performs a remote
configuration or publication action.

## Finding

KlumAST's accepted contract is the disposable platform-rehearsal model: before
creating its canonical protected `gh-pages` ledger, maintainers may publish one
generated, non-release site for manual evaluation, then deactivate Pages and
delete the entire experimental branch. It must not use a release-like
`pending/<version>/<sha>/` path and creates no immutable release evidence.
[KlumAST ADR 0013](https://github.com/klum-dsl/klum-ast/blob/master/docs/adr/0013-versioned-documentation-and-javadocs.md)
and its [release runbook](https://github.com/klum-dsl/klum-ast/blob/master/RELEASING.md)
state that sequence explicitly. It was accepted through
[KlumAST PR #542](https://github.com/klum-dsl/klum-ast/pull/542).

That is a design decision, not completed KlumAST rehearsal evidence. KlumAST
#456 remains open; its VD-1 PR expressly excluded Pages deployment, and its
VD-5 evidence says that no rehearsal or Pages deployment was performed.
([#456](https://github.com/klum-dsl/klum-ast/issues/456),
[PR #537](https://github.com/klum-dsl/klum-ast/pull/537),
[VD-5 evidence](https://github.com/klum-dsl/klum-ast/blob/master/docs/implementation/evidence/issue-456-vd5-pending-pages-stage.md)).
The live GitHub Pages and `gh-pages` endpoints were unavailable when checked;
therefore this note does not claim that KlumAST has executed the teardown.

KlumCast supplies no contrary operational model. Its #47 is a post-KlumAST-4.0
planning issue that still needs to select KlumCast-local Pages mechanics;
current repository workflows contain no Pages workflow, and its live Pages and
`gh-pages` endpoints were unavailable when checked.
([KlumCast #47](https://github.com/klum-dsl/klum-cast/issues/47))

## Difference in AnnoDocimal

AnnoDocimal #71 requires a *non-publishing* rehearsal, rather than durable
non-release Pages evidence. ([#71](https://github.com/blackbuild/anno-docimal/issues/71))
At the start of this research, merged implementation defined an opt-in `deploy` mode that
added a previously absent, immutable
`rehearsal/commonmark-java-static-html-v1/<full-sha>/` tree to `gh-pages`.
([rehearsal workflow](https://github.com/blackbuild/anno-docimal/blob/master/.github/workflows/rehearse-versioned-documentation.yml),
[versioned-documentation guide](https://github.com/blackbuild/anno-docimal/blob/master/docs/versioned-documentation.md),
[publication validation](https://github.com/blackbuild/anno-docimal/blob/master/docs/publication-validation.md))
That retained-ledger behavior is inconsistent with the KlumAST disposable
platform-rehearsal decision and with the stated intent to remove the generated
first rehearsal site. Issue #71 was subsequently amended to select the
disposable pre-ledger model; its implementation must remove that retained
deployment seam.

## Recommended coordinated adjustment

Adopt the KlumAST model for AnnoDocimal's external Pages rehearsals:

1. Render and validate clearly non-release, non-`pending` sites from exact
   source revisions, then temporarily serve them only for manual presentation
   and deep-link verification until one result is accepted.
2. Record each result and exact revision in #71, retaining only the normal
   workflow artifact/evidence permitted by its retention policy—not a Pages
   ledger entry or release-evidence path.
3. Keep the experimental Pages boundary unprotected while rehearsals repeat to
   address found problems. After the first verified and accepted result,
   deactivate Pages and delete the entire experimental branch.
4. Only afterwards create a fresh orphan `.nojekyll` `gh-pages` ledger, apply
   the separately agreed protections/environments, and use it exclusively for
   future authorized immutable release evidence.

This preserves the release boundary: #45 remains the owner of release
authorization, publication ordering, recovery, RC/final publication, and
successor records. The change also removes the present contradiction between a
disposable first rehearsal and a retained rehearsal namespace. It does **not**
resolve the separate workflow-writer/branch-protection gap; that needs its own
least-privilege authentication decision before any canonical ledger deployment.

No remote mutation or AnnoDocimal policy change was made for this research.
