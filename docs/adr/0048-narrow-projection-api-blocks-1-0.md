# Deliver the narrow source-projection API for 1.0

The narrow, implementation-neutral source-projection service blocks AnnoDocimal 1.0. It must support projection to
source text and managed output locations, consume the explicit projection policy, and keep ASM, JavaPoet, parser,
visitor, and shaded implementation types outside the supported contract. The pre-1.0 `AnnoDocGenerator` migration
strategy is a separate decision: this gate does not itself require compatibility shims.
GitHub issue #39 owns the service and policy implementation after issue #37 confirms the API.
