# Define Groovy property documentation before 1.0

Issue #9 blocks AnnoDocimal 1.0. Groovy properties are a core supported declaration shape, so 1.0 must define and verify
how property documentation maps to the property, backing field, and generated accessors. Capture must use Groovy's
semantic property relationship rather than projection-time JavaBeans-name inference. The implementation may use the
current normalized textual carrier and must leave room for issue #30's later structured protocol.
