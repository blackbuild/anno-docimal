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

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

import static com.blackbuild.annodocimal.publication.ConsumerFixtureSupport.copyFixture
import static com.blackbuild.annodocimal.publication.ConsumerFixtureSupport.replaceTokens

@Issue('44')
class GradleConsumerSmokeTest extends Specification {

    @TempDir Path target

    def 'clean Gradle consumer resolves all artifacts and both plugin markers offline'() {
        given:
        copyFixture(Path.of('src/test/fixtures/gradle'), target)
        replaceTokens(target.resolve('settings.gradle'))
        Files.walk(target).withCloseable { files ->
            files.filter { it.fileName.toString() == 'build.gradle' }.forEach { replaceTokens(it) }
        }

        when:
        def result = GradleRunner.create()
                .withProjectDir(target.toFile())
                .withTestKitDir(target.resolve('.gradle-test-kit').toFile())
                .withArguments('exerciseConsumer', '--offline', '--stacktrace')
                .build()

        then:
        result.task(':exerciseConsumer').outcome == TaskOutcome.SUCCESS
        result.task(':artifacts:exerciseArtifacts').outcome == TaskOutcome.SUCCESS
        result.task(':base-plugin:exercisePlugin').outcome == TaskOutcome.SUCCESS
        result.task(':groovy-plugin:exercisePlugin').outcome == TaskOutcome.SUCCESS
    }

}
