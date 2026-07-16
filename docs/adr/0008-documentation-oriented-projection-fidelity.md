# Promise documentation-oriented projection fidelity

Source projection produces a deterministic, Javadoc-consumable and IDE-parseable declaration mirror rather than original
source or a decompilation. The contract preserves the selected declaration shape, including type kind and nesting,
visibility and modifiers, generics and inheritance, members, parameter metadata, annotations, exceptions, and
documentation. It does not preserve bodies, initializers, synthetic implementation details, whitespace, or import layout.
Accordingly, issue #32 is a fidelity defect and issue #33 is a determinism defect; textual formatting remains free to
evolve unless separately documented.
