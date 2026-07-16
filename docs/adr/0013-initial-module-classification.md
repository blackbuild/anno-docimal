# Start with stable automatic module identities

The initial issue #36 target assigns stable automatic module names to the five non-Gradle artifacts:
`com.blackbuild.annodocimal.annotations`, `com.blackbuild.annodocimal.apt`,
`com.blackbuild.annodocimal.ast`, `com.blackbuild.annodocimal.global.ast`, and
`com.blackbuild.annodocimal.generator`. The APT artifact is processor-path tooling rather than an application-module
dependency. `anno-docimal-gradle-plugin` is build-tool-only and intentionally outside JPMS. Explicit descriptors may be
reconsidered after package and API cleanup through a major, compatibility-tested design change.
