/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2026 Stephan Pauxberger (Gradle Plugin) and others
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.blackbuild.annodocimal.publication

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Issue
import spock.lang.See
import spock.lang.Specification
import spock.lang.Tag
import spock.lang.Unroll

import java.util.regex.Matcher
import java.util.regex.Pattern

@Issue('45')
@Tag('documentary')
@See('https://github.com/blackbuild/anno-docimal/blob/master/RELEASING.md#protected-authorization-workflow')
class ProtectedReleaseAuthorizationContractTest extends Specification {

    def 'keeps RC and final authority in separately selected protected environments'() {
        given: 'the checked-in protected release contract'
        File repository = new File(System.getProperty('annodocimal.repository.root'))
        String workflow = new File(repository, '.github/workflows/publish-protected-release.yml').text
        String runbook = new File(repository, 'RELEASING.md').text
        String preflight = job(workflow, 'validate-release-input')
        String publishing = job(workflow, 'publish-complete-product')
        String ordinaryWorkflow = workflow.replace(publishing, '')

        expect: 'publication is manual, read-only until the selected environment provides authority'
        workflow.contains('workflow_dispatch:')
        !workflow.contains('pull_request:')
        !workflow.contains('push:')
        workflow.contains('contents: read')
        preflight.contains('[[ "$REVISION" =~ ^[0-9a-f]{40}$ ]]')
        preflight.contains('RELEASE_STAGE" == rc')
        preflight.contains('RELEASE_STAGE" == final')
        preflight.contains("echo 'name=annodocimal-release-rc'")
        preflight.contains("echo 'name=annodocimal-release-final'")
        !preflight.contains('secrets.')
        !preflight.contains('\n    environment:\n')

        and: 'the preflight fails closed before environment selection and proves source, tag, release record, and Pages handoff state'
        !workflow.contains("&& 'annodocimal-release-rc' || 'annodocimal-release-final'")
        workflow.contains('master_revision=')
        workflow.contains('refs/tags/v$VERSION')
        workflow.contains('release_status')
        workflow.contains('test "$release_status" = 404')
        workflow.contains('expected_path="pending/$RELEASE_VERSION/$REVISION"')
        workflow.contains('test ! -e "pages/$RELEASE_VERSION"')
        workflow.contains('.documentation.status == "pending"')
        workflow.contains('./gradlew verifyUnprivilegedReleaseInputs')
        workflow.contains('test "$master_revision" = "$REVISION"')

        and: 'only the dependent protected job receives publication credentials'
        publishing.contains('needs: validate-release-input')
        publishing.contains('name: ${{ needs.validate-release-input.outputs.environment }}')
        publishing.contains('ANNODOCIMAL_RELEASE_AUTHORIZED: ${{ inputs.stage }}')
        publishing.contains('SONATYPE_USERNAME')
        publishing.contains('SIGNING_KEY')
        publishing.contains('GRADLE_PUBLISH_KEY')
        publishing.contains('persist-credentials: false')
        !ordinaryWorkflow.contains('SONATYPE_USERNAME')
        !ordinaryWorkflow.contains('SIGNING_KEY')
        !ordinaryWorkflow.contains('GRADLE_PUBLISH_KEY')

        and: 'the root declares product coordinates while the convention owns guarded task wiring'
        String rootBuild = new File(repository, 'build.gradle').text
        String convention = new File(repository, 'buildSrc/src/main/groovy/annodocimal-release.conventions.gradle').text
        rootBuild.contains("id 'annodocimal-release.conventions'")
        rootBuild.contains('protectedRelease {')
        [
                ':anno-docimal-annotations:publishMavenJavaPublicationToSonatypeRepository',
                ':anno-docimal-apt:publishMavenJavaPublicationToSonatypeRepository',
                ':anno-docimal-ast:publishMavenJavaPublicationToSonatypeRepository',
                ':anno-docimal-global-ast:publishMavenJavaPublicationToSonatypeRepository',
                ':anno-docimal-generator:publishShadowPublicationToSonatypeRepository',
                ':anno-docimal-gradle-plugin:publishPluginMavenPublication'
        ].every { taskPath -> rootBuild.contains(taskPath) }
        rootBuild.contains(':anno-docimal-gradle-plugin:publishPlugins')
        !rootBuild.contains("tasks.register('verifyCompleteProductPublication')")
        convention.contains("tasks.register('verifyCompleteProductPublication')")
        convention.contains("tasks.register('publishCompleteProduct')")
        convention.contains('Remote publication must use publishCompleteProduct for the complete product')

        and: 'the runbook keeps Page authority, public resolve-back, tags, and CHANGES-derived releases outside this workflow'
        runbook.contains('`annodocimal-release-rc`')
        runbook.contains('`annodocimal-release-final`')
        runbook.contains('`annodocimal-pages-writer`')
        runbook.contains('credential-free public resolve-back')
        runbook.contains('GitHub Release body is copied or derived from the exact final `CHANGES.md` section')
        runbook.contains('It does not create a tag, GitHub Release, or public')
        runbook.contains('Pages snapshot.')
    }

