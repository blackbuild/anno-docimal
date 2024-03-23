package com.blackbuild.annodocimal.plugin

/**
 * Simple data structure to makle scenario tests better to use
 */
class TestScenario {

    String name
    File sourceDir
    File projectDir

    TestScenario(String name, File scenarioRoot, File testProjectsRoot) {
        this.name = name
        sourceDir = new File(scenarioRoot, name)
        projectDir = new File(testProjectsRoot, this.name)
    }

    List<String> getTasks() {
        return ["javadoc"]
    }

    TestScenario prepareScenario() {
        File scenario = new File(sourceDir, "project")
        projectDir.deleteDir()
        projectDir.mkdirs()
        scenario.eachFileRecurse { file ->
            if (file.isFile()) {
                File targetFile = new File(projectDir, scenario.relativePath(file))
                targetFile.parentFile.mkdirs()
                targetFile.text = file.text.replace("%%anno.docimal.ast.jar%%", System.getProperty("anno.docimal.ast.jars"))
                        .replace("%%anno.docimal.annotations.jar%%", System.getProperty("anno.docimal.annotations.jar"))
            }
        }
        def settingsFile = new File(projectDir, "settings.gradle")
        if (!settingsFile.exists())
            settingsFile.text = "rootProject.name = '$name'"

        return this
    }

    void outputMatchesExpectations() {
        def expectedDir = new File(sourceDir, "expected")
        expectedDir.eachFileRecurse { file ->
            if (file.isFile()) {
                File targetFile = new File(projectDir, expectedDir.relativePath(file))
                assert targetFile.isFile()
                if (file.size() != 0L)
                    assert targetFile.text == file.text
            }
        }
    }
}
