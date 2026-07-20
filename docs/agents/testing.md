# Testing

AnnoDocimal currently builds with Java 17 and exercises Groovy 3, 4, and 5. Passing one Groovy generation is not evidence
that the others pass when compiler APIs, AST metadata, bytecode, or rendering may differ.

Java 17 is the current minimum and one artifact set supports all three Groovy generations. Catalogued patch versions are
tested baselines rather than the only permitted versions. Raising Java or dropping a Groovy generation is a product-wide
major compatibility decision, not an incidental test-matrix edit.

## Test lanes

| Gradle task | Groovy generation | Use |
|---|---:|---|
| `test` | 3 | Baseline for focused tests and normal development |
| `groovy4Tests` | 4 | Compatibility check for version-sensitive changes and final validation |
| `groovy5Tests` | 5 | Compatibility check for version-sensitive changes and final validation |
| `check` | 3, 4, 5 where configured | Final repository verification |

Start with the narrowest relevant test. Run Groovy 4 and 5 during development when a change touches compiler APIs, AST
behavior, Groovy syntax, bytecode shape, dependency compatibility, or known output differences. Run root `./gradlew check`
before final handoff.

A documentation-only change does not require the Gradle test lanes or root `check` when its changes cannot affect
compilation, test execution, generated output, or runtime behavior. Run `git diff --check` plus any applicable Markdown,
link, rendering, or documentation-generation checks instead, and report the omitted Gradle validation. Treat changes to
build configuration, executable or compiled examples, generated-source inputs, and test fixtures as code changes rather
than documentation-only changes.

Projects using the multi-Groovy convention share `src/test/groovy`, `src/test/java`, and `src/test/resources` as source
inputs, then compile and process those inputs into separate output directories for every lane. Groovy 4 and 5 therefore
reuse neither Groovy 3 test classes nor Groovy 3 test resources. Across compiled and runtime inputs, sharing is limited to
the production artifact and explicitly declared shared test dependencies. `verifyTestLaneIsolation`, which is part of
each affected project's `check`, rejects
mismatched Groovy or Spock dependencies, cross-lane compiled output on compile or runtime classpaths, unexpected test
class directories, and missing or misplaced lane-specific JUnit results. Build-logic TestKit coverage proves both the
isolated success path and rejection when a compatibility lane receives Groovy 3 compiled test output.

Gradle-plugin changes require TestKit evidence proportionate to the contract, including task input sensitivity,
up-to-date behavior, build-cache restoration, stale-output handling, and configuration-cache reuse where claimed.
Source-projection changes require exact generated-source assertions for affected Java and Groovy shapes.

The Gradle integration must document and verify a supported Gradle range. Use the wrapper as the primary development
baseline and test the documented minimum separately. Raising that minimum is a breaking compatibility change.

## Issue traceability

Every newly added test must carry Spock's `@Issue` annotation with the number of its driving GitHub issue. When one issue
owns a complete specification or test class, put `@Issue` on the class; that annotation is sufficient for every test in
the class driven by that issue. Put `@Issue` on an individual test when a later or different issue drives that test.

When a change affects an existing test, add or amend its `@Issue` annotation only if the test change is significant. A
change is significant when it materially changes the tested behavior, scenario, or expected contract. Mechanical edits,
renames, formatting, and adjustments to shared setup do not require issue-annotation churn.

This policy applies prospectively. Do not expand the current work by retrofitting unrelated parts of the existing test
suite.

Every ignored, conditionally ignored, or pending test must state an actionable reason and, where possible, the condition
for removing the suppression.

Published API compatibility is checked against the per-artifact supported-API baseline. The check covers only types
deliberately classified as supported and must not freeze implementation types merely because they are currently public.
Any accepted incompatibility needs an intentional release-facing explanation.
