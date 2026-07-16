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

### Domain documentation

AnnoDocimal is a single-context repository. Read the root `CONTEXT.md` before architecture or API work and read relevant
decisions under `docs/adr/`. See `docs/agents/domain.md`.

### Coding style

Follow the surrounding Java, Groovy, and Gradle conventions. Import referenced Java and Groovy types and use their simple
names unless a conflict or generated-source constraint requires qualification. See `docs/agents/coding-style.md`.

### Testing

Groovy 3 is the baseline `test` lane. Groovy 4 and Groovy 5 compatibility use `groovy4Tests` and `groovy5Tests`; root
`check` includes the compatibility suites for projects using the multi-Groovy convention. See `docs/agents/testing.md`.

### Issue implementation and commits

Implement issues on a new dedicated branch using small, reasoned commits. Clean up local history before first review;
after review begins, preserve reviewed commits and add fixes in follow-up commits. See `docs/agents/commits.md`.

### Pull requests and releases

Keep public behavior, compatibility evidence, generated-source behavior, user documentation, issue links, and release
notes consistent. Respond to review feedback once, in a consolidated follow-up containing addressed and intentionally
unchanged findings plus validation evidence. See `docs/agents/pull-requests.md`.
