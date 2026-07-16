# Add inherited documentation resolution after 1.0

Issue #10 does not block AnnoDocimal 1.0. Version 1.0 must expose exact extraction unambiguously and must not imply that
documentation inheritance or `{@inheritDoc}` resolution occurs. The full documented Javadoc-like resolution algorithm
is a post-1.0 additive capability with a distinct API. This timing does not weaken the exact-versus-resolved semantic
boundary established in ADR 0021.
