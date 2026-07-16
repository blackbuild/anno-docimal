# Publish a verified Gradle compatibility range

AnnoDocimal's Gradle integration publishes an explicit TestKit-verified supported Gradle range. The wrapper is the primary
development baseline rather than automatically the minimum; CI verifies a documented minimum and the current baseline,
including configuration-cache behavior where claimed. Raising the minimum Gradle version is a release-facing breaking
change. The actual minimum must be measured before it is selected and is not decided by this baseline.
