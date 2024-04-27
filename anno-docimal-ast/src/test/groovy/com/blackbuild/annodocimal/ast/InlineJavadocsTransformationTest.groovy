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

import com.blackbuild.annodocimal.annotations.AnnoDoc
import org.intellij.lang.annotations.Language

class InlineJavadocsTransformationTest extends ClassGeneratingSpecification {

    File srcDir

    def setup() {
        srcDir = new File("build/test-sources/${getClass().simpleName}/$safeFilename")
        srcDir.deleteDir()
        srcDir.mkdirs()
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
        clazz.getAnnotation(AnnoDoc).value() == 'This is a test class'
        clazz.getMethod("method").getAnnotation(AnnoDoc).value() == 'A method'

        and: 'private fields retain their javadoc'
        clazz.getDeclaredField("name").getAnnotation(AnnoDoc).value() == 'A name'

        when:
        def innerClass = clazz.getDeclaredClasses().find { it.simpleName == "InnerClass" }

        then:
        innerClass.getAnnotation(AnnoDoc).value() == 'Inner class'
        innerClass.getMethod("innerMethod").getAnnotation(AnnoDoc).value() == 'Inner class method'
    }

    def "No javadoc should result in no annotation"() {
        when:
        createClass "dummy/TestClass.groovy", '''
package dummy

import com.blackbuild.annodocimal.annotations.InlineJavadocs

@InlineJavadocs
class TestClass {

    String name
    
    void method() {}
    
    static class InnerClass {
        void innerMethod() {}
    }
}
'''

        then:
        clazz.getAnnotation(AnnoDoc) == null
        clazz.getMethod("method").getAnnotation(AnnoDoc) == null
        clazz.getDeclaredField("name").getAnnotation(AnnoDoc) == null

        when:
        def innerClass = clazz.getDeclaredClasses().find { it.simpleName == "InnerClass" }

        then:
        innerClass.getAnnotation(AnnoDoc) == null
        innerClass.getMethod("innerMethod").getAnnotation(AnnoDoc) == null
    }

    def "ignore internal methods"() {
        when:
        createClass "dummy/TestClass.groovy", '''
package dummy

import com.blackbuild.annodocimal.annotations.InlineJavadocs

@InlineJavadocs
class TestClass {

    /** A name */
    String name
    
    /** a Field */
    private String privateField
    
    
    /** a Field */
    protected String protectedField
    
    /**
     * A method
     */ 
    private void privateMethod() {}
    
    /**
     * A method
     */
    protected void protectedMethod() {}
    
    /**
     * internal field
     */
    public String $internalField
    
    /**
     * internal method
     */
    void $internalMethod() {}
}
'''
        then:
        clazz.getDeclaredField("privateField").getAnnotation(AnnoDoc) != null
        clazz.getDeclaredField("protectedField").getAnnotation(AnnoDoc) != null
        clazz.getDeclaredField('$internalField').getAnnotation(AnnoDoc) == null
        clazz.getDeclaredMethod("privateMethod").getAnnotation(AnnoDoc) == null
        clazz.getDeclaredMethod("protectedMethod").getAnnotation(AnnoDoc) != null
        clazz.getDeclaredMethod('$internalMethod').getAnnotation(AnnoDoc) == null
    }

    def "ignore empty javadoc"() {
        when:
        createClass "dummy/TestClass.groovy", '''
package dummy

import com.blackbuild.annodocimal.annotations.InlineJavadocs

@InlineJavadocs
class TestClass {

    /**  */
    void emptyOneliner() {}
    
    /***/
    void emptyOneliner2() {}
   
    /**
     * 
     */
    void emptyMultiline() {}
}
'''
        then:
        clazz.getDeclaredMethod("emptyOneliner").getAnnotation(AnnoDoc) == null
        clazz.getDeclaredMethod("emptyOneliner2").getAnnotation(AnnoDoc) == null
        clazz.getDeclaredMethod('emptyMultiline').getAnnotation(AnnoDoc) == null
    }

    def "conversion without a file should not result in exception"() {
        given:
        // language=groovy
        def text = '''
package dummy

import com.blackbuild.annodocimal.annotations.InlineJavadocs

/**
 * This is a test class
 */
@InlineJavadocs
class TestClass {

    /** A name */
    String name
}
'''
        when:
        clazz = loader.parseClass(text)

        then:
        noExceptionThrown()
    }
}