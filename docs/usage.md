# Using AnnoDocimal

AnnoDocimal transports normalized documentation from compilation to generated JVM APIs and source-oriented tools. It is
not a DSL framework and it does not own IDE model configuration in consuming builds.

## Compatibility and coordinates

AnnoDocimal supports Java 17 and one artifact set across Groovy 3, 4, and 5. The version catalog records the tested
Groovy baselines; it does not restrict consumers to those exact patch releases. The Gradle integration supports Gradle
7.3.3 through the current 8.14.5 wrapper baseline, with TestKit coverage at both endpoints.

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

### Runtime GroovyDoc interoperability

AnnoDocimal accepts Groovy's runtime `groovy.lang.Groovydoc` annotation as an interoperable carrier across Groovy 3, 4,
and 5. It does not replace AnnoDocimal's protocol: `@AnnoDoc` and documentation properties remain the canonical
cross-language representation and the boundary for future protocol evolution.

Exact extraction uses deterministic precedence: a non-blank `@AnnoDoc` value wins, followed by a non-blank runtime
`@Groovydoc` value, followed by the declaration's documentation-properties entry. Runtime GroovyDoc values are
normalized on read: `/** */` delimiters, Groovy's `/**@` runtime marker, leading `*` decoration, shared indentation, and
surrounding blank space are not part of the resulting documentation content.

Groovy can generate the runtime carrier from `/**@ ... */` comments when its `runtimeGroovydoc` optimization is enabled.
If AnnoDocimal local or global capture sees that carrier, it does not emit a duplicate `@AnnoDoc` annotation. Builds that
want both compiler-model Groovydoc and runtime GroovyDoc can enable both options explicitly:

```groovy
tasks.withType(GroovyCompile).configureEach {
    groovyOptions.optimizationOptions.groovydoc = true
    groovyOptions.optimizationOptions.runtimeGroovydoc = true
}
```

Source projection applies the same annotation-carrier precedence, renders the selected normalized content as Javadoc,
and omits both carrier annotations from the projected Java. Documentation properties remain an extraction carrier for
resolved compiler-model classes; a projector given only a class file does not discover adjacent properties resources.

## AST-transformation-author API

The AST-transformation-author API is for developers writing Groovy AST transformations that generate classes or members
and want those generated APIs to carry useful documentation. `AstDocumentation` and immutable `Documentation` values
are its supported surface: read exact documentation from a source or helper declaration, adapt it to the generated
member, and attach the result without depending on AnnoDocimal's extractors, metadata, or annotations directly.

For example, a KlumAST-style transformation can delegate a generated method to a Java helper. The helper keeps its
templated Javadoc beside its implementation, and Java APT preserves it during compilation:

```java
import com.blackbuild.annodocimal.annotations.InlineJavadocs;

@InlineJavadocs
final class WidgetHelper {
    /**
     * Creates a {{kind}} from the supplied source.
     *
     * @param source source data
     */
    static Widget create(String source) {
        // Helper implementation omitted.
        return null;
    }
}
```

When the transformation creates a method that delegates to `WidgetHelper.create`, it reads the helper method's exact
documentation from its compiler model and attaches a template-specialized copy to the generated method:

```java
import com.blackbuild.annodocimal.ast.AstDocumentation;
import com.blackbuild.annodocimal.ast.Documentation;

import org.codehaus.groovy.ast.MethodNode;

MethodNode helperMethod = /* resolved WidgetHelper.create compiler-model method */;
MethodNode generatedMethod = /* newly created delegation method */;

Documentation helperDocumentation = AstDocumentation.extractExact(helperMethod)
        .orElse(Documentation.empty());
Documentation generatedDocumentation = helperDocumentation.toBuilder()
        .template("kind", "widget")
        .build();
AstDocumentation.attach(generatedMethod, generatedDocumentation);
```

`extractExact` hides the capture carrier. It applies the documented canonical/runtime/properties precedence without
requiring transformations to inspect any representation directly. The transformation can further use `summary`,
`paragraph`, `codeBlock`, `filterParameters`, and
`AstDocumentation.referenceTo` before attachment.

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

The neutral `com.blackbuild.annodocimal.base-plugin` applies neither Java nor Groovy. When a Java model is present, it
registers the conventional `createClassStubs` `SourceProjectionTask` over all main class directories and makes
`javadoc` consume its output. This is a Javadoc convenience, not an IDE source-set registration.

For an independently managed IDE-only source mirror, register `SourceProjectionTask` directly. The task owns its
output directory, so the mirror must not be compiled, packaged, or published as a second API:

```groovy
import com.blackbuild.annodocimal.plugin.SourceProjectionTask

plugins {
    id 'java'
    id 'com.blackbuild.annodocimal.base-plugin'
}

tasks.register('dslSourceMirror', SourceProjectionTask) {
    classesDirectories.from(sourceSets.main.output.classesDirs)
    includes.add('**/*_DSL.class')
    excludes.add('**/internal/**')
    outputDirectory.set(layout.buildDirectory.dir('source-mirrors/dsl'))
}
```

`classesDirectories` is documentation-sensitive classpath input. `includes` and `excludes` are declared Ant-style
patterns over slash-normalized paths relative to every input directory, including `.class`; exclusions win. The default
includes all top-level class files. The task rejects duplicate binary names from different inputs and input/output
overlap, projects to a staging tree, and replaces the managed output only after every projection succeeds. It is
cacheable and configuration-cache safe. `projectionPolicy` defaults to `ProjectionPolicy.documentation()` and can be
set to another immutable policy value when the consuming build needs a broader documented projection.

The opinionated `com.blackbuild.annodocimal.groovy-plugin` applies Gradle's Groovy plugin and the neutral base plugin,
then configures Groovy and Java compilation to retain documentation and parameter metadata. Plugin implementation
classes and task actions remain implementation details; `SourceProjectionTask` is the supported Gradle Java API.

## Supported versus implementation-only APIs

The supported surface is deliberately smaller than the set of `public` classes in published JARs. In particular,
`AstDocumentation`/`Documentation` form the authoring facade and `SourceProjector`/`ProjectionPolicy` form the
projection facade. Processors and global providers are supported behavioral integrations with empty Java API
allowlists. Visitors, extractors, parser and Groovy-version helpers, metadata caches, ASM, JavaPoet, shaded classes,
plugin implementation classes, and task actions are implementation-only. The complete allowlists and nullability
contract are recorded in [the supported API document](api/1.0-supported-api.md).
