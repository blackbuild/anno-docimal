# Expose a narrow source-projection API

`anno-docimal-generator` exposes a narrow, implementation-neutral service that projects compiled classes to Java text or
an output location. Consumers depend on its documented projection fidelity, not its ASM visitors, JavaPoet models,
signature parsers, type converters, or shaded dependencies. Those implementation types remain outside the supported API,
preserving freedom for issue #25 or another properly versioned implementation-technology change. This baseline does not
redesign the current `AnnoDocGenerator` entry points.
