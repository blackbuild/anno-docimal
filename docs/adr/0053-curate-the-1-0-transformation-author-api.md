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
`builder()`, `toBuilder()`, `parse(String)`, and `render()`. Its stable vocabulary is summary, ordered paragraphs,
parameter descriptions, optional return description, exception descriptions, repeatable tags, and template values.
Parsing and rendering operate on normalized documentation content and preserve meaning rather than source delimiters,
whitespace, or original layout.

`Documentation.Builder` is a transient mutable, non-thread-safe builder whose builds are independent immutable snapshots.
Its confirmed non-template operations set the summary; add or replace paragraphs; add or replace parameter, return,
exception, and tag content; merge missing content from a fallback with existing content winning; and build. Null values and
blank parameter, exception, or tag names are rejected. Blank descriptions are allowed. Replacing an existing parameter
or exception does not change its insertion position, repeated tags append, collection setters replace their category,
and empty collections clear it.

The template and richer authoring-language members of the builder remain provisional until issue #51 is decided with
KlumAST issue #489. That gate includes code blocks, possible paragraph kinds, substitution and escaping, missing-value and
failure behavior, merge rules, and the final template-member allowlist. It remains a 1.0 blocker and does not preserve the
current 0.x template helpers as API.

The 1.0 allowlist contains only `AstDocumentation`, `Documentation`, and `Documentation.Builder`, plus the authoring
members later accepted by issue #51. `ASTExtractor`, `ClassDocExtractor`, `InheritanceUtil`, the source extractors and
Groovy-version adapters, `DocText`, the current builders, `TemplateHandler`, `JavaDocUtil`, `AnnoDocUtil`, visitors,
transformations, and metadata caches are implementation-only or replaced. No 0.x shim is retained merely for
compatibility. This resolves issue #18's naming concern without merging source capture and AST extraction, which remain
different compiler-phase capabilities.
