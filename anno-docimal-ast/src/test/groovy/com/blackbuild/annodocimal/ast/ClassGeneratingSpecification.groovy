package com.blackbuild.annodocimal.ast

import org.codehaus.groovy.control.CompilerConfiguration
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

    def setup() {
        oldLoader = Thread.currentThread().contextClassLoader
        compilerConfiguration = new CompilerConfiguration()
        //compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(new InlineJavadocsTransformation()))
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