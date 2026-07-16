# Issue branches and commit history

## Work on a dedicated branch

- Create a branch dedicated to the current issue or confirmed work item before the first commit. Include the issue number
  when one exists; otherwise use a short topic that identifies the session.
- Keep unrelated worktree changes out of the branch history.
- Agents may create and amend commits on a newly created dedicated branch without asking again.

## Tell the reasoning in small commits

- Make each commit one self-contained reasoning step rather than splitting mechanically by file or module.
- Use a concise imperative subject that explains what the change achieves. Add a body when the reason is not evident.
- Treat commit subjects and bodies as GitHub issue-linking input. Do not place a GitHub closing keyword before an issue
  reference unless merging that commit is intended to close the issue automatically; negated wording can still trigger
  GitHub's pattern matching. Use neutral issue references as described in `docs/agents/pull-requests.md`.
- Keep a test with the production change that makes it pass. Do not commit a deliberately failing TDD step.
- Run proportionate validation before each commit and keep required documentation consistent with the branch tip.

## Review history before first publication

- Compare the branch commits and final diff with the confirmed scope and acceptance criteria.
- Before review, combine, split, reorder, or reword local commits when that improves the reasoning sequence.
- Re-run the relevant tests after rewriting history.

## Preserve reviewed history

- Once human review or automated findings have examined a revision, treat those commits as frozen.
- Address review feedback in focused follow-up commits; do not amend, reorder, squash, or rebase reviewed commits without
  explicit maintainer direction.
- Cluster related review fixes when that tells a clearer story than one commit per observation.
