# Require valid inherited-generic projections for 1.0

Issue #32 blocks AnnoDocimal 1.0. A source projection that emits an unresolved inherited type variable produces invalid
Java rather than a merely reduced-fidelity mirror. The fix requires an AnnoDocimal-native regression fixture for the
supported inheritance/generic shape and must compile the generated source as verification. KlumAST may supply a
consumer example but must not be the test oracle.
