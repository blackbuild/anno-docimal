# Pull requests and release-facing documentation

Use this checklist for changes to public APIs, published artifacts, compatibility, generated projections, Gradle behavior,
or documentation semantics.

## Scope and evidence

- Link the confirmed issue or work item and state what remains deferred.
- Treat GitHub closing keywords as mechanical syntax, not prose. In pull-request titles and bodies, never place `close`,
  `closes`, `closed`, `fix`, `fixes`, `fixed`, `resolve`, `resolves`, or `resolved` before an issue reference unless the
  pull request is intended to close that issue automatically. Negation, quotation, code formatting, and explanatory
  context do not make the pattern safe. For non-closing relationships, use neutral wording such as `Related: #123`,
  `Issue #123 remains open`, or `This pull request leaves the issue state unchanged`.
- Keep the PR summary, compatibility impact, generated-output impact, and validation evidence current.
- Inspect all required CI and SonarCloud findings for the current revision. A green job proves the tested state, not that
  an architectural decision has been made.
- Run focused tests first, then all affected Groovy lanes; use root `check` for final repository verification.
- Verify that every newly added test carries its driving GitHub issue number in Spock's `@Issue`. A class-level annotation
  is sufficient while one issue owns the class; use method-level annotations for tests driven by later or different
  issues. Add or amend `@Issue` on changed existing tests only for significant behavioral changes, and do not retrofit
  unrelated tests.

## Review feedback

- Preserve reviewed commits and apply review fixes additively according to `docs/agents/commits.md`.
- Post one consolidated follow-up after fixes are pushed. Account for addressed findings, intentionally unchanged findings
  and their reasons, informational observations, commits, focused tests, compatibility lanes, CI, and static analysis.
- Resolving threads, dismissing reviews, or submitting a review still requires explicit authorization.

## Documentation and release impact

- Update `README.md` when the current user-facing usage or compatibility story changes.
- Put detailed user documentation under `docs/`; keep architectural decisions under `docs/adr/`. Do not make a separate
  wiki or generated site the canonical source.
- Keep published Javadoc, artifact descriptions, plugin metadata, examples, and generated-output terminology consistent.
- Record user-visible features, fixes, deprecations, and compatibility breaks under the unreleased section of `CHANGES.md`.
- Put breaking-change migration guides under `docs/migration/` and link them from `CHANGES.md` and the affected user
  documentation. Create the directory lazily when the first migration guide is needed.
- Keep GitHub Release text synchronized with `CHANGES.md`; it is a publication surface, not an independent source.
- Document changes to artifact coordinates, dependencies, Java/Groovy/Gradle compatibility, task behavior, caching,
  annotation processing, AST discovery, or source-projection fidelity before release.
- Treat Java and Groovy baseline changes and major implementation-technology switches as product-wide compatibility
  changes. Verify and document their effects across every published artifact and contract family.
