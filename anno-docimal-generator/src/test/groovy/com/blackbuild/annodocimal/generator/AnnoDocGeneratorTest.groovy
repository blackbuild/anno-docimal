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


import spock.lang.IgnoreIf
import spock.lang.Requires
import spock.lang.Unroll

@Unroll
class AnnoDocGeneratorTest extends ClassGeneratingTest {

    String generatedSource
    SourceBlock generated

    @Override
    def setup() {
        // Repository.setRepository(new ClassPathRepository(new ClassPath(outputDirectory.absolutePath)))
    }

    @Override
    def cleanup() {
        // Repository.clearCache()
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
        generated.imports == ["java.lang.String"]
        generated.text == "public class TestClass"
        generated.getBlock("public TestClass()")
        generated.getBlock("public void method()")
        generated.getBlock("public String getField()")
        generated.getOneOf("public void setField(String *)", "param0", "value")
    }

    def "class conversion"() {
        given:
        createClass("""
            package dummy
            abstract class $signature {}
        """)

        when:
        generateSource()

        then:
        generated.packageName == "dummy"
        generated.strip("public abstract class") == signature

        where:
        signature << [
                "TestClass",
                "Test<T extends Number>",
                "Test implements List<? super Number>",
                "Test implements List<?>",
                "Test<T, I extends Number> implements Map<I, List<T>>, Serializable",
                "Test<T>",
                "Test<T> implements List<List<T>>"
        ]
    }

    def "basic enum conversion"() {
        given:
        createClass("""
            package dummy
            enum MyEnum {
                A, B, C
            }
        """)

        when:
        generateSourceText()

        then:
        generatedSource ==~ ~/package dummy;\s*public enum MyEnum \{\s*A,\s*B,\s*C\s*}\s*/
    }

    def "basic enum conversion with implements and generics"() {
        given:
        createClass("""
            package dummy
            enum $signature {
                A, B, C
            }
        """)

        when:
        generateSourceText()

        then:
        generatedSource.contains("public enum $signature {")

        where:
        signature << [
                "MyEnum",
                "MyEnum implements Serializable",
                "MyEnum implements GenericsHolder<String>"
        ]
    }

    def "enum conversion with annotations"() {
        given:
        createClass("""
            package dummy

            import dummyanno.WithPrimitives
            
            @WithPrimitives
            enum MyEnum {
                @WithPrimitives(intValue = 1) A, 
                @WithPrimitives(intValue = 1) B, 
                @WithPrimitives(intValue = 1) C
            }
        """)

        when:
        generateSourceText()

        then:
        generatedSource.find(annoStringToRegexp("@WithPrimitives public enum MyEnum"))
        generatedSource.find(annoStringToRegexp("@WithPrimitives(intValue = 1) A,"))
        generatedSource.find(annoStringToRegexp("@WithPrimitives(intValue = 1) B,"))
        generatedSource.find(annoStringToRegexp("@WithPrimitives(intValue = 1) C"))
    }

    def "enum conversion with annotations and javadoc"() {
        given:
        createClass("""
            package dummy

import com.blackbuild.annodocimal.annotations.AnnoDoc
import dummyanno.WithPrimitives
            
            @WithPrimitives
            @AnnoDoc("This is a test enum")
            enum MyEnum {
                @AnnoDoc("Value A") @WithPrimitives(intValue = 1) A, 
                @AnnoDoc("Value B") @WithPrimitives(intValue = 1) B, 
                @AnnoDoc("Value C") @WithPrimitives(intValue = 1) C
            }
        """)

        when:
        generateSourceText()

        then:
        generatedSource.find(toJavadoc("This is a test enum") + annoStringToRegexp(" @WithPrimitives public enum MyEnum"))
        generatedSource.find(toJavadoc("Value A") + annoStringToRegexp(" @WithPrimitives(intValue = 1) A,"))
        generatedSource.find(toJavadoc("Value B") + annoStringToRegexp(" @WithPrimitives(intValue = 1) B,"))
        generatedSource.find(toJavadoc("Value C") + annoStringToRegexp(" @WithPrimitives(intValue = 1) C"))
    }

    String toJavadoc(String text) {
        return ("/\\*\\*\\s*" + text.readLines().collect { "\\*\\s*$it" }.join("\\s*") + "\\s*\\*\\/")
    }

    def "interface conversion"() {
        given:
        createClass("""
            package dummy
            interface $signature {}
        """)

        when:
        generateSource()

        then:
        generated.packageName == "dummy"
        generated.strip("public interface") == signature

        where:
        signature << [
                "TestInterface",
                "TestInterface<T extends Number>",
                "TestInterface extends List<? super Number>",
                "TestInterface extends List<?>",
                "TestInterface<T, I extends Number> extends Map<I, List<T>>, Serializable",
                "TestInterface<T>",
                "TestInterface<T> extends List<List<T>>"
        ]
    }

