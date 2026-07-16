# Support independently registered source-projection tasks

The class-to-source Gradle task type is a first-class supported API that consumers can register independently of
AnnoDocimal's default Javadoc wiring. Its contract includes arbitrary class inputs and output directory, declared
top-level filtering, documentation-sensitive inputs, deterministic stale-output cleanup, correct up-to-date and
build-cache behavior, and configuration-cache-safe execution. The base plugin's `createClassStubs` to `javadoc` lifecycle
is a convenience instance; IDE model wiring remains consumer-specific. Issue #35 owns implementation and executable
acceptance, which are outside this baseline session.
