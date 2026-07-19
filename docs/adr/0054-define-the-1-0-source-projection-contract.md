# Define the 1.0 source-projection contract

`anno-docimal-generator` supports a final, thread-safe, reusable `SourceProjector` facade constructed with one non-null
`ProjectionPolicy`. `projectToText(Path)` projects one caller-selected top-level class file to deterministic Java source.
`projectToDirectory(Path, Path)` writes the identical text as UTF-8 with LF endings to the package/type-relative `.java`
path beneath a caller-selected directory, creates required directories, replaces only that managed file atomically when
the file system supports it, and returns the written path. Collection selection and stale-file cleanup belong to callers
and tasks.

Projection is documentation-oriented declaration reconstruction, not decompilation. The root declaration is always
selected. `ProjectionPolicy.documentation()` includes public and protected declarations, recursively includes named
member types, excludes synthetic declarations and Groovy runtime scaffolding, and includes visible language-level
generated APIs. Local and anonymous classes are excluded. The policy is implementation-neutral: it does not expose ASM,
Groovy-version adapters, visitors, JavaPoet, or bytecode flags.

Signature closure is mandatory for every policy. It includes the minimum otherwise-excluded named member declarations
and enclosing chain required to express selected signatures, including inherited generic substitutions. It never copies
external dependency declarations. If one selected declaration cannot be represented validly, projection fails for that
declaration instead of silently omitting it or erasing its types.

`ProjectionPolicy` is a final immutable, thread-safe value with value equality, `documentation()`, `builder()`, and
`toBuilder()`. `builder()` starts from the documentation preset; its nested builder is mutable and non-thread-safe and
produces independent snapshots. The supported controls are included `DeclarationVisibility` values, nested-declaration
inclusion, synthetic-declaration inclusion, and Groovy-runtime-artifact inclusion. Signature closure and valid output
cannot be disabled. Annotation-presence filters are a future additive capability, not part of the 1.0 baseline.

`DeclarationVisibility` is the only public projection enum and contains `PUBLIC`, `PROTECTED`, `PACKAGE_PRIVATE`, and
`PRIVATE`. `SourceProjectionException` is a supported runtime exception for a readable class whose selected declaration
cannot be projected. It identifies the input path and, when available, a stable declaration identifier while preserving
the cause. Ordinary file-system failures remain `IOException`.

Top-level selection is semantic. A direct projection root must be a top-level declaration according to class metadata;
named nested declarations are discovered from that root. Issue #39 may document a filename-based limitation only if
metadata classification proves disproportionately complex, and any such limitation remains a visible open flank rather
than redefining top-level selection.

The 1.0 generator allowlist is `SourceProjector`, `ProjectionPolicy`, `ProjectionPolicy.Builder`,
`DeclarationVisibility`, and `SourceProjectionException`. `AnnoDocGenerator` is removed without a shim. Current visitors,
converters, filters, annotation readers, ASM/JavaPoet types, and every relocated shaded type are implementation-only.
