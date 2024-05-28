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

import com.blackbuild.annodocimal.annotations.InlineJavadocs
import com.google.testing.compile.Compilation
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import org.intellij.lang.annotations.Language
import spock.lang.Specification

import javax.tools.StandardLocation

import static com.google.testing.compile.Compiler.javac

class AnnoDocimalAnnotationProcessorTest extends Specification {

    Compiler compiler = javac().withProcessors(new AnnoDocimalAnnotationProcessor())
    Compilation compilation

    void "Simple class"() {
        when:
        compile("AClass",
                """
package com.blackbuild.annodocimal.ast.test;

import com.blackbuild.annodocimal.annotations.InlineJavadocs;

/**
 * A class for testing.
 */
@InlineJavadocs
public class AClass {}"""
        )
        def props = getJavaDocProperties("com.blackbuild.annodocimal.ast.test", "AClass")

        then:
        props == [classDoc: "A class for testing."]
    }

    void "class with constructor, methods and fields"() {
        when:
        compile("AClass",
                """
package com.blackbuild.annodocimal.ast.test;

import com.blackbuild.annodocimal.annotations.InlineJavadocs;

/**
 * A class for testing.
 */
@InlineJavadocs
public class AClass {

    /**
     * Creates a new instance of {@link AClass}.
     */
    public AClass() {}

    /**
     * A method that does nothing.
     */
    public void aMethod() {
        // do nothing
    }

    /**
     * A method that does something.
     * @param what the thing to do
     * @return the result of doing it
     */
    public String doIt(String what) {
        return "I did it: " + what;
    }

    /**
     * A field.
     */
    public String field = "field";
}"""
        )
        def props = getJavaDocProperties("com.blackbuild.annodocimal.ast.test", "AClass")

        then:
        props == [
                classDoc: "A class for testing.",
                "field.field": "A field.",
                "method.<init>()": "Creates a new instance of {@link AClass}.",
                "method.aMethod()": "A method that does nothing.",
                "method.doIt(java.lang.String)": '''A method that does something.
@param what the thing to do
@return the result of doing it'''
        ]
    }

    void "class with primitive values"() {
        when:
        compile("AClass",
                """
package com.blackbuild.annodocimal.ast.test;

import com.blackbuild.annodocimal.annotations.InlineJavadocs;

/**
 * A class for testing.
 */
@InlineJavadocs
public class AClass {

    /**
     * A method that does something.
     * @param what the thing to do
     */
    public void doIt(int what) {
    }

    /**
     * A field.
     */
    public boolean flag;
}"""
        )
        def props = getJavaDocProperties("com.blackbuild.annodocimal.ast.test", "AClass")

        then:
        props == [
                classDoc: "A class for testing.",
                "field.flag": "A field.",
                "method.doIt(int)": '''A method that does something.
@param what the thing to do'''
        ]
    }

    void "class with generic elements"() {
        when:
        compile("AClass",
                """
package com.blackbuild.annodocimal.ast.test;

import com.blackbuild.annodocimal.annotations.InlineJavadocs;
import java.util.*;

/**
 * A class for testing.
 */
@InlineJavadocs
public class AClass {

    /**
     * A method that does something.
     * @param what the thing to do
     */
    public void doIt(List<String> what) {
    }

    /**
     * A field.
     */
    public Map<String, String> flag;
}"""
        )
        def props = getJavaDocProperties("com.blackbuild.annodocimal.ast.test", "AClass")

        then:
        props == [
                classDoc: "A class for testing.",
                "field.flag": "A field.",
                "method.doIt(java.util.List)": '''A method that does something.
@param what the thing to do'''
        ]
    }

    void "class with inner class"() {
        when:
        compile("AClass",
                """
package com.blackbuild.annodocimal.ast.test;

import com.blackbuild.annodocimal.annotations.InlineJavadocs;

/**
 * A class for testing.
 */
@InlineJavadocs
public class AClass {

    /**
     * A method that does nothing.
     */
    public void aMethod() {
        // do nothing
    }

    /**
    * An inner class.
    */
    static class InnerClass {
        /**
         * Another method that does nothing.
         */
        public void innerMethod() {}
    }
}"""
        )
        def outerProps = getJavaDocProperties("com.blackbuild.annodocimal.ast.test", "AClass")

        then:
        outerProps == [
                classDoc: "A class for testing.",
                "method.aMethod()": "A method that does nothing.",
        ]

        def innerProps = getJavaDocProperties("com.blackbuild.annodocimal.ast.test", 'AClass$InnerClass')

        then:
        innerProps == [
                classDoc: "An inner class.",
                "method.innerMethod()": "Another method that does nothing.",
        ]
    }

    void compile(String name, @Language("java") String code) {
        compilation = compiler.compile(JavaFileObjects.forSourceString(name, code))
    }

    Map<String, String> getJavaDocProperties(String packageName, String className) {
        def file = compilation.generatedFile(StandardLocation.CLASS_OUTPUT, packageName, "$className$InlineJavadocs.JAVADOC_PROPERTIES_SUFFIX").get()
        def properties = new Properties()
        file.openInputStream().withCloseable { reader ->
            properties.load(reader)
        }
        return properties as Map<String, String>
    }

}
