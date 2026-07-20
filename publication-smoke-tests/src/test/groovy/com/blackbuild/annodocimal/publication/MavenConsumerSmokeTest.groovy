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
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

import static com.blackbuild.annodocimal.publication.ConsumerFixtureSupport.copyFixture
import static com.blackbuild.annodocimal.publication.ConsumerFixtureSupport.replaceTokens

@Issue('44')
class MavenConsumerSmokeTest extends Specification {

    @TempDir Path target

    def 'clean Maven POM consumer resolves and compiles against all six artifacts offline'() {
        given:
        copyFixture(Path.of('src/test/fixtures/maven'), target)
        replaceTokens(target.resolve('pom.xml'))
        def localRepository = target.resolve('.m2/repository')
        copyFixture(Path.of(System.getProperty('annodocimal.publication.repository')), localRepository)

        when:
        def process = new ProcessBuilder(
                System.getProperty('annodocimal.maven.executable'),
                '--batch-mode',
                '--no-transfer-progress',
                '--offline',
                "-Dmaven.repo.local=${localRepository}",
                'org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile')
                .directory(target.toFile())
                .redirectErrorStream(true)
                .start()
        def output = process.inputStream.getText('UTF-8')
        def exitCode = process.waitFor()

        then:
        exitCode == 0
        Files.isRegularFile(target.resolve('target/classes/consumer/MavenArtifactConsumer.class'))

        cleanup:
        if (exitCode != null && exitCode != 0)
            System.err.println(output)
    }

}
