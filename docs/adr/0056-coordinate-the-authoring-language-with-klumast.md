# Coordinate the 1.0 authoring language with KlumAST

The documentation authoring and template language must be stabilized before AnnoDocimal 1.0, but it is not decided by
issue #37. AnnoDocimal issue #51 owns the reusable language and supported API; KlumAST issue #489 owns concrete consumer
requirements and migration. This gate blocks the template-related portion of issue #38 but does not block issue #39.

KlumAST is the only proven current consumer and the primary driver for template substitution, code blocks, and possible
paragraph kinds. klum-wrap predates AnnoDocimal and currently has no integration, but its AST-transformation focus makes
it a likely future adopter; it is consulted as an adaptable consumer rather than a co-owner or source of hard
requirements.

KlumCast's diagnostic message templates are a separate domain and impose no requirement on documentation authoring.
Their basic named-template mechanism should nevertheless be consulted for useful alignment. If real common logic emerges,
a future separate library may own it; shared extraction is an option, not an objective or 1.0 dependency.

Until the coordinated decision is complete, no existing 0.x authoring type is provisional API. `DocBuilder`,
`JavadocDocBuilder`, `DocText`, and `TemplateHandler` remain implementation-only or replaced. Only the not-yet-final
authoring members of `Documentation.Builder` are a provisional slot, with an explicit 1.0 disposition of resolve through
issue #51 before the compatibility baseline is locked.
