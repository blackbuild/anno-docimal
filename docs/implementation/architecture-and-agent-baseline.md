# Architecture and agent baseline

Date: 2026-07-16; API-grilling refinement: 2026-07-18

This plan records repository-native evidence and confirmed decisions. It is not an API or build implementation plan.
After the architecture grilling, Stephan separately authorized the GitHub curation recorded in the issue-curation plan.
The later issue #37 decisions in ADRs 0053-0056 and `docs/api/1.0-supported-api.md` supersede provisional API names and
plugin-ID details in this baseline where they differ.

## Confirmed decisions

### Product boundary

AnnoDocimal is an independent documentation transport and source-projection library for generated JVM APIs. KlumAST is an
important consumer, but KlumAST-specific documentation wording and IDEA-only model wiring are consumer policy.

Recorded in `CONTEXT.md` and ADR 0001.

### Product and release structure

The six published modules form one versioned product with four contract families:

1. documentation capture and carriers;
2. transformation-author extraction and documentation-model helpers;
3. compiled-class source projection;
4. Gradle build integration.

All artifacts release together. Major Java or Groovy baseline changes and major implementation-technology switches are
coordinated, tested, versioned, and communicated as product-wide compatibility changes. Contract families may receive
different stability classifications, but modules are not independently versioned products.

Recorded in ADR 0002.

### API compatibility boundary

Source and binary compatibility are governed by an explicit supported-API allowlist for each contract family. A type is
not supported merely because it is public or included in a published JAR. Issue #37 classified all 113 current public
types and defined the intended 1.0 allowlists in `docs/api/1.0-supported-api.md`; only the not-yet-final authoring members
of `Documentation.Builder` remain provisional under issue #51.

Recorded in ADRs 0003, 0026, and 0053-0056.

### Documentation protocol ownership

`anno-docimal-annotations` owns the stable cross-stage documentation protocol:

- `@AnnoDoc` is the current compiled documentation carrier;
- `@InlineJavadocs` is the opt-in capture marker for Java annotation processing and local Groovy AST transformation;
- externally observable documentation-properties naming and key semantics are part of the protocol.

The protocol is intentionally extensible. Open issue #30 sketches future structured annotations for tags, format control,
and parameters. This baseline does not select that schema, decide whether it augments or replaces `@AnnoDoc`, or define its
migration; future work must handle those compatibility choices explicitly.

Recorded in ADR 0004.

### Transformation-author API

Transformation authors receive the supported `AstDocumentation` facade for exact extraction, semantic attachment,
normalized-text attachment, and declaration references, plus immutable `Documentation` and its transient builder.
Source-position parsing, Groovy-version adaptation, visitors, metadata caching, and transformation machinery are not
part of that contract merely because current implementation types are public.

Recorded in ADRs 0005 and 0053.

### Curated transformation-author surface

The supported transformation-author API is the curated facade and semantic value model in ADR 0053. Current extractors,
builders, parsers, template handlers, visitors, caches, and utility classes are implementation-only or replaced without
shims. Issue #30's structured carrier schema remains separate. AnnoDocimal #51 and KlumAST #489 must settle the final
authoring/template members before 1.0.

Recorded in ADRs 0006, 0049, 0053, and 0056.

### Source-projection API boundary

The generator contract is the final, thread-safe `SourceProjector`, immutable `ProjectionPolicy` and builder,
`DeclarationVisibility`, and `SourceProjectionException`. It projects one caller-selected top-level class to text or a
managed file without exposing ASM, JavaPoet, visitors, signature parsers, type conversion, or shaded types.
`AnnoDocGenerator` is replaced without a shim.

Recorded in ADRs 0007 and 0054.

### Source-projection fidelity

Projection output is a deterministic, Javadoc-consumable and IDE-parseable declaration mirror. It preserves the selected
declaration shape—type kind and nesting, visibility and modifiers, generics and inheritance, members, parameter metadata,
annotations, exceptions, and documentation—but does not promise original bodies, initializers, synthetic implementation
details, whitespace, or import layout.

