# Changes

## Unreleased

- Rewrote the repository-owned landing page, usage guide, and migration guidance for the 1.0 documentation contract.
  They now describe the six artifacts, Java and Groovy capture, the supported authoring and projection APIs, automatic
  module names, Javadoc and IDE-mirror boundaries, and the current versus #35-owned Gradle integration. See
  [the usage guide](docs/usage.md) and [0.x-to-1.0 migration](docs/migration/0.x-to-1.0-supported-api.md).

- Defined the supported 1.0 Java API as non-null by default with JSpecify. Supported annotation, transformation-author,
  and source-projection types are `@NullMarked`, genuine raw-null inputs are explicit, and `Optional`, runtime null
  rejection, and template semantics are unchanged. See the [supported API](docs/api/1.0-supported-api.md) and
  [0.x-to-1.0 migration](docs/migration/0.x-to-1.0-supported-api.md).

- Added the supported `SourceProjector` and explicit `ProjectionPolicy` contract for deterministic
  documentation-oriented Java text and managed output files. The policy now defines visibility, named nesting,
  synthetic and Groovy-runtime artifacts, and mandatory signature closure. The provisional `AnnoDocGenerator` API is
  removed; see the [source-projection guide](docs/source-projection.md) and
  [0.x-to-1.0 migration](docs/migration/0.x-to-1.0-supported-api.md).

- Corrected inherited generic source projection so nested types retain their parameterized enclosing owner, including
  bounds and wildcards, and the resulting Java compiles without unresolved generic context.

- Stabilized the 1.0 transformation-authoring language around `Documentation` and `AstDocumentation`, including
  first-class code blocks, deterministic normalization, one-pass named templates, canonical links, and exact
  extraction/attachment. The final Java shape uses optional scalar accessors, explicit clearing, strict null handling,
  immutable value semantics, and a checked-in supported-API baseline. The accidental 0.x helper names are replaced; see the
  [authoring migration guide](docs/migration/0.x-to-1.0-authoring-language.md).

- Published Java-facing artifacts now declare stable automatic module names for JPMS consumers. The global-AST service
  provider is packaged with its service descriptor, preserving classpath discovery while enabling module-path use. The
  Gradle plugin remains build-tool-only and the annotation processor remains processor-path-oriented. See
  [the module-path migration notes](docs/migration/0.x-to-1.0-module-path.md).

## Historical releases

Releases before this changelog was adopted are identified by the repository's Git tags. Historical release-note backfill
must be evidence-based and is tracked separately; entries are not inferred from commit subjects alone.
