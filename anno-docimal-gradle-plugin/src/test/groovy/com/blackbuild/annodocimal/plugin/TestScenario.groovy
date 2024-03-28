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
