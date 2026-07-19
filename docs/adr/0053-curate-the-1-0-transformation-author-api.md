# Curate the 1.0 transformation-author API

The supported transformation-author facade is `com.blackbuild.annodocimal.ast.AstDocumentation`. It is a non-
instantiable capability facade with these supported operations:

- `extractExact(AnnotatedNode)` returns `Optional<Documentation>` for documentation attached directly to that declaration;
- `attach(AnnotatedNode, Documentation)` canonically upserts the AnnoDocimal carrier;
- `attachText(AnnotatedNode, String)` bridges normalized textual content into the same attachment behavior; and
- `referenceTo(AnnotatedNode)` returns the canonical documentation reference for a declaration.

`attach` replaces the existing AnnoDocimal carrier, applies target-parameter filtering and template values, refreshes
AnnoDocimal metadata, and removes the canonical carrier when given empty documentation. It does not remove third-party
documentation carriers. Exact extraction never consults overridden declarations or supertypes; issue #10 owns a future,
separately named resolved-documentation capability.

`Documentation` is a final immutable semantic value with value equality, immutable collections, `empty()`, `isEmpty()`,
`builder()`, `toBuilder()`, `parse(String)`, and `render()`. Its stable vocabulary is an optional summary, ordered prose
and code blocks, parameter descriptions, an optional return description, exception descriptions, repeatable tags,
first-class links, and template values.
Parsing and rendering operate on normalized documentation content and preserve meaning rather than source delimiters,
whitespace, or original layout.

`Documentation.Builder` is a transient mutable, non-thread-safe builder whose builds are independent immutable snapshots.
Its operations set or explicitly clear singular content; add or replace blocks, parameters, exceptions, and tags; add
named template values; append a fallback with existing singular/named content winning; replace the whole document;
filter parameters; and build. Null arguments are rejected throughout. Blank descriptions remain valid, while block text
and names must be non-blank. Replacing an existing parameter or exception does not change its insertion position,
repeated tags append, collection setters replace their category, and empty collections clear it.

Issue #51 and KlumAST #489 completed the authoring-language decision recorded in ADR 0057. ADR 0058 reconciles the first
implementation with the final 1.0 Java shape: optional scalar accessors, explicit clear operations, a typed template map,
uniform value semantics, strict null handling, and removal of redundant aliases before the baseline is locked.

The 1.0 allowlist contains exactly `AstDocumentation`, `Documentation`, `Documentation.Builder`, `Documentation.Block`,
`Documentation.Tag`, `Documentation.Link`, and `Documentation.TemplateException`. `ASTExtractor`, `ClassDocExtractor`,
`InheritanceUtil`, the source extractors and
Groovy-version adapters, `DocText`, the current builders, `TemplateHandler`, `JavaDocUtil`, `AnnoDocUtil`, visitors,
transformations, and metadata caches are implementation-only or replaced. No 0.x shim is retained merely for
compatibility. This resolves issue #18's naming concern without merging source capture and AST extraction, which remain
different compiler-phase capabilities.
