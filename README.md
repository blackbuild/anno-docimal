# AnnoDocimal

AnnoDocimal is an independent documentation transport and source-projection library for generated JVM APIs. It captures
documentation during Java or Groovy compilation, carries it in compiled output, and reconstructs documentation-oriented
Java source for the standard Javadoc tool or an IDE source mirror. KlumAST is an important consumer, not the product
boundary.

AnnoDocimal 1.0 supports Java 17 and one artifact set across Groovy 3, 4, and 5. The Gradle integration supports
Gradle 7.3.3 through the current 8.14.5 wrapper baseline; both endpoints have TestKit coverage.

Groovy's runtime `Groovydoc` annotation is accepted as an interoperable input carrier. AnnoDocimal's `AnnoDoc` and
documentation-properties protocol remains the canonical cross-language and evolution boundary; carrier precedence,
normalization, and duplicate-avoidance behavior are documented in the [usage guide](docs/usage.md#runtime-groovydoc-interoperability).

## Quick start: transformation authors

Use the annotations artifact wherever compiled declarations must carry documentation, and the AST artifact in the
compile-time transformation that creates those declarations:

```groovy
dependencies {
    api "com.blackbuild.annodocimal:anno-docimal-annotations:<version>"
    compileOnly "com.blackbuild.annodocimal:anno-docimal-ast:<version>"
}
```

Attach documentation through the supported facade, rather than through the older visitor, extractor, or formatting
helpers:

```groovy
import com.blackbuild.annodocimal.ast.AstDocumentation

def generatedMethod = classNode.addMethod(/* generated method details */)
AstDocumentation.attachText(generatedMethod, 'Creates the generated value.')
```

For composition, templates, links, exact extraction, and the supported API boundary, see the
[authoring guide](docs/usage.md#ast-transformation-author-api) and
[supported API record](docs/api/1.0-supported-api.md).

## Quick start: source projection

Use `anno-docimal-generator` when a build owns its class-file selection and needs source text or a managed output file:

```java
import com.blackbuild.annodocimal.generator.ProjectionPolicy;
import com.blackbuild.annodocimal.generator.SourceProjector;

import java.nio.file.Path;

SourceProjector projector = new SourceProjector(ProjectionPolicy.documentation());
Path sourceFile = projector.projectToDirectory(classFile, outputDirectory);
```

The projector is documentation-oriented declaration reconstruction, not decompilation. It preserves selected API shape
and documentation, but not original bodies, initializers, whitespace, or imports. The
[source-projection guide](docs/source-projection.md) specifies the policy and fidelity contract.

## Documentation map

- [Artifact, capture, plugin, Javadoc, and IDE integration guide](docs/usage.md)
- [Supported API and implementation-only boundary](docs/api/1.0-supported-api.md)
- [Source-projection policy and fidelity](docs/source-projection.md)
- [Publication validation](docs/publication-validation.md)
- [Versioned documentation and Javadocs](docs/versioned-documentation.md)
- [0.x-to-1.0 migration](docs/migration/0.x-to-1.0-supported-api.md)
- [Module-path migration](docs/migration/0.x-to-1.0-module-path.md)
- [Change history](CHANGES.md)

## Java modules

The Java-facing artifacts have stable automatic module names: `com.blackbuild.annodocimal.annotations`,
`com.blackbuild.annodocimal.apt`, `com.blackbuild.annodocimal.ast`, `com.blackbuild.annodocimal.global.ast`, and
`com.blackbuild.annodocimal.generator`. The Gradle plugin is build-tool-only and intentionally has no JPMS identity.
See the [module-path migration notes](docs/migration/0.x-to-1.0-module-path.md) for named consumers.
