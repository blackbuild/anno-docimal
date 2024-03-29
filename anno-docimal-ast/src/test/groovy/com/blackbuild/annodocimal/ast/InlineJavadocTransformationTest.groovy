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
//file:noinspection GrPackage
package com.blackbuild.annodocimal.ast

import com.blackbuild.annodocimal.annotations.Javadocs
import org.codehaus.groovy.control.CompilerConfiguration
import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

class InlineJavadocTransformationTest extends Specification {
    @Rule TestName testName = new TestName()
    ClassLoader oldLoader
    GroovyClassLoader loader
    CompilerConfiguration compilerConfiguration
    Class<?> clazz
    File srcDir

    def setup() {
        oldLoader = Thread.currentThread().contextClassLoader
        compilerConfiguration = new CompilerConfiguration()
        //compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(new InlineJavadocTransformation()))
        def outputDirectory = new File("build/test-classes/${getClass().simpleName}/$safeFilename")
        outputDirectory.deleteDir()
        outputDirectory.mkdirs()
        srcDir = new File("build/test-sources/${getClass().simpleName}/$safeFilename")
        srcDir.deleteDir()
        srcDir.mkdirs()
        compilerConfiguration.targetDirectory = outputDirectory
        loader = new GroovyClassLoader(oldLoader, compilerConfiguration)
        Thread.currentThread().contextClassLoader = loader
    }

    def getSafeFilename() {
        testName.methodName.replaceAll("\\W+", "_")
    }

    def cleanup() {
        Thread.currentThread().contextClassLoader = oldLoader
    }

    void createClass(String filename, @Language("groovy") String code) {
        File file = new File(srcDir, filename)
        file.parentFile.mkdirs()
        file.text = code
        clazz = loader.parseClass(file)
    }

    def "test simple transformation"() {
        when:
        createClass "dummy/TestClass.groovy", '''
package dummy

import com.blackbuild.annodocimal.annotations.InlineJavadocs

/**
 * This is a test class
 */
@InlineJavadocs
class TestClass {

    /** A name */
    String name
    
    /**
     * A method
     */ 
    void method() {}
    
    /**
     * Inner class
     */
    static class InnerClass {
        /**
         * Inner class method
         */
        void innerMethod() {}
    }
}
'''

        then:
        clazz.getAnnotation(Javadocs).value() == 'This is a test class'
        clazz.getMethod("method").getAnnotation(Javadocs).value() == 'A method'
        clazz.getDeclaredField("name").getAnnotation(Javadocs).value() == 'A name'

        when:
        def innerClass = clazz.getDeclaredClasses().find { it.simpleName == "InnerClass" }

        then:
        innerClass.getAnnotation(Javadocs).value() == 'Inner class'
        innerClass.getMethod("innerMethod").getAnnotation(Javadocs).value() == 'Inner class method'
    }


}