    def 'rejects a Gradle publication without matching protected authorization'() {
        given: 'the repository-local Gradle release gate'
        File repository = new File(System.getProperty('annodocimal.repository.root'))
        Map<String, String> environment = new LinkedHashMap<>(System.getenv())
        environment.remove('ANNODOCIMAL_RELEASE_AUTHORIZED')

        when: 'an RC-shaped version lacks the protected authorization sentinel'
        def rejected = GradleRunner.create()
                .withProjectDir(repository)
                .withArguments('verifyProtectedReleaseAuthorization', '-Prelease.stage=rc', '-Prelease.version=1.0.0-rc.1')
                .withEnvironment(environment)
                .buildAndFail()

        then: 'the guard fails before any publication task can run'
        rejected.output.contains('Protected release authorization does not match -Prelease.stage')

        when: 'the exact protected sentinel and version are supplied'
        environment.put('ANNODOCIMAL_RELEASE_AUTHORIZED', 'rc')
        def accepted = GradleRunner.create()
                .withProjectDir(repository)
                .withArguments('verifyProtectedReleaseAuthorization', '-Prelease.stage=rc', '-Prelease.version=1.0.0-rc.1')
                .withEnvironment(environment)
                .build()

        then: 'the gate admits the identity without invoking a remote publication task'
        accepted.output.contains('BUILD SUCCESSFUL')
    }

    @Unroll
    def 'rejects #condition in the checked-in workflow preflight'() {
        given: 'one exact validation step copied from the checked-in workflow'
        File repository = new File(System.getProperty('annodocimal.repository.root'))
        String workflow = new File(repository, '.github/workflows/publish-protected-release.yml').text

        expect: 'the shell contract fails before it can reach a protected environment'
        runBash(workflowStep(workflow, step), environment) != 0

        where:
        condition                 | step                                                                        | environment
        'a non-lowercase SHA'     | 'Reject malformed release inputs'                                         | [RELEASE_STAGE: 'rc', RELEASE_VERSION: '1.0.0-rc.1', REVISION: 'A' * 40]
        'a stale master SHA'      | 'Require the requested source to be current master'                       | staleMasterEnvironment()
        'an existing release tag' | 'Reject a used tag or GitHub Release record'                              | existingTagEnvironment()
        'a mismatched Pages path' | 'Require the exact pending documentation handoff and absent public target' | [RELEASE_STAGE: 'rc', RELEASE_VERSION: '1.0.0-rc.1', REVISION: 'a' * 40, PENDING_DOCUMENTATION_PATH: 'pending/other']
    }

    @Unroll
    def 'rejects malformed #stage release identity #version before environment selection'() {
        given: 'the credential-free Gradle preflight'
        File repository = new File(System.getProperty('annodocimal.repository.root'))

        when:
        def rejected = GradleRunner.create()
                .withProjectDir(repository)
                .withArguments('verifyUnprivilegedReleaseInputs', "-Prelease.stage=$stage", "-Prelease.version=$version")
                .buildAndFail()

        then:
        rejected.output.contains(message)

        where:
        stage       | version        || message
        'candidate' | '1.0.0-rc.1'   || 'Set -Prelease.stage to exactly rc or final'
        'rc'        | '1.0.0'        || 'Release stage rc does not match exact version 1.0.0'
        'final'     | '1.0.0-rc.1'   || 'Release stage final does not match exact version 1.0.0-rc.1'
        'rc'        | '1.0.0-rc.0'   || 'Release stage rc does not match exact version 1.0.0-rc.0'
    }

    private static String job(String workflow, String name) {
        String marker = "  $name:\n"
        int start = workflow.indexOf(marker)
        assert start >= 0
        Matcher headings = Pattern.compile('(?m)^  [a-z][a-z-]+:$').matcher(workflow)
        int end = -1
        while (headings.find()) {
            if (headings.start() > start) {
                end = headings.start()
                break
            }
        }
        end < 0 ? workflow.substring(start) : workflow.substring(start, end)
    }

    private static String workflowStep(String workflow, String name) {
        int stepStart = workflow.indexOf("name: $name\n")
        assert stepStart >= 0
        String runMarker = '        run: |\n'
        int scriptStart = workflow.indexOf(runMarker, stepStart)
        assert scriptStart >= 0
        scriptStart += runMarker.length()
        int scriptEnd = workflow.indexOf('\n      - ', scriptStart)
        if (scriptEnd < 0)
            scriptEnd = workflow.indexOf('\n  ', scriptStart)
        String indentedScript = scriptEnd < 0 ? workflow.substring(scriptStart) : workflow.substring(scriptStart, scriptEnd)
        indentedScript.readLines().collect { line -> line.size() >= 10 ? line.substring(10) : line }.join('\n')
    }

    private static int runBash(String script, Map<String, String> environment) {
        ProcessBuilder process = new ProcessBuilder('bash', '-c', script)
        environment.each { key, value -> process.environment().put(key, value.toString()) }
        process.redirectErrorStream(true)
        process.start().waitFor()
    }

    private static Map<String, String> staleMasterEnvironment() {
        File bin = temporaryBin('stale-master')
        new File(bin, 'git').text = '''#!/usr/bin/env bash
case "$1:$2" in
  rev-parse:HEAD|rev-parse:*) printf '%s\\n' "$REVISION" ;;
  ls-remote:--exit-code) printf '%s\\trefs/heads/master\\n' bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb ;;
  status:--porcelain=v1) ;;
  *) exit 1 ;;
esac
'''
        new File(bin, 'git').setExecutable(true)
        [REVISION: 'a' * 40, PATH: "$bin:${System.getenv('PATH')}"]
    }

    private static Map<String, String> existingTagEnvironment() {
        File bin = temporaryBin('existing-tag')
        new File(bin, 'git').text = '''#!/usr/bin/env bash
[[ "$1" == show-ref ]]
'''
        new File(bin, 'git').setExecutable(true)
        [VERSION: '1.0.0-rc.1', GITHUB_TOKEN: 'fixture-token', PATH: "$bin:${System.getenv('PATH')}"]
    }

    private static File temporaryBin(String prefix) {
        File directory = File.createTempFile(prefix, '')
        assert directory.delete()
        assert directory.mkdir()
        directory.deleteOnExit()
        directory
    }
}
