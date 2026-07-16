---
name: implement
description: "Implement a piece of work based on a PRD or set of issues."
---

Implement the work described by the user in the PRD or issues.

Read the repository's agent instructions, `docs/agents/coding-style.md`, `docs/agents/commits.md`, and `docs/agents/pull-requests.md`. Before the first commit, create a new branch dedicated to the issue from the agreed base, or confirm the current branch was newly created for this issue.

Use the `tdd` workflow where possible, at pre-agreed seams. Commit each completed reasoning slice according to `docs/agents/commits.md`; a TDD commit contains the failing test and the change that makes it green.

Run the narrowest relevant Gradle test task regularly. Exercise Groovy 4 and 5 lanes whenever compiler, AST, bytecode,
or rendering compatibility can differ, and run root `./gradlew check` once at the end.

Keep required documentation consistent with the final behavior. It may be a separate final commit.

Once done, use the `code-review` workflow against the issue branch base, commit any fixes, then review and improve the complete commit sequence as required by `docs/agents/commits.md` before first publication. Re-run the final verification after any history rewrite. When responding to pull-request review feedback, preserve the reviewed commits, add one or more focused follow-up commits, push them, and post the consolidated disposition required by `docs/agents/pull-requests.md`.
