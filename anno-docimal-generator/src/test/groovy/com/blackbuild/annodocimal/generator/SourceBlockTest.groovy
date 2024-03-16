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
