# Changes

## Unreleased

- Added a protected, immutable GitHub Pages publication seam for deterministic static HTML and all supported Java API
  Javadocs. Pull requests render and crawl a distinct non-release rehearsal without deployment; exact release manifests
  cover the deployed HTML, assets, and Javadoc bytes. Release authorization and recovery remain in the #45 runbook. See
  repository-owned versioned documentation.

- Added shared Java APT, local Groovy, and packaged global-AST capture conformance evidence across Groovy 3, 4, and 5.
  The shared declaration matrix covers documented keys, normalization, nested declarations, parameters, empty content,
  and carrier selection. Java APT now removes surrounding whitespace from documentation-properties values consistently
  with Groovy capture.

- Added runtime GroovyDoc interoperability across Groovy 3, 4, and 5. Exact extraction now selects non-blank carriers in
  deterministic `@AnnoDoc`, runtime `@Groovydoc`, documentation-properties order; local and global Groovy capture avoid
  duplicate `@AnnoDoc` emission when Groovy supplies its runtime carrier. Source projection normalizes and renders the
  selected carrier as Javadoc without copying carrier annotations. AnnoDocimal's protocol remains the canonical
  cross-language and future-evolution boundary; see the [usage guide](docs/user/usage.md#runtime-groovydoc-interoperability).

- Added an unsigned, network-independent publication audit for all six artifacts and both Gradle plugin markers. It
  verifies POM and Gradle metadata, sources and Javadocs, module and service packaging, shaded boundaries, signing and
  staging configuration, and clean Gradle and Apache Maven consumers. A separate exact-version RC task is ready for
  authorized validation from Maven Central and the Gradle Plugin Portal; see
  repository-owned publication validation.

- Added a recompiling Java and Groovy source-projection contract matrix across Groovy 3, 4, and 5. Representative
  declarations now verify deterministic, compilable output for classes, interfaces, annotations, enums, top-level and
  nested records, members, generic and wildcard signatures, arrays, declared exceptions, and documentation carriers.
  Record projections retain record kind and components, and generic declarations no longer lose non-generic declared
  exceptions.

- Normalized projected annotation members lexicographically by name, including primitive, enum, class, nested-annotation,
  and array-valued members. Repeated projection is byte-identical across Groovy 3, 4, and 5 without treating incidental
  source or bytecode declaration order as Java semantics.

- Added the supported cacheable `SourceProjectionTask` and the `com.blackbuild.annodocimal.groovy-plugin` convention.
  Independently registered tasks select top-level class files with declared patterns, project deterministic managed
  source mirrors, clean stale output, and support Gradle's build and configuration caches. Gradle 7.3.3 through 8.14.5
  is supported with TestKit coverage at both endpoints. `CreateClassStubs` and
  `com.blackbuild.annodocimal.plugin` are removed without shims; see [the usage guide](docs/user/usage.md) and
  [0.x-to-1.0 migration](docs/user/migration/0.x-to-1.0-supported-api.md).

- Rewrote the repository-owned landing page, usage guide, and migration guidance for the 1.0 documentation contract.
  They now describe the six artifacts, Java and Groovy capture, the supported authoring and projection APIs, automatic
  module names, Javadoc and IDE-mirror boundaries, and the conventional and independently configurable Gradle
  integrations. See
  [the usage guide](docs/user/usage.md) and [0.x-to-1.0 migration](docs/user/migration/0.x-to-1.0-supported-api.md).

- Defined the supported 1.0 Java API as non-null by default with JSpecify. Supported annotation, transformation-author,
  and source-projection types are `@NullMarked`, genuine raw-null inputs are explicit, and `Optional`, runtime null
  rejection, and template semantics are unchanged. See the [supported API](docs/user/supported-api.md) and
  [0.x-to-1.0 migration](docs/user/migration/0.x-to-1.0-supported-api.md).

- Added the supported `SourceProjector` and explicit `ProjectionPolicy` contract for deterministic
  documentation-oriented Java text and managed output files. The policy now defines visibility, named nesting,
  synthetic and Groovy-runtime artifacts, and mandatory signature closure. The provisional `AnnoDocGenerator` API is
  removed; see the [source-projection guide](docs/user/source-projection.md) and
  [0.x-to-1.0 migration](docs/user/migration/0.x-to-1.0-supported-api.md).

- Corrected inherited generic source projection so inherited type variables are resolved through parameterized API
  relationships and nested types retain their enclosing owner, including bounds and wildcards. The resulting Java now
  compiles without unresolved generic context.

- Stabilized the 1.0 transformation-authoring language around `Documentation` and `AstDocumentation`, including
  first-class code blocks, deterministic normalization, one-pass named templates, canonical links, and exact
  extraction/attachment. The final Java shape uses optional scalar accessors, explicit clearing, strict null handling,
  immutable value semantics, and a checked-in supported-API baseline. The accidental 0.x helper names are replaced; see the
  [authoring migration guide](docs/user/migration/0.x-to-1.0-authoring-language.md).

- Published Java-facing artifacts now declare stable automatic module names for JPMS consumers. The global-AST service
  provider is packaged with its service descriptor, preserving classpath discovery while enabling module-path use. The
  Gradle plugin remains build-tool-only and the annotation processor remains processor-path-oriented. See
  [the module-path migration notes](docs/user/migration/0.x-to-1.0-module-path.md).

## Historical releases

Releases before this changelog was adopted are identified by the repository's Git tags. Historical release-note backfill
must be evidence-based and is tracked separately; entries are not inferred from commit subjects alone.
