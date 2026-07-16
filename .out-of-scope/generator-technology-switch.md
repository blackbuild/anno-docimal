# Generator technology switches without a behavioral goal

AnnoDocimal does not switch its source-projection implementation solely to adopt a named ASM, JavaPoet, or Palantir
JavaPoet variant. Projection technology is internal; the product contract is valid, deterministic, documentation-oriented
Java source with a narrow implementation-neutral API.

## Why this is out of scope

A technology-only switch creates migration and publication risk without defining an observable improvement. A future
issue is welcome when it demonstrates a concrete fidelity defect, compatibility requirement, maintainability problem, or
dependency constraint and supplies behavioral acceptance criteria. The implementation may then change if that is the
proportionate solution.

## Prior requests

- #25 — switch to palantir/javapoet
