# Coordinate the 1.0 authoring language with KlumAST

The documentation authoring and template language had to be stabilized before AnnoDocimal 1.0. AnnoDocimal issue #51
owned the reusable language and supported API; KlumAST issue #489 owned concrete consumer requirements and migration.
Both issues are complete, and ADR 0057 records the resulting language. They no longer block issue #38 or #39.

KlumAST is the only proven current consumer and the primary driver for template substitution, code blocks, and possible
paragraph kinds. klum-wrap predates AnnoDocimal and currently has no integration, but its AST-transformation focus makes
it a likely future adopter; it is consulted as an adaptable consumer rather than a co-owner or source of hard
requirements.

KlumCast's diagnostic message templates are a separate domain and impose no requirement on documentation authoring.
Their basic named-template mechanism should nevertheless be consulted for useful alignment. If real common logic emerges,
a future separate library may own it; shared extraction is an option, not an objective or 1.0 dependency.

The coordination did not promote any existing 0.x authoring type. `DocBuilder`, `JavadocDocBuilder`, `DocText`, and
`TemplateHandler` remain implementation-only or replaced. The authoring slot is now final; ADR 0058 records the small
pre-baseline API-shape corrections that issue #38 must make to the first implementation.
