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

class JavadocDocumentationTest extends Specification {


    def "should add a title to the documentation"() {
        given:
        JavadocDocumentation javadoc = new JavadocDocumentation()

        when:
        javadoc.title("Title")

        then:
        javadoc.toJavadoc() == "Title\n\n"
    }

    def "should add a paragraph to the documentation"() {
        given:
        JavadocDocumentation javadoc = new JavadocDocumentation()

        when:
        javadoc.p("Paragraph")

        then:
        javadoc.toJavadoc() == "<p>\nParagraph\n</p>\n\n"
    }

    def "should add a parameter to the documentation"() {
        given:
        JavadocDocumentation javadoc = new JavadocDocumentation()

        when:
        javadoc.param("name", "description")

        then:
        javadoc.toJavadoc() == "@param name description\n"
    }

    def "should throw an IllegalStateException if a method is called out of order"() {
        given:
        JavadocDocumentation javadoc = new JavadocDocumentation()

        when:
        javadoc.p("Paragraph")
        javadoc.title("Title")

        then:
        def exception = thrown(IllegalStateException)
        exception.message == "Cannot add TITLE after PARAGRAPH"
    }

    def "should throw an UnsupportedOperationException if code method is called"() {
        given:
        JavadocDocumentation javadoc = new JavadocDocumentation()

        when:
        javadoc.code("Code")

        then:
        def exception = thrown(UnsupportedOperationException)
        exception.message == "Not implemented yet"
    }


}