# Make Gradle task correctness a 1.0 release gate

Issue #35 blocks AnnoDocimal 1.0. Before the published Gradle task and plugin contracts are stabilized, the source-
projection task must have reusable declared inputs and outputs, documentation-sensitive invalidation, deterministic
stale-output cleanup, correct up-to-date and build-cache behavior, and configuration-cache reuse. TestKit must verify
these behaviors across the supported Gradle range. This architecture session records the gate and acceptance boundary;
implementation remains in issue #35.
