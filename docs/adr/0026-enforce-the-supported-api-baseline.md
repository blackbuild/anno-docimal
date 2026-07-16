# Enforce the supported API baseline mechanically

Each published artifact receives a mechanical source/binary compatibility baseline for its explicitly declared
supported API. The check must not treat every public implementation type as supported accidentally. Compatibility
exceptions require an intentional, release-facing rationale.

Before the 1.0 release, a dedicated API grilling session is mandatory. It must classify every published public type as
supported, provisional with an explicit disposition, or implementation-only; finalize the per-artifact allowlist; and
establish the initial mechanical baseline. This architecture session schedules that release gate but does not perform
the final classification. GitHub issue #37 owns the dedicated grilling.
