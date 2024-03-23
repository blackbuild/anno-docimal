package com.blackbuild.annodocimal.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification

class AnnoDocimalPluginTest extends Specification {

    @Shared File scenarioRoot = new File("src/test/scenarios")
    @Shared File target = new File("build/test-scenarios")

    TestScenario scenario

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
}