# Separate exact documentation from inherited resolution

Documentation extraction distinguishes exact documentation attached to a declaration from resolved documentation inherited
or augmented from overridden methods and supertypes. Resolved documentation follows documented Javadoc-like rules,
including explicit `{@inheritDoc}` augmentation, rather than relying on Java `@Inherited` or a generator-only heuristic.
Issue #10 owns the detailed resolution algorithm and facade design; projection does not apply inheritance invisibly.
