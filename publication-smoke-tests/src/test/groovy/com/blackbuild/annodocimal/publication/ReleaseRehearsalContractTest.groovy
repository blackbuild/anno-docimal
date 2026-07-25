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

import spock.lang.Issue
import spock.lang.See
import spock.lang.Specification
import spock.lang.Tag

@Issue('45')
@Tag('documentary')
@See('https://github.com/blackbuild/anno-docimal/blob/master/RELEASING.md#local-non-publishing-rehearsal')
class ReleaseRehearsalContractTest extends Specification {

    def 'demonstrates the local non-publishing rehearsal'() {
        given:
        File repository = new File(System.getProperty('annodocimal.repository.root'))
        String runbook = new File(repository, 'RELEASING.md').text
        String build = new File(repository, 'build.gradle').text

        expect: 'the runbook derives a release from one clean exact master commit and projected changelog section'
        runbook.contains('`git rev-parse HEAD`')
        runbook.contains('`v<version>`')
        runbook.contains('`## <version> (unreleased)`')
        runbook.contains('immediately before the first actual RC build')

        and: 'the complete public product and immutable documentation paths are named rather than implied'
        runbook.contains('anno-docimal-annotations')
        runbook.contains('anno-docimal-gradle-plugin')
        runbook.contains('com.blackbuild.annodocimal.base-plugin')
        runbook.contains('com.blackbuild.annodocimal.groovy-plugin')
        runbook.contains('`/pending/<version>/<full-source-sha>/`')
        runbook.contains('`/<version>/`')
        runbook.contains('`/archive/`')

        and: 'no pre-publication evidence advances an alias or status, and public proof precedes the advance'
        runbook.contains('public-artifact proof')
        runbook.contains('must not advance an alias or public status')
        runbook.contains('credential-free public resolve-back')

        and: 'partial publication burns a version while safe retries remain local or idempotent'
        runbook.contains('burned version')
        runbook.contains('Safe retry')
        runbook.contains('new version')

        and: 'the local rehearsal composes only safe local validation and writes durable local evidence'
        build.contains("tasks.register('releaseRehearsal')")
        build.contains("tasks.register('verifyReleaseRehearsalInputs')")
        build.contains("providers.gradleProperty('release.version')")
        build.contains('does not match the configured build version')
        build.contains("dependsOn tasks.named('check')")
        build.contains("dependsOn tasks.named('renderLocalDocumentation')")
        build.contains("'release-rehearsal/evidence.md'")
        runbook.contains('-Prelease.version=1.0.0-rc.1')
        runbook.contains('cannot tag, upload, dispatch, publish documentation, change environments, or mutate GitHub')
    }
}
