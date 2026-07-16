# Evolve structured documentation carriers additively

Future structured documentation annotations are introduced additively with a compatibility bridge that continues to
read the current `@AnnoDoc(String)` carrier. A structured carrier may become preferred, but ending old-carrier
readability requires an explicit major-version decision and a detailed migration plan. Issue #30 owns that plan,
including the annotation schema, documentation-properties evolution, carrier precedence, mixed-version behavior, and
rollout. Those details are outside this baseline session.
