# Coding style

Follow the conventions already established in the surrounding module. These rules record recurring review expectations
that are not enforced by formatting tools.

## Imports and qualified names

- Import referenced Java and Groovy types and use their simple names in declarations, method bodies, annotations, class
  literals, generic arguments, and method references.
- Fully qualify a type only for a real simple-name conflict or a documented generated-source, template, or compiler
  constraint. Keep the qualification local to the ambiguous use.
- Generated projections are governed by the generator. Handwritten fixtures and source templates should still declare
  imports when their format permits it.

## Compatibility-sensitive code

- Treat Groovy compiler APIs, AST behavior, bytecode layout, ASM parsing, JavaPoet rendering, Gradle task inputs, and
  service registration as compatibility seams.
- Keep version-specific behavior explicit and cover it in the affected Groovy lane rather than relying on incidental
  behavior of the baseline compiler.
- Preserve deterministic generated output. If order is not part of the semantic input, normalize it before rendering.
