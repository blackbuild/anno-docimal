# Verify capture-path conformance before 1.0

A capture-conformance suite blocks AnnoDocimal 1.0. Shared fixtures must verify that Java annotation processing, local
Groovy capture, and global Groovy capture produce equivalent observable extracted documentation for declaration keys,
normalization, nested declarations, parameters, and empty documentation, while also verifying global service discovery
and Groovy 3, 4, and 5 behavior. Storage may legitimately differ between documentation properties and annotations; the
normalized protocol semantics must agree. GitHub issue #43 owns this suite.
