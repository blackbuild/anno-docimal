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
package com.blackbuild.annodocimal.ast


import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

abstract class ClassGeneratingSpecification extends Specification {

    @Rule
    TestName testName = new TestName()
    ClassLoader oldLoader
    GroovyClassLoader loader
    CompilerConfiguration compilerConfiguration
    Class<?> clazz


    static Map<?, ?> astData = [:]

    def setup() {
        oldLoader = Thread.currentThread().contextClassLoader
        compilerConfiguration = new CompilerConfiguration()
        ImportCustomizer imports = new ImportCustomizer().addStarImports(getClass().getPackageName())
        compilerConfiguration.addCompilationCustomizers(imports)
        compilerConfiguration.optimizationOptions.groovydoc = Boolean.TRUE

        def outputDirectory = new File("build/test-classes/${getClass().simpleName}/$safeFilename")
        outputDirectory.deleteDir()
        outputDirectory.mkdirs()
        compilerConfiguration.targetDirectory = outputDirectory
        loader = new GroovyClassLoader(oldLoader, compilerConfiguration)
        Thread.currentThread().contextClassLoader = loader
    }

    def getSafeFilename() {
        testName.methodName.replaceAll("\\W+", "_")
    }

    def cleanup() {
        Thread.currentThread().contextClassLoader = oldLoader
        astData.clear()
    }

    Class<?> getClass(String className) {
        return loader.loadClass(className)
    }

    Class<?> createClass(@Language("groovy") String code) {
        clazz = loader.parseClass(code)
    }

    Class<?> createClassFromSecondaryloader(@Language("groovy") String code) {
        GroovyClassLoader secondaryLoader = new GroovyClassLoader(loader, compilerConfiguration)
        return secondaryLoader.parseClass(code)
    }

}