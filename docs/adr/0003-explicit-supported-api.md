# Define compatibility through an explicit supported API

AnnoDocimal defines source and binary compatibility through an explicit supported-API allowlist for each contract family,
not through the set of public classes contained in published JARs. Existing public types remain provisional until they are
classified; implementation types may be internalized only through appropriately versioned changes. Publication alone and
Java `public` visibility do not promote a type into the supported contract.

For the 1.0 boundary, provisional 0.x source APIs do not receive compatibility shims by default. Known consumers are
migrated and the path is documented; shims remain only when independently useful and cheap (ADR 0049).
