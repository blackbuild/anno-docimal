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

import org.intellij.lang.annotations.Language
import spock.lang.Specification

class SourceBlockTest extends Specification {

    String sourceCode

    def "basic test"() {
        given:
        source """
package dummy;

import groovy.lang.GroovyObject;

public class TestClass implements GroovyObject {
  public TestClass() {
  }

  public void method() {
  }

  public static class InnerClass implements GroovyObject {
    public void innerMethod() {
    }
  }
}"""

        when:
        def block = SourceBlock.fromtext(sourceCode)
        println block

        then:
        block.text == "public class TestClass implements GroovyObject"
        block.innerBlocks.size() == 3
        block.innerBlocks[0].text == "public TestClass()"
        block.innerBlocks[1].text == "public void method()"
        block.innerBlocks[2].text == "public static class InnerClass implements GroovyObject"
        block.innerBlocks[2].innerBlocks.size() == 1
        block.innerBlocks[2].innerBlocks[0].text == "public void innerMethod()"
    }

    void source(@Language("Java") String code) {
        sourceCode = code
    }

    def "basic test with javadoc"() {
        given:
        source """
package dummy;

/**
 * This is a test class
 */
public class TestClass {
    /**
     * This is a method
     */
    public void method() {
    }
    
    /**
     * This is an inner class
     */
    public static class InnerClass {
        /**
         * This is an inner method
         */
        public void innerMethod() {
        }
    }
}"""
        when:
        def block = SourceBlock.fromtext(sourceCode)

        then:
        block.javaDoc == "This is a test class"
        block.innerBlocks.size() == 2
        block.innerBlocks[0].javaDoc == "This is a method"
        block.innerBlocks[1].javaDoc == "This is an inner class"
        block.innerBlocks[1].innerBlocks[0].javaDoc == "This is an inner method"
    }
}
