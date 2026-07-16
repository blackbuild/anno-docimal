# Keep a generator CLI out of the 1.0 gate

Issue #23 does not block AnnoDocimal 1.0. The narrow generator API and reusable Gradle task are the supported integration
surfaces for 1.0. An executable distribution remains a post-1.0 enhancement and should proceed only with a concrete CLI
contract covering inputs, selection policy, output cleanup, diagnostics, exit codes, and distribution rather than merely
adding a main-class manifest.
