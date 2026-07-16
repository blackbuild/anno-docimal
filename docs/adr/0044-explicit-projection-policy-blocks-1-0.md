# Define the supported projection policy before 1.0

An explicit source-projection inclusion policy blocks AnnoDocimal 1.0. The supported projection API must define how it
selects visibility, nested declarations, synthetic members, Groovy-generated artifacts, and declarations required to
represent selected signatures. Top-level class-file input selection remains the responsibility of the calling build or
Gradle task. Existing `AnnoDocGenerator` entry points may retain legacy behavior through a documented migration rather
than silently changing. GitHub issue #39 owns the projection API and policy implementation.
