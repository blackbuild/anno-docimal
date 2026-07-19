# Changes

## Unreleased

- Added the supported `SourceProjector` and explicit `ProjectionPolicy` contract for deterministic
  documentation-oriented Java text and managed output files. The policy now defines visibility, named nesting,
  synthetic and Groovy-runtime artifacts, and mandatory signature closure. The provisional `AnnoDocGenerator` API is
  removed; see the [source-projection guide](docs/source-projection.md) and
  [0.x-to-1.0 migration](docs/migration/0.x-to-1.0-supported-api.md).

- Stabilized the 1.0 transformation-authoring language around `Documentation` and `AstDocumentation`, including
  first-class code blocks, deterministic normalization, one-pass named templates, canonical links, and exact
  extraction/attachment. The accidental 0.x helper names are replaced; see the
  [authoring migration guide](docs/migration/0.x-to-1.0-authoring-language.md).

- Published Java-facing artifacts now declare stable automatic module names for JPMS consumers. The global-AST service
  provider is packaged with its service descriptor, preserving classpath discovery while enabling module-path use. The
  Gradle plugin remains build-tool-only and the annotation processor remains processor-path-oriented. See
  [the module-path migration notes](docs/migration/0.x-to-1.0-module-path.md).

## Historical releases

Releases before this changelog was adopted are identified by the repository's Git tags. Historical release-note backfill
must be evidence-based and is tracked separately; entries are not inferred from commit subjects alone.
