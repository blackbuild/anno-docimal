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
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

@Issue('#44')
class LocalPublicationContractTest extends Specification {

    private static final List<String> ARTIFACTS = [
            'anno-docimal-annotations',
            'anno-docimal-apt',
            'anno-docimal-ast',
            'anno-docimal-global-ast',
            'anno-docimal-generator',
            'anno-docimal-gradle-plugin'
    ]

    private static final List<String> PLUGIN_IDS = [
            'com.blackbuild.annodocimal.base-plugin',
            'com.blackbuild.annodocimal.groovy-plugin'
    ]

    @Shared Path repository = Path.of(System.getProperty('annodocimal.publication.repository'))
    @Shared String version = System.getProperty('annodocimal.publication.version')

    def 'clean local repository contains the complete publication product'() {
        expect:
        ARTIFACTS.every { artifact ->
            def directory = moduleDirectory('com.blackbuild.annodocimal', artifact)
            Files.isRegularFile(directory.resolve("${artifact}-${version}.jar")) &&
                    Files.isRegularFile(directory.resolve("${artifact}-${version}.pom")) &&
                    Files.isRegularFile(directory.resolve("${artifact}-${version}.module")) &&
                    Files.isRegularFile(directory.resolve("${artifact}-${version}-sources.jar")) &&
                    Files.isRegularFile(directory.resolve("${artifact}-${version}-javadoc.jar"))
        }

        and:
        PLUGIN_IDS.every { pluginId ->
            def artifact = "${pluginId}.gradle.plugin"
            def directory = moduleDirectory(pluginId, artifact)
            Files.isRegularFile(directory.resolve("${artifact}-${version}.pom"))
        }
    }

    private Path moduleDirectory(String group, String artifact) {
        repository.resolve(group.replace('.', '/')).resolve(artifact).resolve(version)
    }
}
