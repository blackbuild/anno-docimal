## Agent guidance

### Issue tracker

Issues and product requirements for this repository live in GitHub Issues. External pull requests are not treated as a
feature-request surface. See `docs/agents/issue-tracker.md`.

### Triage roles

Use the confirmed canonical triage-role mapping in `docs/agents/triage-labels.md`. Creating, renaming, or applying labels
still requires maintainer authorization for the intended mutation.

### Reusable workflows

Repository-local generic workflows live under `.agents/skills/`. Use them when their descriptions match the task, and
apply this repository's `AGENTS.md`, `CONTEXT.md`, ADRs, and `docs/agents/` policies where generic examples differ. Do not
invent AnnoDocimal-specific workflows until repeated repository work demonstrates a stable need.

### License plugin

If license-plugin configuration conflicts with a planned change, ask for the plugin/configuration to be changed. Never
rename, retype, or otherwise adapt files merely to circumvent license-header handling; for example, do not rename a
`.txt` file to `.java` because the plugin cannot handle `.txt`. An outdated license-file year requires a dedicated issue.
If an outdated year or related structural problem is discovered incidentally during another task, ask for confirmation
before creating the separate issue or task.

### Domain documentation

AnnoDocimal is a single-context repository. Read the root `CONTEXT.md` before architecture or API work and read relevant
decisions under `docs/adr/`. See `docs/agents/domain.md`.

### Coding style

Follow the surrounding Java, Groovy, and Gradle conventions. Import referenced Java and Groovy types and use their simple
names unless a conflict or generated-source constraint requires qualification. See `docs/agents/coding-style.md`.

### Testing

Groovy 3 is the baseline `test` lane. Groovy 4 and Groovy 5 compatibility use `groovy4Tests` and `groovy5Tests`; root
`check` includes the compatibility suites for projects using the multi-Groovy convention. See `docs/agents/testing.md`.
Documentation-only changes that cannot affect compilation, test execution, generated output, or runtime behavior do not
require Gradle checks; run the applicable documentation and diff checks instead.
Every newly added test must carry its driving GitHub issue number in Spock's `@Issue`; a class-level annotation is
sufficient while one issue owns the class, and individual methods identify tests driven by later or different issues.
Add or amend `@Issue` on changed existing tests only for significant behavioral changes. Apply this rule prospectively;
do not retrofit unrelated tests.
Every newly added user-visible feature also needs a readable documentary happy-path test marked with
`@Tag("documentary")` and linked through `@See` to its canonical documentation under `docs/`. Name new executable test
classes with the `Test` suffix; use `<Theme>DocumentaryTest` for cohesive dedicated documentary classes, and do not
blanket-rename existing `*Spec` classes. Keep the feature issue, documentary test, and user documentation mutually
traceable. See `docs/agents/testing.md`.

### Feature discussion examples

During grilling and implementation, use a compact example as a syntax or API probe for a user-visible feature. Show public
Java client APIs in Java first and, when meaningful, Groovy second. For Groovy-facing AST authoring or Gradle/plugin
behavior, use the natural Groovy or Gradle DSL surface. Evolve the accepted example into the documentary test and its
abbreviated user-documentation example. Internal-only changes may omit the example with a brief reason. See
`docs/agents/testing.md`.

### Issue implementation and commits

Implement issues on a new dedicated branch using small, reasoned commits. Clean up local history before first review;
after review begins, preserve reviewed commits and add fixes in follow-up commits. See `docs/agents/commits.md`.

### Pull requests and releases

Keep public behavior, compatibility evidence, generated-source behavior, user documentation, issue links, and release
notes consistent. Respond to review feedback once, in a consolidated follow-up containing addressed and intentionally
unchanged findings plus validation evidence. See `docs/agents/pull-requests.md`.
