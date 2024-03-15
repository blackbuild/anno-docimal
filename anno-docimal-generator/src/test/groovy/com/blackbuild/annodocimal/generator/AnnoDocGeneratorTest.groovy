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
package com.blackbuild.annodocimal.generator

class AnnoDocGeneratorTest extends ClassGeneratingTest {

    def "basic conversion"() {
        given:
        def clazz = createClass("""
            package dummy
            class TestClass {
                String field
                void method() {
                    println "Hello"
                }
            }
        """)
        StringBuilder output = new StringBuilder()

        when:
        AnnoDocGenerator.generate(clazz, output)
        def source = output.toString().readLines()
        def trimmed = source*.trim()

        then:
        noExceptionThrown()
        source.contains("package dummy;")
        source.find { it.startsWith("public class TestClass ") }
        source.find { it.contains("implements GroovyObject") }
        trimmed.contains( "public void method() {" )
    }

    def "basic test with generated documentation"() {
        given:
        def clazz = createClass("""
            package dummy
     
            import com.blackbuild.annodocimal.annotations.Javadocs
            
            @Javadocs("This is a test class")
            class TestClass {
                @Javadocs("This is a field")
                String field
                
                @Javadocs("This is a method")
                void method() {
                    println "Hello"
                }
            }
        """)
        StringBuilder output = new StringBuilder()

        when:
        AnnoDocGenerator.generate(clazz, output)

        def sourceText = output.toString()
        def source = sourceText.readLines()
        def trimmed = source*.trim()

        then:
        noExceptionThrown()
        source.contains("package dummy;")
        sourceText.contains '''/**
 * This is a test class
 */
public class TestClass'''

        source.find { it.contains("implements GroovyObject") }
        trimmed.contains( "public void method() {" )
    }

    def "basic test with static inner class"() {
        given:
        def clazz = createClass("""
            package dummy
     
            import com.blackbuild.annodocimal.annotations.Javadocs
            
            class TestClass {
                void method() {
                    println "Hello"
                }
                
                static class InnerClass {
                    void innerMethod() {
                        println "Hello"
                    }
                }
            }
        """)
        StringBuilder output = new StringBuilder()

        when:
        AnnoDocGenerator.generate(clazz, output)

        def sourceText = output.toString()
        def source = sourceText.readLines()
        def trimmed = source*.trim()

        then:
        noExceptionThrown()
        source.contains("package dummy;")
        trimmed.find { it.startsWith("public class TestClass") }
        trimmed.find { it.startsWith("public static class InnerClass") }
        trimmed.contains( "public void innerMethod() {" )
    }



}