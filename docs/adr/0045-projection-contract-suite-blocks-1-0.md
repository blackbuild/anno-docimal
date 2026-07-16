# Recompile representative source projections before 1.0

A source-projection contract suite blocks AnnoDocimal 1.0. It must project representative Java and Groovy declaration
shapes, assert deterministic documentation-oriented output, and compile the resulting Java. Coverage includes generics,
nested types, annotations, supported records/enums and declaration forms, and canonical/interoperable documentation
carriers across Groovy 3, 4, and 5. Issue #32 becomes one regression fixture in this broader matrix, not the only
compilation check. GitHub issue #41 owns this suite.
