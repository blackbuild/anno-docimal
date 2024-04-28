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

import org.apache.bcel.Repository
import org.apache.bcel.classfile.Utility
import org.apache.bcel.util.ClassPath
import org.apache.bcel.util.ClassPathRepository

class AnnoDocGeneratorTest extends ClassGeneratingTest {

    String generatedSource
    SourceBlock generated

    @Override
    def setup() {
        Repository.setRepository(new ClassPathRepository(new ClassPath(outputDirectory.absolutePath)))
    }

    @Override
    def cleanup() {
        Repository.clearCache()
    }

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
     
            import com.blackbuild.annodocimal.annotations.AnnoDoc
            
            @AnnoDoc("This is a test class")
            class TestClass {
                @AnnoDoc("This is a field")
                String field
                
                @AnnoDoc("This is a method")
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

    def "basic test with generated documentation and custom annotations"() {
        given:
        parseClass '''
package bummy

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.RUNTIME)
@interface MyAnnotation {
    String value()
}
'''

        createClass("""
            package dummy
     
            import bummy.MyAnnotation
            import com.blackbuild.annodocimal.annotations.AnnoDoc
            
            @AnnoDoc("This is a test class")
            @MyAnnotation("This is a custom annotation")
            class TestClass {
                @AnnoDoc("This is a field")
                @MyAnnotation("This is a custom annotation")
                String field
                
                @AnnoDoc("This is a method")
                @MyAnnotation("This is a custom annotation")
                void method() {
                    println "Hello"
                }
            }
        """)

        when:
        generateSource()

        then:
        generated.packageName == "dummy"
        generated.imports == ["bummy.MyAnnotation", "groovy.lang.GroovyObject", "java.lang.String"]
        generated.text == "public class TestClass implements GroovyObject"
        generated.javaDoc == "This is a test class"
        generated.innerBlocks.size() == 4
        generated.getBlock("public TestClass()")
        generated.getBlock("public void method()").javaDoc == "This is a method"
        generated.getBlock("public String getField()")
        generated.getBlock("public void setField(String arg0)")
    }

    def "basic annotation conversion"() {
        given:
        parseClass '''
package bummy

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@interface MyAnnotation {
    String name()
    Class<?> type()
    RetentionPolicy retention()
    ElementType[] targets()
    String[] names()
    Target anno()
    Target[] annos()
    int intValue()
    long longValue()
    float floatValue()
    double doubleValue()
    char charValue()
    boolean booleanValue()
    byte byteValue()
    short shortValue()
}
'''
        // since there is no way to specify a literal short/char/byte in groovy, we use MAX/MIN_VALUE
        createClass("""
            package dummy

import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
            
            @bummy.MyAnnotation(
                name = "Test",
                type = String.class,
                retention = RetentionPolicy.RUNTIME,
                targets = [ElementType.FIELD, ElementType.METHOD],
                names = ["a", "b"],
                anno = @Target(ElementType.FIELD),
                annos = [@Target(ElementType.FIELD), @Target(ElementType.METHOD)],
                intValue = 1,
                longValue = 2L,
                floatValue = 3.0f,
                doubleValue = 4.0d,
                charValue = Character.MIN_VALUE,
                booleanValue = true,
                byteValue = Byte.MAX_VALUE,
                shortValue = Short.MAX_VALUE
            )
            class TestClass {}
        """)

        when:
        generateSourceText()

        then:
        noExceptionThrown()
        generatedSource.contains('''@MyAnnotation(
    charValue = '\u0000',
    intValue = 1,
    name = "Test",
    floatValue = 3.0f,
    booleanValue = true,
    shortValue = 32767,
    doubleValue = 4.0,
    type = String.class,
    longValue = 2L,
    byteValue = 127,
    retention = RetentionPolicy.RUNTIME,
    anno = @Target({ElementType.FIELD}),
    names = {"a", "b"},
    targets = {ElementType.FIELD, ElementType.METHOD},
    annos = {@Target({ElementType.FIELD}), @Target({ElementType.METHOD})}
)''')
    }

    def "basic test with static inner class"() {
        given:
        createClass("""
            package dummy
     
            import com.blackbuild.annodocimal.annotations.AnnoDoc
            
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


        when:
        def innerClass = generated.getBlock("public static class InnerClass implements GroovyObject")

        then:
        innerClass.innerBlocks.size() == 2
        innerClass.getBlock("public InnerClass()")
        innerClass.getBlock("public void innerMethod()")
    }

    void generateSource() {
        generateSourceText()
        generated = SourceBlock.fromtext(generatedSource)
    }

    void generateSourceText() {
        StringBuilder builder = new StringBuilder()
        AnnoDocGenerator.generate(new File(outputDirectory, Utility.packageToPath(clazz.getName()) + ".class"), builder)
        generatedSource = builder.toString()
    }
}