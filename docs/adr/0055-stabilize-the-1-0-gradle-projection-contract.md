# Stabilize the 1.0 Gradle projection contract

The supported reusable task type is `com.blackbuild.annodocimal.plugin.SourceProjectionTask`. It replaces
`CreateClassStubs` without a shim. Its supported member allowlist is limited to the declarative getters for:

- `@Classpath ConfigurableFileCollection classesDirectories`;
- `@Input SetProperty<String> includes` and `excludes`;
- `@Nested Property<ProjectionPolicy> projectionPolicy`; and
- `@OutputDirectory DirectoryProperty outputDirectory`.

There are no supported fluent configuration helpers, public task-action method, or subclassing SPI. The default selects
all top-level class files and uses `ProjectionPolicy.documentation()`. Include and exclude values are Gradle/Ant patterns
over slash-normalized paths relative to each classes directory, including the `.class` suffix; exclusions win. Matching
creates candidates and class metadata then determines top-level status. Duplicate binary names from different input
directories fail deterministically with both origins.

The task exclusively owns its output directory. It projects the complete selection into staging and replaces the managed
tree only after all projections succeed. A successful execution therefore removes stale files; a failed execution leaves
the previous successful output intact. Input and output directories must not overlap. The task remains cacheable,
documentation-sensitive, configuration-cache safe, and independently registrable; IDE model wiring remains consumer
policy.

The supported neutral plugin ID remains `com.blackbuild.annodocimal.base-plugin`. It applies neither Java nor Groovy. When
Gradle's Java model appears it lazily registers the conventional `createClassStubs` instance over all main class
directories and connects its output to `javadoc`. Without that model it registers and wires nothing, while consumers can
still register `SourceProjectionTask` manually.

The supported opinionated plugin ID is `com.blackbuild.annodocimal.groovy-plugin`. It replaces the ambiguous
`com.blackbuild.annodocimal.plugin` ID without a shim, applies Gradle's Groovy plugin and the AnnoDocimal base plugin, and
configures relevant Groovy and Java compilation tasks to retain documentation and parameter metadata. Both layers are
lazy and configuration-cache safe. The plugin implementation classes are packaging details, not supported Java APIs.

KlumAST's Groovy/Spock version convention may later move into an independent project that this plugin can mention or
compose with. AnnoDocimal 1.0 neither depends on that future project nor absorbs its version-management role. A future
Java-oriented convention plugin is likewise additive and outside 1.0.
