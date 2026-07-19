# Define supported API nullability with JSpecify

AnnoDocimal 1.0 uses JSpecify 1.0.0 for compiler-visible nullability on its supported Java API. Each supported top-level
type is `@NullMarked`; supported nested types inherit that null-marked scope from their enclosing supported type. The
annotation is deliberately type-scoped rather than package- or module-scoped because the AST, generator, and Gradle
plugin packages also contain public implementation-only types that are outside the compatibility contract.

`@Nullable` is used only where raw `null` is genuine. On the public surface this includes the conventional argument to
value-based `equals(Object)`. Nullable private state and package-private constructor inputs inside a null-marked supported
type are also annotated so the implementation accurately describes absent scalar state and exception diagnostics.
Supported accessors continue to represent absence with `Optional`; collection elements, map keys and values, facade
arguments, builder arguments, paths, documentation text, and template values remain non-null.

JSpecify metadata supplements rather than replaces behavior. Public methods continue to reject prohibited null inputs at
runtime, explicit clear/empty operations retain their existing meaning, and raw or dynamic template-map calls retain
their contextual validation. Groovy 3, 4, and 5 consumers can compile against the annotated types; dynamic Groovy callers
still rely on the runtime contract when their tooling does not interpret nullness metadata.

JSpecify is an API dependency of artifacts whose supported types carry its runtime-visible annotations. The generator
shadow JAR excludes JSpecify's classes and declares the dependency in its publication metadata so consuming the generator
together with another AnnoDocimal artifact does not create a split `org.jspecify.annotations` package on the module path.

The annotation types, transformation-author facade/model, and source-projection API are null-marked now. A supported
Gradle task type must adopt the same contract when issue #35 introduces it; APT and global-AST currently have empty Java
API allowlists. Implementation-only types remain unmarked. JetBrains `@Language` in test sources remains independent IDE
language-injection metadata and is not part of the supported nullability standard.

The scoped authoring and projection compatibility baselines capture `@NullMarked` declarations and public `@Nullable`
parameter contracts. Runtime null rejection, `Optional` absence, template behavior, Java consumer compilation, and
Groovy 3/4/5 interpretation remain separate behavioral evidence.
