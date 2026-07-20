# Changes

## Unreleased

- Added the supported cacheable `SourceProjectionTask` and the `com.blackbuild.annodocimal.groovy-plugin` convention.
  Independently registered tasks select top-level class files with declared patterns, project deterministic managed
  source mirrors, clean stale output, and support Gradle's build and configuration caches. Gradle 7.3.3 through 8.14.5
  is supported with TestKit coverage at both endpoints. `CreateClassStubs` and
  `com.blackbuild.annodocimal.plugin` are removed without shims; see [the usage guide](docs/usage.md) and
  [0.x-to-1.0 migration](docs/migration/0.x-to-1.0-supported-api.md).

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

- Corrected inherited generic source projection so inherited type variables are resolved through parameterized API
  relationships and nested types retain their enclosing owner, including bounds and wildcards. The resulting Java now
  compiles without unresolved generic context.

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