Issue #32 is therefore a projection-fidelity defect and issue #33 is a deterministic-output defect. Exact textual layout
is not a compatibility contract unless separately documented.

Recorded in ADR 0008.

### Projection inclusion policy

`ProjectionPolicy.documentation()` includes public/protected declarations and named member types, excludes synthetic and
Groovy-runtime scaffolding, and always applies signature closure. The builder exposes only implementation-neutral
visibility, nested, synthetic, and Groovy-runtime controls. Top-level class-file selection remains a consuming-build or
Gradle-task concern; annotation-presence filtering is a future additive feature.

Recorded in ADRs 0009 and 0054.

### Reusable Gradle task contract

`SourceProjectionTask` is the supported reusable task type. Its API is limited to the five Gradle property getters for
classpath-sensitive class directories, include/exclude patterns, nested projection policy, and output directory. It owns
all-or-nothing synchronized output, deterministic duplicate detection, stale cleanup, cacheability, and configuration-
cache-safe execution. `CreateClassStubs` is replaced without a shim. IDE model wiring remains consumer-owned.

Recorded in ADRs 0010 and 0055; issue #35 owns implementation and TestKit acceptance.

### Layered Gradle plugins

The supported plugin IDs form a neutral/opinionated pair:

- `com.blackbuild.annodocimal.base-plugin` applies no language plugin, reacts lazily to the Java model, and otherwise
  leaves manual `SourceProjectionTask` registration available; and
- `com.blackbuild.annodocimal.groovy-plugin` applies Gradle's Groovy plugin and the base layer, then configures required
  Groovy/Java compiler metadata.

The ambiguous `com.blackbuild.annodocimal.plugin` ID is replaced without a shim. A future Java convention and an external
shared Groovy/Spock convention remain additive options. Both delivered layers must remain configuration-cache safe.

Recorded in ADRs 0011 and 0055.

### Module-path policy

Each published artifact receives one documented classification:

- explicit Java module where practical;
- stable automatic module where Groovy-version, shading, or transitional API constraints make an explicit descriptor
  unsuitable;
- build-tool-only and intentionally outside JPMS.

Stable module identity is a product compatibility contract. Explicit descriptors do not override the requirement for one
artifact set to work with Groovy 3, 4, and 5. Service-provider metadata and packaging must match the classification, so the
current global-AST provider split remains an issue #36 defect.

Recorded in ADR 0012.

### Initial artifact module classifications

Issue #36 should initially assign these stable automatic module names:

| Artifact | Classification and module identity |
|---|---|
| `anno-docimal-annotations` | stable automatic `com.blackbuild.annodocimal.annotations` |
| `anno-docimal-apt` | processor-path-oriented stable automatic `com.blackbuild.annodocimal.apt` |
| `anno-docimal-ast` | stable automatic `com.blackbuild.annodocimal.ast` |
| `anno-docimal-global-ast` | stable automatic `com.blackbuild.annodocimal.global.ast`, after service packaging repair |
| `anno-docimal-generator` | stable automatic `com.blackbuild.annodocimal.generator` |
| `anno-docimal-gradle-plugin` | build-tool-only, outside JPMS |

Explicit descriptors can be reconsidered after package and API cleanup through a major, compatibility-tested design
change. They are not the immediate issue #36 target.

Recorded in ADR 0013.

### Global-AST service adapter

`anno-docimal-global-ast` should contain a minimal global transformation provider class that its own service descriptor
names. That provider delegates to the reusable local transformation implementation in `anno-docimal-ast`.

This preserves optional global activation and local `@InlineJavadocs` use while repairing the cross-artifact provider
declaration. Issue #36 owns implementation plus classpath and module-path acceptance.

Recorded in ADR 0014.

### Documentation ownership

- `README.md` is the concise landing page and quick start.
- Version-controlled files under `docs/` own detailed user and architecture documentation.
- Generated Javadocs provide API reference.
- A mutable wiki is not a canonical source.

