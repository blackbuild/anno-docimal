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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.blackbuild.annodocimal.publication

import spock.lang.Issue
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

@Issue('#44')
class PublicRcFixtureContractTest extends Specification {

    def 'public RC fixtures expose no local or composite fallback'() {
        when:
        def settings = [
                Path.of('src/publicRc/gradle-settings.gradle'),
                Path.of('src/publicRc/maven-settings.gradle')
        ].collect(Files::readString).join('\n')

        then:
        settings.contains('gradlePluginPortal()')
        settings.count('mavenCentral') == 2
        !settings.contains('mavenLocal')
        !settings.contains('includeBuild')
        !settings.contains('flatDir')
        !settings.contains('url =')
    }

    def 'public RC templates enumerate the complete exact-version product'() {
        when:
        def gradleFixture = Files.walk(Path.of('src/test/fixtures/gradle')).withCloseable { files ->
            files.filter { Files.isRegularFile(it) }
                    .collect { Files.readString(it) }
                    .join('\n')
        }
        def mavenPom = Files.readString(Path.of('src/test/fixtures/maven/pom.xml'))

        then:
        [
                'anno-docimal-annotations',
                'anno-docimal-apt',
                'anno-docimal-ast',
                'anno-docimal-global-ast',
                'anno-docimal-generator',
                'anno-docimal-gradle-plugin'
        ].every { artifact ->
            gradleFixture.contains("${artifact}:%%VERSION%%") &&
                    mavenPom.contains("<artifactId>${artifact}</artifactId>") &&
                    mavenPom.contains('<version>%%VERSION%%</version>')
        }
        gradleFixture.contains("id 'com.blackbuild.annodocimal.base-plugin' version '%%VERSION%%'")
        gradleFixture.contains("id 'com.blackbuild.annodocimal.groovy-plugin' version '%%VERSION%%'")
    }
}
