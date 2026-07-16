# Require deterministic annotation projection for 1.0

Issue #33 blocks AnnoDocimal 1.0. Source projection must normalize annotation-member ordering rather than inherit
generation-specific iteration order from Groovy or bytecode tooling. The regression must verify identical deterministic
output across the supported Groovy 3, 4, and 5 lanes.