This direction is intended to align with KlumAST's planned move away from wiki-owned documentation and a future common
project scaffolding across AnnoDocimal, klum-cast, KlumAST, and additional Klum-family projects. The common scaffolding
mechanism is a separate cross-repository decision and is not selected here.

Recorded in ADR 0015.

### Release and migration documentation

- `CHANGES.md` is the canonical release history and contains an unreleased section.
- Breaking changes receive guides under `docs/migration/`, linked from the changelog and affected user documentation.
- GitHub Release text publishes the same repository-owned information rather than becoming a separate source.
- Java/Groovy baseline changes, protocol migrations, artifact/module changes, and supported-API changes require explicit
  release and migration documentation.

Historical releases are currently represented by Git tags. Any changelog backfill must be evidence-based and is separate
from this baseline.

Recorded in ADR 0016.

### Current versioned documentation mechanism

Until shared cross-project documentation infrastructure exists:

- each Git release tag is the immutable repository documentation snapshot for that version;
- the default branch documents the next release;
- versioned source and Javadoc artifacts in Maven Central provide matching artifact-level references;
- documentation uses repository-relative links where practical;
- AnnoDocimal does not build a bespoke hosting or version-selector system.

A future shared system could initially use the common `com.blackbuild` namespace across AnnoDocimal and Klum-family
projects. That would ease a shared first step but could reduce visible Klum branding; the cross-project documentation work
must decide that tradeoff.

Recorded in ADR 0017.

### Java and Groovy compatibility

- Java 17 is the minimum runtime/toolchain level.
- One released artifact set supports Groovy generations 3, 4, and 5.
- Catalogued patch versions are tested baselines, not the only permitted versions.
- Raising Java or dropping a Groovy generation is a product-wide major compatibility change.

If the Groovy 3 versus 4/5 JPMS module-name split proves unreasonable to solve across the related libraries, dropping
Groovy 3 remains a last-resort cross-project option. Jenkins no longer supplies the former reason for retaining Groovy
2.4. This exception is not selected here and must not become the default resolution of issue #36 or #455.

Recorded in ADR 0018.

### Gradle compatibility

The Gradle integration will publish an explicit TestKit-verified supported range. The wrapper is the primary development
baseline rather than automatically the minimum. CI should verify the documented minimum and current baseline, including
configuration-cache behavior where claimed. Raising the minimum is a release-facing breaking change.

The actual minimum must be measured before selection and is not decided by this baseline.

Recorded in ADR 0019.

### Capture integration boundary

These are supported behavioral contracts:

- configured Java APT plus `@InlineJavadocs` produces documentation-properties resources;
- `@InlineJavadocs` triggers local Groovy documentation capture;
- adding `anno-docimal-global-ast` enables global capture through service discovery;
- every path produces the documented protocol.

Processor, transformation, visitor, source-parser, and Groovy-version-adapter classes are implementation types. Required
provider/transformation class names remain packaging obligations rather than general-purpose source APIs.

Recorded in ADR 0020.

### Documentation inheritance and resolution

Extraction distinguishes exact documentation attached to a declaration from resolved documentation inherited or
augmented from overridden methods and supertypes. Resolution follows documented Javadoc-like rules, including explicit
`{@inheritDoc}` augmentation; projection does not apply inheritance invisibly.

Issue #10 owns the detailed resolution algorithm and future facade shape.

Recorded in ADR 0021.

### Groovy property documentation

Property documentation is a semantic input owned by AnnoDocimal. Capture should retain the association while Groovy's
property model is available, and a documented policy should determine how it applies to the property, backing field,
and generated accessors. Source projection must not synthesize that meaning solely from JavaBeans-shaped names.

Issue #9 owns the detailed rules. Future structured protocol work in issue #30 may provide a richer representation, but
that does not postpone recognizing the current property semantics.

Recorded in ADR 0022.

### Runtime GroovyDoc interoperability

