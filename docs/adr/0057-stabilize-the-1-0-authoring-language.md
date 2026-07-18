# Stabilize the 1.0 documentation authoring language

AnnoDocimal 1.0 authoring uses immutable `Documentation` values and the transient
`Documentation.Builder`; it does not promote `DocBuilder`, `JavadocDocBuilder`, `DocText`, or
`TemplateHandler` to supported API.

Documentation has a summary, authored-order blocks, named parameter and exception descriptions, an optional return
description, and ordered generic tags. Prose and code are the only first-class block forms in 1.0. Code renders as a
`<pre>` block and prose as a `<p>` block. Generic block kinds and paragraph qualifiers remain additive future work.

Composition is deliberate: builders append a fallback document, replace all content, or filter parameter descriptions.
Appending preserves authored block and generic-tag order, with the receiving document winning for its existing singular
or named content. Replacing a named parameter or exception retains its insertion position. Rendering orders parameter
tags by the final target signature and removes documentation for parameters not in that signature. Parsing and rendering
normalize line endings, block layout, tag continuations, and ordering; `parse(render(document))` preserves the document's
meaning, not its original whitespace or source decoration.

Templates are explicit builder metadata. A named value is written as `{{key}}`; a conditional parameter fragment is
written as `{{param:key?fragment}}`. Rendering is a single pass: supplied `String` values and fragments are literal,
are neither recursively evaluated nor implicitly escaped, and only an absent output parameter omits its conditional
fragment. Missing values, malformed delimiters or keys, and null or non-`String` inputs fail with target and key context.
Parsing an `@template` tag keeps it a generic tag; it never creates template metadata implicitly.

Links are `Documentation.Link` values. `AstDocumentation.referenceTo` creates a canonical class/member reference and
fails with target context for unsupported or incomplete AST nodes. The link renders canonically with `inline()` or via
the builder's `see` operation. `Documentation.Link.text` is intentionally author-owned text and is not validated as an
AST target.

The supported transformation facade is `AstDocumentation`: exact extraction, semantic/text attachment, and AST
reference creation. Exact extraction never resolves inherited documentation. Attachment replaces AnnoDocimal's carrier,
retains third-party annotations, filters parameters against the target's final signature, and removes its carrier for an
empty document.

Issue #30 still owns a future structured carrier schema; issue #10 owns inherited documentation resolution. This
decision does not introduce either. KlumAST #455's multi-Groovy design and a shared KlumCast/klum-wrap template library
remain outside the 1.0 boundary.
