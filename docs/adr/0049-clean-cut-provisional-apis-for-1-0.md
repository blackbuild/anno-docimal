# Prefer a clean cut for provisional APIs at 1.0

AnnoDocimal 1.0 should expose the best supported source API rather than preserve undefined 0.x helper surfaces for
hypothetical consumers. The known consumer, KlumAST, must be migrated before its 4.0 release, and the repository must
publish a clear 0.x-to-1.0 migration path. A compatibility shim is retained only when it is independently useful and
cheap, not as a default obligation. This rule applies to provisional source APIs; it does not override separately
confirmed persisted-protocol compatibility such as continued `@AnnoDoc(String)` readability.