Groovy's runtime GroovyDoc annotation is an accepted interoperable input carrier. AnnoDocimal's protocol remains the
canonical cross-language representation and the place where structured documentation evolves. Capture, extraction,
and projection require deterministic precedence and must avoid duplicate carrier emission.

Issue #19 owns the detailed compatibility work; its checklist must be reconciled with the current Groovy 3+ capture
implementation rather than applied literally.

Recorded in ADR 0023.

### Normalized textual representation

Textual carriers store normalized documentation content without source-comment delimiters or leading line decoration.
Capture adapters normalize Java, Groovy, and future supported documentation syntaxes; renderers add the syntax required
by their output dialect. This allows newer Java Markdown documentation comments to be supported without changing the
textual carrier contract.

The Markdown adapter and the structured annotation schema remain future design work rather than baseline
implementation.

Recorded in ADR 0024.

### Structured protocol evolution

Structured documentation annotations are introduced additively, with a compatibility bridge that continues reading
the current `@AnnoDoc(String)` carrier. Ending old-carrier readability requires an explicit major-version decision and
a detailed migration plan.

Issue #30 owns the schema, documentation-properties evolution, precedence, mixed-version behavior, and rollout. Those
details are deliberately outside this baseline session.

Recorded in ADR 0025.

### Supported-API enforcement and the 1.0 gate

Each published artifact receives a mechanical compatibility baseline for its explicitly supported API. The check must
not accidentally freeze every public implementation type, and accepted incompatibilities require release-facing
rationale.

Issue #37 completed the public-type classification, finalized every independent allowlist, and specified how the human
allowlist feeds per-artifact machine-readable source/binary signature snapshots. Issue #51 must resolve the one provisional
authoring-language slot before implementation locks the first baseline.

Recorded in ADR 0026 and `docs/api/1.0-supported-api.md`.

### Structured protocol release target

Issue #30 does not block 1.0. The 1.0 release stabilizes the current carrier behavior and additive-evolution promise;
the structured annotation initiative targets a future 2.0 release. It is treated as a major ecosystem change even if
the compatibility bridge prevents a strict source or binary break.

The later migration plan must include supporting projects such as the AnnoDoc IntelliJ plugin and mixed protocol
versions. That coordination is outside this baseline session.

Recorded in ADR 0027.

### Gradle correctness release gate

Issue #35 blocks 1.0. The reusable source-projection task requires documentation-sensitive invalidation, deterministic
stale-output cleanup, correct up-to-date and build-cache behavior, configuration-cache reuse, and TestKit verification
across the supported Gradle range before the Gradle integration is stabilized.

This baseline records the acceptance boundary; implementation stays in issue #35.

Recorded in ADR 0028 and the 1.0 release plan.

### Module-path correctness release gate

Issue #36 blocks 1.0. The confirmed stable automatic names and global-AST provider adapter must be implemented and
verified before release because module identities and service packaging are published compatibility contracts.

Explicit descriptors remain a later option and must not predetermine the multi-Groovy redesign in KlumAST issue #455.
AnnoDocimal 1.0 is a hard release prerequisite for KlumAST 4.0. Development may use pre-release versions, but KlumAST
4.0 must not stabilize or release against provisional AnnoDocimal 0.x APIs.

Recorded in ADR 0029 and the 1.0 release plan.

The cross-repository sequencing decision is recorded separately in ADR 0030. Implementation coordination and KlumAST
issue/release-plan changes stay in the owning repository.

### Inherited-generic projection release gate

Issue #32 blocks 1.0. Projecting an unresolved inherited type variable produces invalid Java. The regression must be
captured in an AnnoDocimal-native fixture and the resulting source compiled; KlumAST can supply an example but is not
the test oracle.

Recorded in ADR 0031 and the 1.0 release plan.

### Deterministic annotation projection release gate

Issue #33 blocks 1.0. Annotation-member ordering must be normalized instead of inheriting Groovy-generation or bytecode-
tool iteration order, with identical output verified across Groovy 3, 4, and 5.

Recorded in ADR 0032 and the 1.0 release plan.

