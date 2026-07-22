---
status: superseded by ADR-0060
---

# Use release tags and Maven artifacts as current versioned documentation

Until shared cross-project documentation infrastructure exists, each Git release tag is the immutable repository
documentation snapshot for that version and the default branch documents the next release. Versioned source and Javadoc
artifacts published to Maven Central provide the matching artifact-level references. Documentation should use
repository-relative links where practical, and AnnoDocimal will not build a bespoke hosting or version-selector system.
A future shared system may initially use the common `com.blackbuild` namespace across AnnoDocimal and Klum projects, but
the resulting reduction in visible Klum branding is a cross-project tradeoff, not a decision of this session.
