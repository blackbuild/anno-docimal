# Using AnnoDocimal

AnnoDocimal transports normalized documentation from compilation to generated JVM APIs and source-oriented tools. It is
not a DSL framework and it does not own IDE model configuration in consuming builds.

## Compatibility and coordinates

AnnoDocimal supports Java 17 and one artifact set across Groovy 3, 4, and 5. The version catalog records the tested
Groovy baselines; it does not restrict consumers to those exact patch releases. The Gradle wrapper is the current
development baseline. The supported minimum Gradle version and reusable task contract are not yet published: issue
[#35](https://github.com/blackbuild/anno-docimal/issues/35) owns their TestKit evidence and stabilization.

All artifacts use group `com.blackbuild.annodocimal`:

| Artifact | Use it when | Runtime or build role | Automatic module |
|---|---|---|---|
| `anno-docimal-annotations` | declarations or consumers need `@AnnoDoc` / `@InlineJavadocs` | Runtime protocol dependency | `com.blackbuild.annodocimal.annotations` |
| `anno-docimal-apt` | Java source documentation must survive compilation | annotation-processor path tooling | `com.blackbuild.annodocimal.apt` |
| `anno-docimal-ast` | a Groovy transformation captures or writes documentation | compile-time transformation dependency | `com.blackbuild.annodocimal.ast` |
| `anno-docimal-global-ast` | every Groovy source in a compilation should be captured through service discovery | compile-time transformation dependency | `com.blackbuild.annodocimal.global.ast` |
| `anno-docimal-generator` | class files must become documentation-oriented Java source | build tooling | `com.blackbuild.annodocimal.generator` |
| `anno-docimal-gradle-plugin` | a Gradle build wants the conventional projection/Javadoc integration | build plugin | — |

The annotation processor is processor-path tooling, not a normal application-module dependency. The Gradle plugin is
not intended for an application module path. Named consumers should use the stable module names rather than deriving
names from JAR files; see [the module-path migration](migration/0.x-to-1.0-module-path.md).

## Capture documentation

### Java annotation processing

Add annotations to the source and put the processor on the annotation-processor path. The processor is service
discovered; consumers do not construct its implementation class.

```groovy
dependencies {
    implementation "com.blackbuild.annodocimal:anno-docimal-annotations:<version>"
    annotationProcessor "com.blackbuild.annodocimal:anno-docimal-apt:<version>"
}
```

```java
import com.blackbuild.annodocimal.annotations.InlineJavadocs;

/** Documents the API that a later transformation reads. */
@InlineJavadocs
public class SourceType {
    /** Performs the documented action. */
    public void action() {
    }
}
```

The processor writes documentation properties to the class output. Their `__annodoc.properties` suffix and class,
method, and field key semantics are protocol behavior. They are read by AnnoDocimal extraction; they are not generated
Javadoc files.

### Local Groovy capture

For an explicitly selected package or type, use `@InlineJavadocs` and place `anno-docimal-ast` on the Groovy
compilation classpath:

```groovy
dependencies {
    compileOnly "com.blackbuild.annodocimal:anno-docimal-ast:<version>"
}
```

```groovy
import com.blackbuild.annodocimal.annotations.InlineJavadocs

@InlineJavadocs
class LocalCapture {
    /** This source documentation becomes embedded documentation. */
    String name
}
```

The marker activates local capture; the transformation, visitor, parser, metadata, and Groovy-version adapter classes
are implementation-only. `@InlineJavadocs` is source-retained, while `@AnnoDoc` is the runtime documentation carrier.

### Global Groovy capture

To enable capture by Groovy global-AST service discovery, add the global artifact to the compilation that processes
Groovy source:

```groovy
dependencies {
    compileOnly "com.blackbuild.annodocimal:anno-docimal-global-ast:<version>"
}
```

The global artifact packages both the provider and its service descriptor, and delegates to the reusable local
transformation. Its provider class name is a packaging detail, not a consumer Java API. Do not combine it with an
assumption that documentation will be inherited: capture stores exact documentation; a separately named resolved
documentation capability remains owned by issue [#10](https://github.com/blackbuild/anno-docimal/issues/10).

## Transformation-author API

`AstDocumentation` and immutable `Documentation` values are the supported transformation-author surface. Extract exact
documentation, compose it, and attach it to a generated declaration:

```java
Documentation captured = AstDocumentation.extractExact(source).orElse(Documentation.empty());
Documentation generated = captured.toBuilder()
        .summary("Creates {{kind}} documentation.")
        .template("kind", "builder")
        .paragraph("The generated builder preserves model semantics.")
        .codeBlock("owner.copyBuilder(source)")
        .filterParameters(List.of("source"))
        .see(AstDocumentation.referenceTo(source))
        .build();
AstDocumentation.attach(target, generated);
```

`extractExact` does not search supertypes. `attach` replaces AnnoDocimal's carrier, keeps third-party carriers, filters
parameter descriptions to the target signature, and removes AnnoDocimal documentation for an empty value. Values are
immutable and builders are mutable, non-thread-safe snapshots. Public inputs reject `null`; optional scalar accessors
use `Optional`, and JSpecify marks the supported Java types non-null by default. See the
[authoring migration guide](migration/0.x-to-1.0-authoring-language.md) for template and clean-cut details.

## Source projection, Javadoc, and IDE mirrors

`SourceProjector` projects one caller-selected top-level class file to deterministic Java source or one managed
package-relative output file. `ProjectionPolicy.documentation()` includes public/protected declarations and named nested
types, excludes synthetic and Groovy runtime scaffolding, retains visible language-level generated APIs, and always
applies signature closure. It is not a decompiler. The full fidelity, inclusion, and failure policy is in
[source-projection.md](source-projection.md).

The current base Gradle plugin ID is `com.blackbuild.annodocimal.base-plugin`. In a Java-model project it registers the
conventional `createClassStubs` task and makes `javadoc` use its output. This is a Javadoc convenience, not an IDE
source-set registration. An IDE-only source mirror must be configured by the consuming build and must not be compiled,
packaged, or published as a second API.

The current opinionated plugin ID is `com.blackbuild.annodocimal.plugin`. It applies the base implementation and adds
Groovy/Javac compiler options intended to retain documentation and parameter metadata. Both current IDs, their task
implementation type, and plugin implementation classes are transitional behavior rather than the supported 1.0 Gradle
Java API. Issue [#35](https://github.com/blackbuild/anno-docimal/issues/35) owns the future `SourceProjectionTask`,
the `com.blackbuild.annodocimal.groovy-plugin` ID, independently registered task contract, managed-tree cleanup,
configuration-cache, and Gradle-range evidence. Do not use that future ID until it is delivered.

## Supported versus implementation-only APIs

The supported surface is deliberately smaller than the set of `public` classes in published JARs. In particular,
`AstDocumentation`/`Documentation` form the authoring facade and `SourceProjector`/`ProjectionPolicy` form the
projection facade. Processors and global providers are supported behavioral integrations with empty Java API
allowlists. Visitors, extractors, parser and Groovy-version helpers, metadata caches, ASM, JavaPoet, shaded classes,
plugin implementation classes, and task actions are implementation-only. The complete allowlists and nullability
contract are recorded in [the supported API document](api/1.0-supported-api.md).
