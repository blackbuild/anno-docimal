# Publication validation

AnnoDocimal releases one product version across six Maven artifacts and two Gradle plugin IDs. The publication audit
checks that complete product without signing, publishing, credentials, or a pre-existing local Maven repository.

| Maven coordinate | Published role |
| --- | --- |
| `com.blackbuild.annodocimal:anno-docimal-annotations` | Documentation carrier annotations |
| `com.blackbuild.annodocimal:anno-docimal-apt` | Java annotation processor |
| `com.blackbuild.annodocimal:anno-docimal-ast` | Groovy AST capture and supported authoring API |
| `com.blackbuild.annodocimal:anno-docimal-global-ast` | Global Groovy AST capture service |
| `com.blackbuild.annodocimal:anno-docimal-generator` | Standalone source-projection generator |
| `com.blackbuild.annodocimal:anno-docimal-gradle-plugin` | Shaded Gradle plugin implementation |

The plugin marker IDs are `com.blackbuild.annodocimal.base-plugin` and
`com.blackbuild.annodocimal.groovy-plugin`. Their marker POMs point to the plugin implementation at the same version.

## Local publication audit

Run the complete network-independent audit with:

```shell
./gradlew :publication-smoke-tests:test
```

The audit stages unsigned artifacts directly under `build/publication-audit-repository`; it does not invoke a Maven
publish task, a Plugin Portal task, or a signing task. It verifies:

- all six primary JARs, POMs, Gradle module metadata files, source JARs, and Javadoc JARs;
- both plugin marker POMs and their exact implementation dependencies;
- intended Maven scopes, Gradle variants, referenced files, and publication descriptions;
- stable automatic module names plus the annotation-processor and global-AST service providers;
- both plugin descriptors and their implementation classes;
- relocated generator dependencies and the absence of accidental unrelocated, Gradle, Groovy, or JSpecify contents in
  the shaded generator and plugin JARs; and
- clean, offline Gradle and Apache Maven consumers resolving all six coordinates. The Gradle fixture also applies both
  plugin IDs. The Maven runtime is a pinned build-tool dependency and uses a fresh isolated local repository.

CI exposes this same audit as the **Clean publication and consumer smoke tests** check. Its result is behavioral
evidence for the local publication contract; a passing Sonar analysis is supplementary static analysis and never
substitutes for this check.

`verifyPublicationReleaseConfiguration`, included in the root `check`, inspects the signing tasks for every Maven
publication, the Maven Central staging and snapshot endpoints, and the Gradle Plugin Portal publication task. It does
not read signing credentials, create signatures, open a staging repository, or publish anything.

## Authorized public RC validation

After one exact immutable RC version has been explicitly authorized and published to the public repositories, validate
that version with:

```shell
./gradlew publicRcSmoke -PannodocimalVersion=1.0.0-rc.1
```

The version must match `MAJOR.MINOR.PATCH-rc.NUMBER`; dynamic versions and snapshots are rejected. The generated clean
fixtures use fresh, isolated Gradle user homes and only Maven Central and the Gradle Plugin Portal. They contain no local
repository, flat-directory, composite-build, or project fallback, and exercise all six Maven artifacts and both plugin
IDs at the exact supplied version.

Preparing this task does not authorize an RC or a release. Until an authorized RC exists publicly, successful public
resolution remains an external delivery condition. Issue #45 owns the release runbook, protected authorization,
publication order, and recovery procedure. Its documentation-site handoff must retain proof before a final render writes
an RC successor status record; that mutable root record is the only permitted post-publication status change and never
rewrites an RC snapshot or manifest. A `pending` candidate or final documentation render may provide protected,
unlisted pre-publication evidence, but it authorizes no artifact publication, public status record, or navigation alias.
That single-use pending evidence is distinct from the disposable, non-release static-HTML presentation rehearsal. Its
local artifact is selected from `local-rehearsal/`; one or more maintainer-led platform rehearsals may temporarily use
that artifact from a clearly experimental Pages branch. They record each validation in #71 and keep Pages unprotected
until the first result is verified and accepted, after which Pages is deactivated and the branch is removed. They are not
retained on the canonical Pages ledger.

The canonical documentation writer is a separate, master-only, reviewed
`annodocimal-pages-writer`-gated job, not part of render, crawl, publication smoke, or rehearsal. It requires the
requested exact source to still be current `master`, validates the staged immutable manifest binding, and uses the
dedicated environment-scoped Pages-writer App only for the `gh-pages` push. A remote read-back must confirm the pushed
commit and manifest before the job succeeds. The separate `github-pages` environment is the credential-free GitHub
Pages service deployment from the protected `gh-pages` branch; it never receives or mints the App token. This is
deployment integrity evidence; it neither authorizes a release nor changes #45's ordering, recovery, RC/final, tagging,
signing, or release-record ownership.
