# Source projection

AnnoDocimal reconstructs documentation-oriented Java declarations from compiled JVM classes. A projection is a class
stub or IDE source mirror: it preserves selected declaration meaning and documentation, but it is not original source or
a decompilation.

## Project one top-level class

`SourceProjector` is final, thread-safe, and reusable. Construct it with one immutable policy:

```java
SourceProjector projector = new SourceProjector(ProjectionPolicy.documentation());

String source = projector.projectToText(classFile);
Path sourceFile = projector.projectToDirectory(classFile, outputDirectory);
```

Both operations project exactly one caller-selected top-level `.class` file. A direct nested, local, or anonymous class
file is rejected as a projection root. Directory scanning, top-level class-file filtering, and stale-output cleanup stay
with the caller or Gradle task.

`projectToText` returns deterministic Java text with LF endings. `projectToDirectory` writes the identical UTF-8 text to
the package/type-relative `.java` path beneath the supplied output directory. It creates parent directories and replaces
only that managed file, atomically when the file system supports atomic moves. It does not clean sibling output.

Annotation members are projected in lexicographic member-name order at every nesting level. This normalization applies
equally to primitive, enum, class, nested-annotation, and array-valued members; array elements retain their declared
sequence. The member-name rule is a source-projection determinism guarantee, not a claim that Java annotation semantics
assign an order to members, and it deliberately does not preserve incidental source, compiler, or bytecode order.

## Documentation inclusion policy

`ProjectionPolicy.documentation()` has the stable documentation-oriented defaults:

| Concern | Default and invariant |
|---|---|
| Projection root | Always included. The visibility selection does not remove it. |
| Visibility | Public and protected members are included. Package-private and private members are excluded unless signature closure requires them. |
| Nested declarations | Named member types are included recursively when their visibility is selected. Local and anonymous classes are excluded. |
| Synthetic declarations | Excluded. Enabling them cannot disable valid-source checks; conflicting bridge declarations fail projection. |
| Groovy runtime artifacts | Groovy runtime interfaces, metadata fields/accessors, and similarly named scaffolding are excluded. Visible language-level APIs such as generated property accessors remain included. |
| Signature closure | Always enabled. It includes otherwise-excluded named member types and their enclosing chain when selected signatures or annotations refer to them. External dependency declarations are never copied. |

Use `ProjectionPolicy.builder()` or `policy.toBuilder()` to replace the included `DeclarationVisibility` values or toggle
named nested, synthetic, and Groovy-runtime inclusion. Builders are mutable and not thread-safe; every `build()` call
returns an independent immutable value. Signature closure and valid Java output are not optional policy switches.

Groovy runtime classification uses compiler metadata and the owning declaration's role. A declaration name alone never
turns a user-authored Groovy member into runtime scaffolding.

Source projection preserves declaration kind and nesting, visibility and modifiers, generic and inheritance signatures,
selected constructors/methods/fields, parameter metadata, annotations, exceptions, and embedded documentation. It does
not promise original bodies or initializers: deterministic default statements and values are synthesized where Java
requires them for a valid stub. Synthetic implementation details under the documentation preset, original whitespace,
and import layout are likewise not preserved.

Supported Java records retain their record kind, component list, generic bounds, implemented interfaces, and named
nested declarations. When the inclusion policy selects a canonical constructor, the projection emits deterministic
component assignments; otherwise the record declaration relies on Java's implicit canonical constructor. Both forms
remain compilable without reconstructing the original constructor implementation.

Generic signatures retain the declaration context needed to resolve every type variable. In particular, a nested member
type reached through a parameterized enclosing type keeps that owner qualification in method, field, superclass, and
interface signatures, including inherited signatures with bounds and wildcards. When implementation bytecode retains a
type variable declared by an inherited API, projection resolves it through the parameterized superclass or interface
path before rendering the implementation signature.

## Failures

Ordinary class-input and managed-output file-system failures are `IOException`. A readable class whose selected
declaration cannot be represented as valid Java throws `SourceProjectionException`; the exception retains the input path
and exposes a stable declaration identifier when one is available.

## Supported API boundary and migration

The supported generator API contains only `SourceProjector`, `ProjectionPolicy`, `ProjectionPolicy.Builder`,
`DeclarationVisibility`, and `SourceProjectionException`. ASM, JavaPoet, signature parsers, visitors, converters,
annotation readers, and shaded types are implementation details and are excluded from the compatibility baseline.

Those supported types are JSpecify `@NullMarked`. Paths, policies, and selected visibility values are non-null; absent
declaration identifiers remain non-null `Optional` results. Runtime null rejection remains in place for dynamic Groovy
and other callers whose tooling does not interpret JSpecify metadata. The published JSpecify dependency stays external
to the generator shadow JAR so module-path consumers do not receive a duplicate annotation package.

The provisional 0.x `AnnoDocGenerator` helper is removed without a compatibility shim. See the
[0.x-to-1.0 supported-API migration](migration/0.x-to-1.0-supported-api.md) for the direct replacement. The reusable
Gradle task contract, including collection filtering and stale-output cleanup, remains owned by issue #35.
