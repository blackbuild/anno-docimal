# Own release history and migrations in the repository

`CHANGES.md` is the canonical release history and maintains an unreleased section. Breaking changes receive migration
guides under `docs/user/migration/`, linked from the changelog and relevant user documentation. GitHub Release text publishes
the same repository-owned information rather than becoming an independent source. Java or Groovy baseline changes,
documentation-protocol migrations, artifact/module changes, and supported-API changes require explicit release and
migration documentation.