    def "annotation conversion"() {
        given:
        createClass("""
            package dummy

            import dummyanno.WithPrimitives
            
            @WithPrimitives
            @interface MyAnnotation {
                @WithPrimitives int intValue() default 0
                String stringValue()
            }
        """)

        when:
        generateSourceText()

        then:
        generatedSource == '''package dummy;

import dummyanno.WithPrimitives;
import java.lang.String;

@WithPrimitives
public @interface MyAnnotation {
  @WithPrimitives
  int intValue() default 0;

  String stringValue();
}
'''
    }

    def "method conversion"() {
        given:
        createClass("""
            package dummy
            abstract class Dummy$generics {
                $signature${signature.endsWith("{") ? "}" : ""}
            }
        """)

        when:
        generateSourceText()

        then:
        generatedSource.contains(adjustSignature(signature, params))

        where:
        signature                                     | params    | generics
        "void method() {"                             | ""        | ""
        "void method(String aString) {"               | "aString" | ""
        "void method(T aParam) {"                     | "aParam"  | "<T>"
        "void method() throws Exception {"            | ""        | ""
        "public <T extends List> T method(T input) {" | "input"   | ""
        "abstract void method()"                      | ""        | ""
    }

    def "interface method conversion"() {
        given:
        createClass("""
            package dummy
            interface Dummy$generics {
                $signature
            }
        """)

        when:
        generateSourceText()

        then:
        generatedSource.contains(adjustSignature(signature, params))

        where:
        signature                                   | params    | generics
        "void method()"                             | ""        | ""
        "void method(String aString)"               | "aString" | ""
        "void method(T aParam)"                     | "aParam"  | "<T>"
        "void method() throws Exception"            | ""        | ""
    }

    @IgnoreIf({ GroovySystem.version.startsWith("2.") })
    def "interface method conversion G3"() {
        given:
        createClass("""
            package dummy
            interface Dummy {
                default String method() {}
            }
        """)

        when:
        generateSourceText()

        then:
        generatedSource == '''package dummy;

import groovy.transform.Trait;
import java.lang.String;

@Trait
public interface Dummy {
  default String method() {
  }
}
'''
    }

    def "method conversion with param annotation"() {
        given:
        createClass("""
            package dummy

            import dummyanno.*
            import java.lang.annotation.* 

            abstract class Dummy$generics {
                $signature {}
            }
        """)

        when:
        generateSource()

        then:
        generatedSource.contains(signature)

        where:
        signature                                                  | generics
        "void method(@WithPrimitives(intValue = 5) String param0)" | ""
    }

    def "private methods are ignored"() {
        given:
        createClass("""
            package dummy

            import groovy.transform.PackageScope
            abstract class Dummy {
                private void privateMethod() {}
                protected void protectedMethod() {}
                @PackageScope void packageMethod() {}
                void publicMethod() {}
            }
        """)

        when:
        generateSource()

        then:
        !generatedSource.contains("privateMethod()")
        generatedSource.contains("protected void protectedMethod()")
        generatedSource.contains("void packageMethod()")
        generatedSource.contains("public void publicMethod()")
    }

    def "method conversion with annotations"() {
        given:
        createClass("""
            package dummy
            
            import dummyanno.*
            import java.lang.annotation.* 

            abstract class Dummy {
                $anno void method() {}
            }
        """)

        when:
        generateSourceText()

        then:
        generatedSource.find(annoStringToRegexp(anno + " public void method"))

        where:
        anno << [ // Groovy has problems with byte, char, double, short literals as anno members
                  "@WithPrimitives",
                  "@WithPrimitives(intValue = 1)",
                  "@WithPrimitives(booleanValue = true)",
                  "@WithPrimitives(floatValue = 1.0f)",
                  "@WithPrimitives(longValue = 1L)",
                  '@WithPrimitives(stringValue = "bla")',
                  '@WithPrimitives(intArray = [1, 2])',
                  '@WithPrimitives(classValue = ArrayList.class)',
                  "@WithPrimitives(intValue = 1, booleanValue = true)",
                  "@WithEnum(RetentionPolicy.RUNTIME)",
                  "@Nested(primitiveValue = @WithPrimitives)",
                  "@Nested(primitiveValue = @WithPrimitives(intValue = 1))",
                  "@Nested(primitiveValue = @WithPrimitives(intValue = 1, booleanValue = true))",
                  "@Nested(enumValue = @WithEnum(RetentionPolicy.RUNTIME))",
                  "@Nested(enumValue = @WithEnum(RetentionPolicy.RUNTIME), primitiveValue = @WithPrimitives(intValue = 1))",
                  "@Nested(enumValue = @WithEnum(RetentionPolicy.RUNTIME), primitiveValue = @WithPrimitives(intValue = 1, booleanValue = true))",
                  "@Nested(primitivesArray = [@WithPrimitives(intValue = 1), @WithPrimitives(intValue = 2)])",
        ]
    }

