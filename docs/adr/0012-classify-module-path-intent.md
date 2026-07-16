# Classify every published artifact's module-path intent

Every published AnnoDocimal artifact receives a documented module-path classification: explicit Java module where
practical, stable automatic module where Groovy-version, shading, or transitional API constraints make an explicit
descriptor unsuitable, or build-tool-only and intentionally outside JPMS. Stable module identity is part of product
compatibility, but explicit descriptors do not take precedence over one artifact set working with Groovy 3, 4, and 5.
Service-provider metadata and packaging must agree with the classification, so issue #36's global-AST provider split is a
real defect. Exact names and per-artifact classifications remain to be confirmed.
