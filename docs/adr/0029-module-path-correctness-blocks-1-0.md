# Make module-path correctness a 1.0 release gate

Issue #36 blocks AnnoDocimal 1.0. The confirmed stable automatic module names and global-AST provider-adapter packaging
must be implemented and verified before 1.0, because module identities and service-provider packaging are published
compatibility contracts. Explicit module descriptors remain a later option and must not force the separate multi-Groovy
decision owned by KlumAST issue #455.

AnnoDocimal 1.0 is also intended to be a release dependency or strong recommendation for KlumAST 4.0. The exact
cross-repository gate strength requires a separate confirmed decision.