### Property-documentation release gate

Issue #9 blocks 1.0. Groovy properties are a core supported declaration shape, so their property/field/accessor
documentation mapping must be explicit and verified using Groovy's semantic property relationship.

Recorded in ADR 0033 and the 1.0 release plan.

### Documentation-inheritance release timing

Issue #10 does not block 1.0. Exact extraction must be unambiguous and must not imply inherited resolution. The full
Javadoc-like inheritance and `{@inheritDoc}` algorithm follows after 1.0 as a distinct additive capability.

Recorded in ADR 0034 and the 1.0 release plan.

### Runtime GroovyDoc interoperability release gate

Issue #19 blocks 1.0 after being re-scoped from its Groovy-2-era checklist. Acceptance covers runtime GroovyDoc
recognition, capture duplicate avoidance, projection support, and deterministic carrier precedence across Groovy 3, 4,
and 5.

Recorded in ADR 0035 and the 1.0 release plan.

### Extractor naming and issue 18

Issue #18 is not a separate extractor-merge implementation. The source and AST extractors operate in different phases;
its remaining naming/facade concern is an input to the mandatory pre-1.0 API grilling. The authorized curation pass
closed it as superseded by #37 and #38.

Recorded in ADR 0036 and the 1.0 release plan.

### Groovy 2 parameter workaround

Issue #22 is obsolete under the Groovy 3+ compatibility floor. Jenkins no longer justifies adding a Groovy 2-specific
parameter-name carrier, so the authorized curation pass retired it.

Recorded in ADR 0037 and the 1.0 release plan.

### Executable generator timing

Issue #23 does not block 1.0. A CLI remains a post-1.0 enhancement pending a concrete contract for input selection,
output cleanup, diagnostics, exit codes, and distribution. Adding only an executable manifest is insufficient.

Recorded in ADR 0038 and the 1.0 release plan.

### Generator technology timing

Issue #25 does not block 1.0. Projection validity, fidelity, and determinism are the contract; ASM, JavaPoet, Palantir
JavaPoet, or another implementation is internal. The authorized curation pass closed the solution-first issue; a future
behavioral issue may demonstrate concrete value.

Recorded in ADR 0039 and the 1.0 release plan.

### Documentation-completeness release gate

Repository-owned documentation blocks 1.0. README/docs, published Javadocs, release notes, compatibility and supported-
API references, projection semantics, and 0.x-to-1.0 migration guidance must consistently cover all artifacts and both
plugins. Historical changelog backfill remains evidence-based but is not a release gate.

Recorded in ADR 0040 and the 1.0 release plan.

### Publication-consistency release gate

Publication consistency blocks 1.0. A clean local-repository consumer smoke test must verify all six artifacts and both
plugin markers, including metadata, dependency scopes, signatures, sources/Javadocs, shaded contents, descriptors, and
resolution. The generator shadow publication's missing Gradle module metadata and documentation artifacts require
explicit resolution, without prescribing the Gradle implementation.

Recorded in ADR 0041 and the 1.0 release plan.

### Explicit CI-evidence release gate

Explicit CI evidence blocks 1.0. Java 17, Groovy 3/4/5, minimum and current Gradle, configuration-cache reuse, and
publication/consumer smoke tests must each produce identifiable, diagnosable results. Efficient job composition remains
an implementation choice.

Recorded in ADR 0042 and the 1.0 release plan.

### Release-process-readiness gate

A documented and rehearsed release procedure blocks 1.0. It must cover version/tag derivation, signed Maven Central
staging, Plugin Portal publication, changelog/GitHub Release synchronization, verification, and failure recovery. Full
automation is optional, and this session does not authorize publication.

Recorded in ADR 0043 and the 1.0 release plan.

### Explicit projection-policy release gate

The inclusion policy in ADR 0054 defines visibility, nested declarations, synthetic and Groovy runtime artifacts,
mandatory signature closure, semantic top-level roots, and removal of `AnnoDocGenerator` without a shim. Top-level
collection selection remains in the build/task layer. GitHub issue #39 owns implementation.

