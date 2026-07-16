# Make declaration inclusion an explicit projection policy

The new projection service uses an explicit policy to select member visibility, nested types, synthetic members, and
Groovy-generated artifacts, while retaining declarations needed to represent selected signatures. Top-level class-file
selection belongs to the consuming build or Gradle task, not the projection engine. Existing `AnnoDocGenerator` entry
points retain their legacy behavior until a separately designed, versioned migration; this baseline does not choose the
new policy types or presets.
