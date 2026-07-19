# Reconcile the authoring model with the 1.0 Java API

Issue #51 and KlumAST #489 established the authoring language in ADR 0057 and delivered its first implementation. Before
the 1.0 baseline is locked, issue #38 must make the following clean-cut Java API corrections. They refine the supported
shape without changing the accepted documentation language.

- `Documentation.getSummary()`, `Documentation.getReturnDescription()`, and `Documentation.Link.getLabel()` return
  `Optional<String>`. The primary transformation consumers are Java implementations even when they transform Groovy;
  Groovy 3, 4, and 5 also give `Optional` useful truth semantics.
- Every public facade and builder argument rejects `null`. Blank descriptions remain valid. Empty replacement
  collections clear their category. `clearSummary()` and `clearReturn()` are the explicit singular clearing operations.
- `Documentation.parse(null)` rejects `null`, while blank text parses to `Documentation.empty()`.
  `AstDocumentation.attach` rejects a null node or document; callers remove documentation with
  `Documentation.empty()`. `attachText` rejects null input and treats blank input as empty/removal.
- `Documentation`, `Documentation.Block`, `Documentation.Tag`, and `Documentation.Link` all have value-based
  `equals`, `hashCode`, and diagnostic structural `toString`. `toString` must not render templates, and its exact text is
  not a compatibility contract.
- `Documentation.Builder.templateValues` accepts `Map<String, String>`. Raw Java and dynamic-language calls containing
  null or non-string values still fail with the target and key context required by ADR 0057.
- `paragraph` and `returns` are the single supported append/set names. The redundant `appendParagraph` and
  `replaceReturn` aliases are removed before 1.0. Their semantics and the replacement operations remain explicit in
  documentation rather than duplicated in method names.

The final AST allowlist is therefore `AstDocumentation`, `Documentation`, `Documentation.Builder`,
`Documentation.Block`, `Documentation.Tag`, `Documentation.Link`, and `Documentation.TemplateException`. No current
0.x helper is retained as a shim. The scoped baseline introduced with issue #51 is evidence, not yet the final 1.0
baseline: issue #38 must update it after these corrections, and the release compatibility work must apply the complete
per-artifact rules in the supported-API record.