Recorded in ADRs 0044 and 0054 and the 1.0 release plan.

### Projection-contract-suite release gate

A projection fixture suite blocks 1.0. It must assert deterministic output and compile projected Java for representative
Java/Groovy declarations, generics, nested types, annotations, supported records/enums, and documentation carriers
across Groovy 3, 4, and 5. Issue #32 is one regression in this broader matrix. GitHub issue #41 owns the suite.

Recorded in ADR 0045 and the 1.0 release plan.

### Capture-conformance release gate

A shared capture suite blocks 1.0. Java APT, local Groovy capture, and global Groovy capture must produce equivalent
observable normalized documentation for the supported declaration cases, while service discovery and all Groovy lanes
are verified. Storage may differ between properties and annotations. GitHub issue #43 owns the suite.

Recorded in ADR 0046 and the 1.0 release plan.

### Curated transformation-author API release gate

Delivery of `AstDocumentation`, immutable `Documentation`, and its builder blocks 1.0. Issue #37 designed the facade,
mapped consumers, classified current helpers, and selected clean migration without issue #30's carrier schema. Issue #38
owns implementation; issue #51 and KlumAST #489 must settle its final authoring-language members.

Recorded in ADRs 0047, 0053, and 0056 and the 1.0 release plan.

### Narrow source-projection API release gate

Delivery of the narrow implementation-neutral projection service blocks 1.0. ADR 0054 defines source text and managed
file output, policy construction, inclusion and failure behavior, and the supported type allowlist. ASM, JavaPoet,
parsers, visitors, and shaded types remain internal; `AnnoDocGenerator` receives no shim.

Recorded in ADRs 0048 and 0054 and the 1.0 release plan.

### Provisional-API migration rule

AnnoDocimal 1.0 prefers the best supported API over mandatory shims for undefined 0.x helper surfaces. KlumAST, the only
known consumer, must be migrated and the path documented. Shims remain only when independently useful and cheap. This
does not override separately confirmed persisted-protocol compatibility.

Recorded in ADR 0049 and the 1.0 release plan.

### 1.0 issue structure

One tracking issue coordinates the 1.0 release narrative, dependency order, and gate visibility. Independently executable
issues retain their own acceptance criteria, and the mandatory API grilling is a separate human-decision predecessor.

Recorded in ADR 0050 and the issue-curation plan.

The tracker and originally curated hard-gate issues use a dedicated GitHub `1.0` milestone for queryable progress.
AnnoDocimal #51 is an additional authorized 1.0 prerequisite but its tracker/milestone/label curation remains unchanged
until separately authorized. Post-1.0 and 2.0 work remains outside the milestone. Recorded in ADRs 0051 and 0056.

The completed historical `initial-release` milestone is closed rather than renamed or reused. Recorded in ADR 0052.

## Current module and API map

| Module | Owning role | Published surface observed at 0.7.1 |
|---|---|---|
| `anno-docimal-annotations` | Runtime documentation carrier and Java-source capture marker | `@AnnoDoc`, `@InlineJavadocs` |
| `anno-docimal-apt` | Java source documentation capture into properties resources | Annotation processor and public properties builder; API dependency on annotations |
| `anno-docimal-ast` | Groovy source capture plus extraction and documentation-model helpers for AST authors | Local transformation, visitor, extractors, parsers, formatters, builders, templating and inheritance helpers; API dependency on annotations |
| `anno-docimal-global-ast` | Global Groovy AST discovery adapter | Service descriptor plus API dependency on `anno-docimal-ast` |
| `anno-docimal-generator` | Compiled class to Java source projection | Generator entry point plus public ASM/JavaPoet implementation types; shaded publication |
| `anno-docimal-gradle-plugin` | Javadoc task integration and compiler option convention | Two plugin IDs, public `CreateClassStubs` task type, shaded generator classes |

## Current lifecycle map