    @Requires({ GroovySystem.version.startsWith("5.") })
    def "method conversion with annotations G5"() {
        // TODO: Move special cases to java tests
        given:
        createClass("""
            package dummy
            
            import dummyanno.*

            abstract class Dummy {
                $anno void method() {}
            }
        """)

        when:
        generateSourceText()

        then:
        generatedSource.find(annoStringToRegexp(anno + " public void method"))

        where:
        anno << [ // Groovy has problems with byte, char, double, short
                  "@WithPrimitives(byteValue = 1 as byte)",
                  "@WithPrimitives(charValue = 'a')",
                  "@WithPrimitives(doubleValue = 1.0)",
                  "@WithPrimitives(shortValue = 1)",
        ]
    }

    String annoStringToRegexp(String anno) {
        anno.replace("(", "\\s*\\(\\s*")
                .replace(")", "\\s*\\)\\s*")
                .replace(",", "\\s*,\\s*")
                .replace(" ", "\\s*")
                .replace("[", "\\{")
                .replace("]", "\\}")
                .replace("\\s*\\s*", "\\s*")
    }


    def "field conversion"() {
        given:
        createClass("""
            package dummy
            import dummyanno.*

            class Dummy$generics {
                $signature
            }
        """)

        when:
        generateSourceText()

        then:
        generatedSource.find(annoStringToRegexp(signature))

        where:
        signature                                        | generics
        "public int field"                               | ""
        "public Object field"                            | ""
        "public T field"                                 | "<T>"
        "@WithPrimitives public int field"               | ""
        "@WithPrimitives(intValue = 5) public int field" | ""
    }

    def "method conversion with generic exceptions"() {
        given:
        createClass("""
            package dummy
            abstract class Dummy<T extends Exception> {
                void method() throws T {}
            }
        """)

        when:
        generateSource()

        then: "Groovy erases the generic type of the exception"
        generatedSource.contains("void method() throws Exception")
    }

    private String adjustSignature(String signature, String params) {
        if (!GroovySystem.version.startsWith("2."))
            return signature

        // replace parameter names with param0, param1, ...
        params.tokenize(",").eachWithIndex { param, index ->
            signature = signature.replaceFirst(param, "param$index")
        }

        signature.replaceAll("Dummy", "TestClass")
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
        generated.imports == ["java.lang.String"]
        generated.text == "public class TestClass"
        generated.javaDoc == "This is a test class"
        generated.innerBlocks.size() == 4
        generated.getBlock("public TestClass()")
        generated.getBlock("public void method()").javaDoc == "This is a method"
        generated.getBlock("public String getField()")
        generated.getBlock("public void setField(String param0)") || generated.getBlock("public void setField(String value)")
    }

    @IgnoreIf({ GroovySystem.version.startsWith("2.") })
    def "bug: visibility is off"() {
        given:
        createClass("""
            package dummy
     
            import com.blackbuild.annodocimal.annotations.AnnoDoc
            
            class TestClass {
                String field
                
                void method() {
                    println "Hello"
                }
                
                private void privateMethod() {
                    println "Hello"
                }
                
                protected void protectedMethod() {
                    println "Hello"
                }
                
                protected static class InnerClass {
                    void innerMethod() {
                        println "Hello"
                    }
                }
            }
        """)

        when:
        generateSourceText()

        then:
        generatedSource.contains("protected static class InnerClass")
    }

    def "basic test with generated documentation and bad params"() {
        given:
        createClass("""
            package dummy
     
            import com.blackbuild.annodocimal.annotations.AnnoDoc
            
            class TestClass {
                @AnnoDoc('''This is a method
@param bad missing parameter''')
                void method() {
                    println "Hello"
                }
            }
        """)

        when:
        generateSource()

        then:
        generated.getBlock("public void method()").javaDoc == "This is a method"
    }

    def "basic test with generics"() {
        given:
        createClass("""
            package dummy
     
            class TestClass<T extends Number> {
                void method(T age) {
                    println "Hello " + age
                }
            }
        """)

        when:
        generateSource()

        then:
        generated.getBlock("public void method(T age)") || generated.getBlock("public void method(T param0)")
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
        generated.imports == ["bummy.MyAnnotation", "java.lang.String"]
        generated.text == "public class TestClass"
        generated.javaDoc == "This is a test class"
        generated.innerBlocks.size() == 4
        generated.getBlock("public TestClass()")
        generated.getBlock("public void method()").javaDoc == "This is a method"
        generated.getBlock("public String getField()")
        generated.getBlock("public void setField(String param0)") || generated.getBlock("public void setField(String value)")
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
    charValue = '\\u0000',
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
        !generated.imports
        generated.text == "public class TestClass"
        generated.innerBlocks.size() == 3
        generated.getBlock("public TestClass()")
        generated.getBlock("public void method()")


        when:
        def innerClass = generated.getBlock("public static class InnerClass")

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
        AnnoDocGenerator.generate(new File(outputDirectory, clazz.getName().replace('.', '/') + ".class"), builder)
        generatedSource = builder.toString()
    }
}