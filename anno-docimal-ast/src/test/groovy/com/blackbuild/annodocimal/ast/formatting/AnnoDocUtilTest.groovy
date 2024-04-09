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
package com.blackbuild.annodocimal.ast.formatting

import com.blackbuild.annodocimal.annotations.AnnoDoc
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import spock.lang.Specification

import static org.codehaus.groovy.ast.tools.GeneralUtils.constX

class AnnoDocUtilTest extends Specification {

    AnnotatedNode node

    def "Tag not present returns default"() {
        given:
        def text = "This is a test class"

        when:
        def result = AnnoDocUtil.getNamedTagText("tag", text)

        then:
        result.empty
    }

    def "Tag present returns value"() {
        given:
        def text = '''This is a test class
@tag value'''

        when:
        String result = AnnoDocUtil.getNamedTagText("tag", text).get()

        then:
        result == "value"
    }

    def "Tag present returns value with multiple whitespace"() {
        given:
        def text = '''This is a test class
@tag     value'''

        when:
        String result = AnnoDocUtil.getNamedTagText("tag", text).get()

        then:
        result == "value"
    }

    def "Tag present returns value with leading whitespace"() {
        given:
        def text = '''This is a test class
  @tag     value'''

        when:
        String result = AnnoDocUtil.getNamedTagText("tag", text).get()

        then:
        result == "value"
    }

    def "tags are converted into a multi map"() {
        given:
        def text = '''this is a test class.
@param name the name of the person
@param age the age of the person
@throws IllegalArgumentException if the age is negative
@throws IllegalStateException if the age is too high
@deprecated
'''
        when:
        def result = AnnoDocUtil.getAllMultiTags(text)

        then:
        result == ["param":["name the name of the person", "age the age of the person"],
                   "throws":["IllegalArgumentException if the age is negative", "IllegalStateException if the age is too high"],
                   "deprecated":[""]]
    }

    def "Returns correctly formatted multiline tags"() {
        given:
        def text = '''This is a test class

@tag value goes here
     and into the next line'''

        when:
        String result = AnnoDocUtil.getNamedTagText("tag", text).get()

        then:
        result == "value goes here and into the next line"
    }

    def "should return the first sentence of a Javadoc text with correct formatting and escaping"() {
        given:
        def javadoc = "This is the first sentence. This is the second sentence."

        when:
        def result = AnnoDocUtil.getFirstSentenceOfJavadoc(javadoc)

        then:
        result == "This is the first sentence."
    }

    def "should use empty line as first sentence delimiter"() {
        given:
        def javadoc = """This is the first sentence

This is the second sentence."""

        when:
        def result = AnnoDocUtil.getFirstSentenceOfJavadoc(javadoc)

        then:
        result == "This is the first sentence"
    }

    def "should use <p> as first sentence delimiter"() {
        given:
        def javadoc = """This is the first sentence
<p>
This is the second sentence."""

        when:
        def result = AnnoDocUtil.getFirstSentenceOfJavadoc(javadoc)

        then:
        result == "This is the first sentence"
    }

    def "should use tag first sentence delimiter"() {
        given:
        def javadoc = """This is the first sentence
@param bla blub"""

        when:
        def result = AnnoDocUtil.getFirstSentenceOfJavadoc(javadoc)

        then:
        result == "This is the first sentence"
    }

    def "should return an empty string when Javadoc text has no sentences"() {
        given:
        def javadoc = ""

        when:
        def result = AnnoDocUtil.getFirstSentenceOfJavadoc(javadoc)

        then:
        result == ""
    }


    AnnotatedNode withJavaDoc(String text) {
        AnnotatedNode node = new AnnotatedNode()
        def annotation = new AnnotationNode(ClassHelper.make(AnnoDoc.class))
        node.addAnnotation(annotation)
        annotation.addMember("value", constX(text))
        return node
    }

}