1. Java APT or a Groovy AST transformation captures source documentation.
2. Transformation authors extract, parse, rewrite, template, or attach documentation while generating APIs.
3. Compiled classes retain embedded documentation and parameter metadata.
4. The generator projects top-level class files and their public nested types into Java source.
5. The base Gradle plugin registers `createClassStubs` and makes `javadoc` consume its output.
6. The convention plugin also enables Groovy documentation metadata and Java/Groovy parameter names.

IDE-only source mirrors reuse steps 3 and 4 but are not part of the default Javadoc lifecycle. Selection and IDEA model
wiring currently belong to the consumer.

## Verified compatibility and publication facts

- Java toolchains target 17.
- Groovy 3.0.25 is the baseline test runtime; Groovy 4.0.32 and 5.0.6 use compatibility suites over shared tests.
- `./gradlew check` passed on 2026-07-16 and exercised all configured Groovy lanes.
- CI uses one Ubuntu/JDK 17 job and runs `build jacocoTestReport sonar`; the Gradle graph supplies compatibility lanes.
- Six artifacts and two Gradle plugin markers are configured for publication through Maven Central/Plugin Portal tooling.
- Most artifacts publish Gradle module metadata; the generator shadow publication does not.
- The five Java-facing module-path artifacts declare the stable automatic names confirmed for issue #36; the Gradle
  plugin remains build-tool-only and outside JPMS, while the APT artifact remains processor-path-oriented.
- The global-AST JAR packages its service descriptor with its provider adapter, which delegates to the reusable local
  transformation in the AST artifact. Module-path and classpath discovery are exercised across Groovy 3, 4, and 5.

## Governance baseline established

- Root `AGENTS.md` routes repository policy to `docs/agents/`.
- Root `CONTEXT.md` and `docs/adr/` own vocabulary and accepted architectural decisions.
- GitHub issue handling, triage roles, documentation ownership, coding/testing, dedicated branches, pre-review history
  cleanup, additive review commits, consolidated review responses, CI evidence, and release-facing documentation follow
  the adapted KlumAST baseline.
- The canonical triage-role labels were aligned with KlumAST after explicit maintainer approval. A later explicit approval
  authorized the 1.0 milestone/tracker, gate-issue creation, existing-issue curation, and confirmed historical closures.
- Generic research, grilling, domain-modeling, implementation, code-review, bug-diagnosis, TDD, QA, and triage workflows
  are available under `.agents/skills/` and route through AnnoDocimal's own policies and test lanes.
- No AnnoDocimal-specific workflow skills are introduced by this plan.

## Architectural pressure points requiring decisions

1. The authoring/template language in AnnoDocimal #51 and KlumAST #489, including code blocks, possible paragraph kinds,
   substitution/failure semantics, and the final builder-member allowlist.
2. The measured minimum supported Gradle version for issue #35's TestKit matrix.
3. Long-term shared documentation publication and branding mechanics.

Deferred protocol evolution: issue #30 may introduce structured documentation annotations and targets a future 2.0
release. Keep it separate from the 1.0 baseline and do not infer its design from the existing issue stub.

KlumAST issue #455 owns the cross-repository multi-Groovy redesign. This repository supplies facts but does not select that
design in this session.

Issue #455 should retain a last-resort decision branch for dropping Groovy 3 across the related libraries if the JPMS
module-name split cannot be solved proportionately. Exercising that branch would require an explicit major compatibility
decision and coordinated migration, not a repository-local build simplification.

## Applied issue map

The authorized curation pass created tracker #47 and gate issues #37–#41 and #43–#46. Existing 1.0 gates #9, #19,
#32, #33, #35, and #36 were curated into milestone `1.0`; #36 was later completed through merged PR #50. Issues #18,
#22, and #25 were closed with confirmed rationale. The issue #37 session separately created unlabelled AnnoDocimal #51
and KlumAST #489 without changing the tracker or other existing issues. Detailed scope, dependency order, and non-gates
live in `docs/implementation/1.0-issue-curation-plan.md`.
