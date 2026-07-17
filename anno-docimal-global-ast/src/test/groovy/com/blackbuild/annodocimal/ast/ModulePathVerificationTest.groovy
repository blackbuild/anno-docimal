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

import spock.lang.Specification

import java.util.spi.ToolProvider

class ModulePathVerificationTest extends Specification {

    private static final Map<String, String> MODULE_NAMES = [
            annotations: 'com.blackbuild.annodocimal.annotations',
            apt        : 'com.blackbuild.annodocimal.apt',
            ast        : 'com.blackbuild.annodocimal.ast',
            globalAst  : 'com.blackbuild.annodocimal.global.ast',
            generator  : 'com.blackbuild.annodocimal.generator'
    ]

    def 'published module-path artifacts expose their stable automatic module names'() {
        expect:
        MODULE_NAMES.every { artifact, moduleName ->
            def result = runTool('jar', '--describe-module', '--file', moduleArtifact(artifact).absolutePath)
            result.exitCode == 0 && result.output.readLines().any { it.startsWith("$moduleName@") && it.endsWith(' automatic') }
        }
    }

    def 'global AST module owns the registered transformation provider'() {
        when:
        def result = runTool('jar', '--describe-module', '--file', moduleArtifact('globalAst').absolutePath)

        then:
        result.exitCode == 0
        result.output.contains('provides org.codehaus.groovy.transform.ASTTransformation with com.blackbuild.annodocimal.global.ast.InlineJavadocsGlobalTransformation')
    }

    def 'named consumer compiles and runs with annotation, AST, and global service integration'() {
        given:
        def consumerDirectory = File.createTempDir('annodocimal-module-consumer', '')
        def sourceDirectory = new File(consumerDirectory, 'com/example/consumer')
        sourceDirectory.mkdirs()
        new File(consumerDirectory, 'module-info.java').text = """
            module com.example.annodocimal.consumer {
                requires com.blackbuild.annodocimal.annotations;
                requires com.blackbuild.annodocimal.ast;
                requires com.blackbuild.annodocimal.global.ast;
                requires ${groovyModuleName()};
                uses org.codehaus.groovy.transform.ASTTransformation;
            }
        """.stripIndent()
        new File(sourceDirectory, 'Consumer.java').text = '''
            package com.example.consumer;

            import com.blackbuild.annodocimal.annotations.InlineJavadocs;
            import com.blackbuild.annodocimal.ast.InlineJavadocsTransformation;
            import org.codehaus.groovy.transform.ASTTransformation;

            import java.util.ServiceLoader;

            @InlineJavadocs
            public class Consumer {
                public static void main(String[] args) {
                    System.out.println(InlineJavadocsTransformation.class.getName());
                    ServiceLoader.load(ASTTransformation.class).stream()
                            .map(ServiceLoader.Provider::type)
                            .map(Class::getName)
                            .forEach(System.out::println);
                }
            }
        '''.stripIndent()
        def modulePath = [moduleArtifact('annotations'), moduleArtifact('ast'), moduleArtifact('globalAst'), groovyArtifact()].join(File.pathSeparator)
        def outputDirectory = new File(consumerDirectory, 'classes')

        when:
        def compilation = runTool('javac', '--module-path', modulePath, '-d', outputDirectory.absolutePath,
                new File(consumerDirectory, 'module-info.java').absolutePath, new File(sourceDirectory, 'Consumer.java').absolutePath)
        def execution = runJava(modulePath, outputDirectory)

        then:
        compilation.exitCode == 0
        execution.exitCode == 0
        execution.output.contains('com.blackbuild.annodocimal.ast.InlineJavadocsTransformation')
        execution.output.contains('com.blackbuild.annodocimal.global.ast.InlineJavadocsGlobalTransformation')

        cleanup:
        consumerDirectory.deleteDir()
    }

    private static File moduleArtifact(String artifact) {
        new File(System.getProperty("annodocimal.module.$artifact"))
    }

    private static File groovyArtifact() {
        new File(GroovySystem.protectionDomain.codeSource.location.toURI())
    }

    private static String groovyModuleName() {
        def result = runTool('jar', '--describe-module', '--file', groovyArtifact().absolutePath)
        assert result.exitCode == 0: result.output
        result.output.readLines().find { it.endsWith(' automatic') }.tokenize('@')[0]
    }

    private static ToolResult runTool(String toolName, String... arguments) {
        def output = new StringWriter()
        int exitCode = ToolProvider.findFirst(toolName).orElseThrow().run(new PrintWriter(output), new PrintWriter(output), arguments)
        new ToolResult(exitCode, output.toString())
    }

    private static ToolResult runJava(String modulePath, File outputDirectory) {
        def process = new ProcessBuilder("${System.getProperty('java.home')}/bin/java", '--module-path', "$modulePath${File.pathSeparator}${outputDirectory.absolutePath}",
                '-m', 'com.example.annodocimal.consumer/com.example.consumer.Consumer').redirectErrorStream(true).start()
        new ToolResult(process.waitFor(), process.inputStream.text)
    }

    private static class ToolResult {
        final int exitCode
        final String output

        ToolResult(int exitCode, String output) {
            this.exitCode = exitCode
            this.output = output
        }
    }
}
