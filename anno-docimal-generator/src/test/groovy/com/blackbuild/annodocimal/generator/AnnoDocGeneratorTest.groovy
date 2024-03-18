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
//file:noinspection GrMethodMayBeStatic
package com.blackbuild.annodocimal.generator

class AnnoDocGeneratorTest extends ClassGeneratingTest {

    String generatedSource
    SourceBlock generated

    def "basic conversion"() {
        given:
        createClass("""
            package dummy
            class TestClass {
                String field
                void method() {
                    println "Hello"
                }
            }
        """)

        when:
        generateSource()

        then:
        generated.packageName == "dummy"
        generated.imports == ["groovy.lang.GroovyObject", "java.lang.String"]
        generated.text == "public class TestClass implements GroovyObject"
        generated.innerBlocks.size() == 4
        generated.getBlock("public TestClass()")
        generated.getBlock("public void method()")
        generated.getBlock("public String getField()")
        generated.getBlock("public void setField(String arg0)")
    }

    def "basic test with generated documentation"() {
        given:
        createClass("""
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

        when:
        generateSource()

        then:
        generated.packageName == "dummy"
        generated.imports == ["groovy.lang.GroovyObject", "java.lang.String"]
        generated.text == "public class TestClass implements GroovyObject"
        generated.javaDoc == "This is a test class"
        generated.innerBlocks.size() == 4
        generated.getBlock("public TestClass()")
        generated.getBlock("public void method()").javaDoc == "This is a method"
        generated.getBlock("public String getField()")
        generated.getBlock("public void setField(String arg0)")
    }

    def "basic test with static inner class"() {
        given:
        createClass("""
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

        when:
        generateSource()

        then:
        generated.packageName == "dummy"
        generated.imports == ["groovy.lang.GroovyObject"]
        generated.text == "public class TestClass implements GroovyObject"
        generated.innerBlocks.size() == 3
        generated.getBlock("public TestClass()")
        generated.getBlock("public void method()")
        generated.getBlock("public static class InnerClass implements GroovyObject").innerBlocks.size() == 1
        generated.getBlock("public static class InnerClass implements GroovyObject").getBlock("public void innerMethod()")
    }

    void generateSource() {
        StringBuilder builder = new StringBuilder()
        AnnoDocGenerator.generate(clazz, builder)
        generatedSource = builder.toString()
        generated = SourceBlock.fromtext(generatedSource)
    }
}