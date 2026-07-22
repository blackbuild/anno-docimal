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

## Test class naming and organization

Name every new executable test class with the `Test` suffix. Normally use `<Subject><Concern>Test`, or `<Subject>Test`
when the subject is already narrow. Do not introduce new `*Spec` names, and do not expand unrelated work into a blanket
rename of existing `*Spec` classes.

A production class or concept does not require a one-to-one test class. Split tests when each resulting class has a
cohesive purpose and the split improves readability, navigation, or fixture clarity; keep related behavior together when
splitting would fragment ownership or duplicate setup.

## Documentary tests

Every newly added user-visible feature must have one or more readable documentary tests. At least one should be a happy
path that demonstrates the feature's basic use as executable code. Prefer meaningful API or plugin examples over
placeholder names when that makes the intended use easier to understand.

Mark each documentary Spock test with `@Tag("documentary")`. Put the tag on the feature method unless the whole class is
documentary, in which case a class-level tag is sufficient. Put `@See` on the same feature or class and link it to the
relevant canonical AnnoDocimal documentation under `docs/`, preferably to a stable heading anchor. Because Spock exposes
`@See` as a report attachment, use an absolute URL to the repository documentation.

Documentary examples may stay beside focused behavioral tests or live in a dedicated thematic class. Prefer a cohesive
`<Theme>DocumentaryTest` when several examples form a useful reading path, share understandable setup, or span multiple
driving issues within one theme. Keep an isolated happy path with its focused behavioral tests when extraction would
duplicate fixtures or fragment ownership. Use `DocumentaryTest`, not `DocumentationTest`, for a dedicated class.

Put `@Tag("documentary")` on a dedicated documentary class. If its feature methods originate from different issues, put
`@Issue` on each method. Put `@See` on each method when documentation targets differ; a class-level `@See` is sufficient
only when one documentation target genuinely covers the whole class.

A typical documentary feature method looks like:

```groovy
@Issue("123")
@Tag("documentary")
@See("https://github.com/blackbuild/anno-docimal/blob/master/docs/user/usage.md#capture-documentation")
def "demonstrates the basic feature usage"() {
    // readable happy path
}
```

Keep the feature issue, documentary test, and user documentation mutually traceable:

- The feature issue identifies the documentary test class and feature method, and the relevant documentation section.
- The documentary test carries the driving `@Issue` number, the `documentary` tag, and an `@See` link to the relevant
  documentation file or section.
- The documentation shows an abbreviated example aligned with the executable test, and identifies the documentary test
  class and feature method that exercise it.

This policy applies prospectively. Do not expand unrelated work into a suite-wide documentary-test, naming, or
traceability audit, and do not rename existing tests merely to conform to the new convention.

## Feature discussion examples

During grilling and implementation of a user-visible feature, use a compact example as a syntax or API probe before the
design is settled. Keep it small enough to expose awkward calls, names, or configuration and to compare alternatives.
Once accepted, use it as the seed for the documentary happy path and the abbreviated documentation example.

- For a public Java client API, show Java first and Groovy second when the Groovy view is meaningful.
- For Groovy-facing AST authoring, show the natural Groovy source surface.
- For Gradle or plugin behavior, show the natural Groovy or Gradle DSL surface and omit unrelated build setup.
- An internal-only change may omit the example, but state the reason briefly rather than skipping it silently.
