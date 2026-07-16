# Treat AnnoDocimal as an independent documentation pipeline

AnnoDocimal is a general-purpose documentation transport and source-projection library for generated JVM APIs, not a
KlumAST subsystem. Its supported contracts must be justified by AnnoDocimal's capture, extraction, documentation-model,
projection, and build-integration use cases; KlumAST-specific Builder wording and IDEA-only model wiring remain consumer
policy. KlumAST stays an important integration and compatibility consumer.
