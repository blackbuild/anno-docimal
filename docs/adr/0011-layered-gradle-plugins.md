# Keep a base and opinionated Gradle plugin pair

Both published Gradle plugin IDs are supported and form an intentional base/opinionated pair. The base plugin registers
the conventional `createClassStubs` task and connects it to Javadoc; the opinionated plugin applies the base layer and
configures compiler prerequisites for documentation capture and parameter metadata. Independently registered projection
tasks require neither convention lifecycle. This follows Gradle's recommended composition approach, preserves the split
introduced for issue #28, and requires both layers to configure lazily and remain configuration-cache safe.
