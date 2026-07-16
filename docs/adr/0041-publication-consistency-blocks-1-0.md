# Verify every publication contract before 1.0

Publication consistency blocks AnnoDocimal 1.0. A clean local-repository smoke test must verify all six artifact
coordinates and both Gradle plugin markers, their POM and Gradle metadata where applicable, dependency scopes,
signatures, sources and Javadocs, shaded contents, plugin descriptors, and consumer resolution. The generator's current
shadow publication requires explicit attention because it emits no Gradle module metadata and its build does not
currently produce generator sources/Javadoc artifacts. Acceptance is based on complete, resolvable publication behavior,
not a prescribed Gradle publication implementation. GitHub issue #44 owns this gate.
