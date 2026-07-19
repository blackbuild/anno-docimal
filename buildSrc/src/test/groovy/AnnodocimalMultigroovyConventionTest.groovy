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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

class AnnodocimalMultigroovyConventionTest extends Specification {

    @TempDir
    File projectDir

    def "compiles shared test inputs and records results in every Groovy lane"() {
        given:
        prepareProject()

        when:
        BuildResult result = run('verifyTestLaneIsolation').build()

        then:
        result.task(':verifyTestLaneIsolation').outcome.name() == 'SUCCESS'
        laneOutput('test', 'test', 'test').every { it.exists() }
        laneOutput('test-g4', 'groovy4Tests', 'groovy4Tests').every { it.exists() }
        laneOutput('test-g5', 'groovy5Tests', 'groovy5Tests').every { it.exists() }
    }

    def "rejects Groovy 3 compiled test output in a compatibility lane"() {
        given:
        prepareProject()
        file('build.gradle') << '''

dependencies {
    groovy4TestsImplementation sourceSets.test.output
}
'''

        when:
        BuildResult result = run('groovy4Tests').buildAndFail()

        then:
        result.output.contains('Groovy 4 source-set compile classpath contains another lane\'s output:')
        result.output.contains('build/classes/groovy/test')
        result.output.contains('build/classes/java/test')
        result.output.contains('build/resources/test')
    }

    private List<File> laneOutput(String groovyDirectory, String suiteName, String resultName) {
        return [
                file("build/classes/groovy/$groovyDirectory/example/LaneSpec.class"),
                file("build/classes/java/$suiteName/example/SharedHelper.class"),
                file("build/resources/$suiteName/shared.txt"),
                file("build/test-results/$resultName/TEST-example.LaneSpec.xml")
        ]
    }

    private void prepareProject() {
        file('settings.gradle').text = '''
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

gradle.beforeProject {
    it.ext.groovyVersion = 'v3'
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        libs {
            version('groovy-v3', '3.0.25')
            version('groovy-v4', '4.0.32')
            version('groovy-v5', '5.0.6')
            library('groovy-v3', 'org.codehaus.groovy', 'groovy-all').versionRef('groovy-v3')
            library('groovy-v4', 'org.apache.groovy', 'groovy-all').versionRef('groovy-v4')
            library('groovy-v5', 'org.apache.groovy', 'groovy-all').versionRef('groovy-v5')

            library('jb-anno', 'org.jetbrains', 'annotations').version('16.0.2')

            version('spock-g3', '2.4-groovy-3.0')
            version('spock-g4', '2.4-groovy-4.0')
            version('spock-g5', '2.4-groovy-5.0')
            library('spock-g3', 'org.spockframework', 'spock-core').versionRef('spock-g3')
            library('spock-junit4-g3', 'org.spockframework', 'spock-junit4').versionRef('spock-g3')
            library('spock-g4', 'org.spockframework', 'spock-core').versionRef('spock-g4')
            library('spock-junit4-g4', 'org.spockframework', 'spock-junit4').versionRef('spock-g4')
            library('spock-g5', 'org.spockframework', 'spock-core').versionRef('spock-g5')
            library('spock-junit4-g5', 'org.spockframework', 'spock-junit4').versionRef('spock-g5')
            bundle('spock-groovy-v3', ['spock-g3', 'spock-junit4-g3', 'groovy-v3'])

            library('bytebuddy', 'net.bytebuddy', 'byte-buddy').version('1.9.3')
            library('objenesis', 'org.objenesis', 'objenesis').version('2.6')
            library('jpl', 'org.junit.platform', 'junit-platform-launcher').version('1.9.2')
            bundle('spockRuntime', ['bytebuddy', 'objenesis'])
        }
    }
}

rootProject.name = 'test-project'
'''
        file('build.gradle').text = '''
plugins {
    id 'annodocimal-multigroovy.conventions'
}
'''
        file('src/test/groovy/example/LaneSpec.groovy').text = '''
package example

import spock.lang.Specification

class LaneSpec extends Specification {

    def "uses shared Java and resource inputs"() {
        expect:
        SharedHelper.value() == 'shared'
        getClass().getResource('/shared.txt').text.trim() == 'shared'
    }
}
'''
        file('src/test/java/example/SharedHelper.java').text = '''
package example;

public class SharedHelper {

    public static String value() {
        return "shared";
    }
}
'''
        file('src/test/resources/shared.txt').text = 'shared\n'
    }

    private GradleRunner run(String... arguments) {
        return GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(arguments + ['--stacktrace'])
                .withPluginClasspath()
    }

    private File file(String path) {
        File target = new File(projectDir, path)
        target.parentFile.mkdirs()
        return target
    }
}
