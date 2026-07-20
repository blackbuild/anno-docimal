/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 Stephan Pauxberger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.blackbuild.annodocimal.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

class AnnoDocimalPluginTest extends Specification {

    @Shared File scenarioRoot = new File("src/test/scenarios").absoluteFile
    @Shared File target = new File("build/test-scenarios").absoluteFile

    TestScenario scenario

    @TempDir
    File testProjectDir

    def "test scenario #name"(String name) {
        given:
        scenario = new TestScenario(name, scenarioRoot, target).prepareScenario()

        when:
        def result = runTask()

        then:
        noExceptionThrown()
        scenario.outputMatchesExpectations()

        where:
        name << scenarioRoot.listFiles().findAll { it.isDirectory() }.collect { it.name }
    }

    protected BuildResult runTask() {
        return GradleRunner.create()
                .withProjectDir(scenario.projectDir)
                .withArguments(scenario.tasks)
                .withDebug(true)
                .withPluginClasspath()
                .forwardOutput()
                .build()
    }

    def "separately registered source mirror selects documented top-level classes"() {
        given:
        prepareMirrorProject()

        when:
        runMirrorTask('sourceMirror')

        then:
        def source = new File(testProjectDir, 'build/source-mirror/example/Widget_DSL.java').text
        source.contains('DSL documentation')
        source.contains('class Nested')
        !new File(testProjectDir, 'build/source-mirror/example/Unrelated.java').exists()
    }

    def "source mirror removes stale output after a selected class disappears"() {
        given:
        prepareMirrorProject()
        runMirrorTask('sourceMirror')
        new File(testProjectDir, 'build/classes/java/main/example/Widget_DSL.class').delete()

        when:
        runMirrorTask('sourceMirror', '-x', 'compileJava')

        then:
        !new File(testProjectDir, 'build/source-mirror/example/Widget_DSL.java').exists()
    }

    def "source mirror is up-to-date and restores its managed directory from the build cache"() {
        given:
        prepareMirrorProject()
        runMirrorTask('sourceMirror', '--build-cache')

        when:
        def upToDate = runMirrorTask('sourceMirror', '--build-cache')
        testProjectDir.toPath().resolve('build/source-mirror').toFile().deleteDir()
        def restored = runMirrorTask('sourceMirror', '--build-cache')

        then:
        upToDate.output.contains(':sourceMirror UP-TO-DATE')
        restored.output.contains(':sourceMirror FROM-CACHE')
    }

    def "source mirror stores and reuses the strict configuration cache"() {
        given:
        prepareMirrorProject()

        when:
        runMirrorTask('sourceMirror', '--configuration-cache')
        def reused = runMirrorTask('sourceMirror', '--configuration-cache')

        then:
        reused.output.contains('Reusing configuration cache.')
    }

    def "AnnoDoc-only class content changes invalidate the source mirror"() {
        given:
        prepareMirrorProject()
        runMirrorTask('sourceMirror')
        def source = new File(testProjectDir, 'src/main/java/example/Widget_DSL.java')
        source.text = source.text.replace('DSL documentation', 'Changed DSL documentation')

        when:
        def result = runMirrorTask('sourceMirror')

        then:
        result.output.contains(':sourceMirror')
        !result.output.contains(':sourceMirror UP-TO-DATE')
        new File(testProjectDir, 'build/source-mirror/example/Widget_DSL.java').text.contains('Changed DSL documentation')
    }

    def "the documented Gradle minimum supports the reusable task and configuration cache"() {
        given:
        prepareMirrorProject()

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('sourceMirror', '--configuration-cache')
                .withGradleVersion('7.3.3')
                .withPluginClasspath()
                .build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    private BuildResult runMirrorTask(String... arguments) {
        GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments(arguments)
                .withPluginClasspath()
                .build()
    }

    private void prepareMirrorProject() {
        new File(testProjectDir, 'settings.gradle').text = "rootProject.name = 'source-mirror-test'"
        new File(testProjectDir, 'build.gradle').text = """
            plugins {
                id 'java'
                id 'com.blackbuild.annodocimal.groovy-plugin'
            }

            repositories { mavenCentral() }

            dependencies {
                implementation files('${System.getProperty('anno.docimal.annotations.jar')}')
            }

            tasks.register('sourceMirror', com.blackbuild.annodocimal.plugin.SourceProjectionTask) {
                classesDirectories.from(sourceSets.main.output.classesDirs)
                includes.add('**/*_DSL.class')
                outputDirectory.set(layout.buildDirectory.dir('source-mirror'))
            }
        """.stripIndent()
        def sourceDirectory = new File(testProjectDir, 'src/main/java/example')
        sourceDirectory.mkdirs()
        new File(sourceDirectory, 'Widget_DSL.java').text = '''
            package example;

            import com.blackbuild.annodocimal.annotations.AnnoDoc;

            @AnnoDoc("DSL documentation")
            public class Widget_DSL {
                public static class Nested {
                }
            }
        '''.stripIndent()
        new File(sourceDirectory, 'Unrelated.java').text = '''
            package example;

            public class Unrelated {
            }
        '''.stripIndent()
    }
}
