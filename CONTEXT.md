# AnnoDocimal

AnnoDocimal is an independent documentation transport and source-projection library for generated JVM APIs. KlumAST is
an important consumer, but its DSL model and IDE integration do not define AnnoDocimal's product boundary.

## Language

**Documentation protocol**:
The cross-stage representations by which documentation capture, extraction, projection, and runtime consumers exchange
documentation. The protocol can evolve beyond the current single-string carrier without changing its ownership.
_Avoid_: AnnoDoc annotation when referring to the complete extensible contract

**Normalized documentation content**:
Textual documentation after source-comment delimiters and per-line decoration have been removed. It retains the content
needed for semantic parsing and later rendering without retaining whether the source used traditional Javadoc or a
newer Markdown documentation-comment syntax.
_Avoid_: Raw comment when source syntax has been removed

**Source documentation**:
Documentation attached to Java or Groovy declarations in source form before compilation.
_Avoid_: Source Javadoc when the distinction between Java and Groovy matters

**Embedded documentation**:
Documentation carried in compiled output so later compilation or projection stages can recover it.
_Avoid_: Runtime Javadoc

**Interoperable documentation carrier**:
A language- or tool-owned representation, such as Groovy's runtime GroovyDoc annotation, that AnnoDocimal can consume
without making it the canonical cross-language protocol or its schema-evolution boundary.
_Avoid_: Native protocol when referring to an accepted alternate input

**Documentation properties**:
Per-class resources that carry Java source documentation when annotations cannot be added to an existing class.
_Avoid_: Generated Javadoc files

**Structured documentation annotations**:
A future protocol representation that may model tags, formatting, and parameters separately rather than encoding the
complete document in one string. Its concrete schema and migration from the current carrier remain undecided.
_Avoid_: AnnoDoc 2 until a name and contract are confirmed

**Documentation capture**:
The conversion of source documentation into embedded documentation or documentation properties during compilation.
_Avoid_: Stub generation

**Documentation extraction**:
Reading documentation associated with a compiler model element, regardless of whether it came from embedded
documentation or documentation properties.
_Avoid_: Source extraction when the source representation is irrelevant

**Exact documentation**:
Documentation attached directly to one declaration without consulting overridden declarations or supertypes.
_Avoid_: Local documentation, which is also used for AST transformation scope

**Resolved documentation**:
Documentation produced by applying the library's explicit inheritance and `{@inheritDoc}` resolution policy to a
declaration.
_Avoid_: Inherited annotation

**Property documentation**:
Documentation authored for a Groovy property and semantically associated, under an explicit policy, with its property
declaration, backing field, and generated accessors. Capture should retain this association while the compiler exposes
the property model.
_Avoid_: Getter documentation when the source declaration is a property

**Documentation model**:
The structured summary, ordered prose/code blocks, parameter descriptions, return description, exceptions, tags, and template values used
to inspect or construct documentation.
_Avoid_: Formatter API

**Documentation summary**:
The primary introductory description of a documentation model, before its ordered body blocks.
_Avoid_: Documentation title

**Source projection**:
Java source reconstructed from compiled classes so standard source-oriented tools can observe generated APIs and their
documentation. A projection preserves selected declaration meaning, not original implementation or textual formatting.
_Avoid_: Decompilation

**Class stub**:
A source projection intended as input to Javadoc or another source-oriented documentation tool.
_Avoid_: Java source mirror when the output participates in documentation generation

**Source mirror**:
A source projection exposed to an IDE for navigation and completion without being compiled, packaged, or published as a
second API. IDE model wiring remains the consuming build's responsibility.
_Avoid_: Generated source when that implies a compilation input

**Local documentation transformation**:
Documentation capture enabled explicitly for selected Groovy declarations.
_Avoid_: Annotation processor

**Global documentation transformation**:
Documentation capture discovered for a Groovy compilation through the global AST transformation service mechanism.
_Avoid_: Gradle plugin

**Versioned documentation snapshot**:
An immutable rendered presentation of repository-owned documentation and API reference for one exact product version
and source commit. It does not replace the canonical authored source.
_Avoid_: Documentation source when referring to the rendered site

**Pending documentation evidence**:
An immutable, unlisted, single-use proof for one candidate or final release before it becomes public. It advances no
public navigation or alias by itself.
_Avoid_: Preview or rehearsal, which implies disposable evidence

**Documentation presentation rehearsal**:
A non-release render used to prove static-site presentation and navigation without claiming or consuming a release
version. It writes no lifecycle status record or alias.
_Avoid_: Pending documentation, which is protected single-use release evidence
