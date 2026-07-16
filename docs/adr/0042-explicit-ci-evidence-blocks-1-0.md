# Require explicit compatibility evidence in CI for 1.0

Explicit CI evidence blocks AnnoDocimal 1.0. CI must visibly verify Java 17, Groovy 3, 4, and 5, the empirically selected
minimum and current Gradle versions, configuration-cache reuse, and clean publication/consumer smoke tests. The workflow
may combine checks efficiently, but every compatibility promise must have an identifiable, diagnosable result rather
than being implicit inside one opaque build outcome. GitHub issue #46 owns this gate.
