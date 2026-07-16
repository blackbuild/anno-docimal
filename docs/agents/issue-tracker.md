# Issue tracker: GitHub

GitHub Issues are the repository's issue and product-requirement tracker. Infer the repository from the configured Git
remote and use read-only inspection before proposing mutations.

## Issue handling

- Verify an issue against owning source, tests, build configuration, documentation, history, and linked consumers.
- Separate confirmed behavior, current limitations, proposed behavior, and acceptance evidence.
- Do not create, rewrite, close, label, assign, or relate issues until the maintainer confirms the intended result.
- Use closing keywords only when a change fully delivers the confirmed issue contract.
- Keep cross-repository work in the repository that owns the decision. Link related consumer work without duplicating its
  product policy.

External pull requests are not a feature-request surface. Review them as proposed changes, not as accepted requirements.
