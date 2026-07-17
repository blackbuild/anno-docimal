# Changes

## Unreleased

- Published Java-facing artifacts now declare stable automatic module names for JPMS consumers. The global-AST service
  provider is packaged with its service descriptor, preserving classpath discovery while enabling module-path use. The
  Gradle plugin remains build-tool-only and the annotation processor remains processor-path-oriented. See
  [the module-path migration notes](docs/migration/0.x-to-1.0-module-path.md).

## Historical releases

Releases before this changelog was adopted are identified by the repository's Git tags. Historical release-note backfill
must be evidence-based and is tracked separately; entries are not inferred from commit subjects alone.
