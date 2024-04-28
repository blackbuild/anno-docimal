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


import spock.lang.Specification

class JavadocDocBuilderTest extends Specification {

    def "should return a string with Javadoc format when there are paragraphs, parameters, exceptions, and other tags"() {
        given:
        def docBuilder = new JavadocDocBuilder()
        docBuilder.title = "This is the title"
        docBuilder.paragraphs = ["This is paragraph 1", "This is paragraph 2"]
        docBuilder.params = ["param1": "This is parameter 1", "param2": "This is parameter 2"]
        docBuilder.returnType = "This is the return type"
        docBuilder.exceptions = ["Exception1": "This is exception 1", "Exception2": "This is exception 2"]
        docBuilder.otherTags = ["Tag1": ["Value1", "Value2"], "Tag2": ["Value3"]]

        when:
        def result = docBuilder.toJavadoc()

        then:
        result == """This is the title
<p>
This is paragraph 1
</p>
<p>
This is paragraph 2
</p>
@param param1 This is parameter 1
@param param2 This is parameter 2
@return This is the return type
@throws Exception1 This is exception 1
@throws Exception2 This is exception 2
@Tag1 Value1
@Tag1 Value2
@Tag2 Value3
"""
    }

    def "should instantiate JavadocDocumentation with a title"() {
        given:
        def title = "Test Title"

        when:
        def doc = new JavadocDocBuilder().title(title)

        then:
        doc.title == title
    }

    def "should add paragraphs to the documentation"() {
        given:
        def doc = new JavadocDocBuilder().title("Test Title")

        when:
        doc.p("Paragraph 1")
        doc.p("Paragraph 2")
        def result = doc.toJavadoc()

        then:
        result ==
                """Test Title
<p>
Paragraph 1
</p>
<p>
Paragraph 2
</p>
"""
    }

    def "should add parameters to the documentation"() {
        given:
        def doc = new JavadocDocBuilder().title("Test Title")

        when:
        doc.param("param1", "Description 1")
        doc.param("param2", "Description 2")
        def result = doc.toJavadoc()

        then:
        result ==
                """Test Title
@param param1 Description 1
@param param2 Description 2
"""
    }

    def "should instantiate JavadocDocumentation without a title"() {
        given:
        def doc = new JavadocDocBuilder()

        when:
        def result = doc.toJavadoc()

        then:
        result == ""
    }

    def "should add paragraphs to the documentation with null values"() {
        given:
        def doc = new JavadocDocBuilder().title("Test Title")

        when:
        doc.p(null)
        def result = doc.toJavadoc()

        then:
        result ==
        """Test Title
"""
    }

    def "should add parameters to the documentation with null values"() {
        given:
        def doc = new JavadocDocBuilder().title("Test Title")

        when:
        doc.param("param1", null)
        def result = doc.toJavadoc()

        then:
        result ==
        """Test Title
"""
    }

    def "should correctly parse a existing javadoc string into a builder"() {
        given:
        def javadoc = """This is the title
<p>
This is paragraph 1
</p>
<p>
This is paragraph 2
</p>
@param param1 This is parameter 1
@param param2 This is parameter 2
@return This is the return type
@throws Exception1 This is exception 1
@throws Exception2 This is exception 2
@Tag1 Value1
@Tag1 Value2
@Tag2 Value3
"""

        when:
        def docBuilder = new JavadocDocBuilder().fromRawText(javadoc)

        then:
        docBuilder.title == "This is the title"
        docBuilder.paragraphs == ['''<p>
This is paragraph 1
</p>
<p>
This is paragraph 2
</p>''']
        docBuilder.params == ["param1": "This is parameter 1", "param2": "This is parameter 2"]
        docBuilder.returnType == "This is the return type"
        docBuilder.exceptions == ["Exception1": "This is exception 1", "Exception2": "This is exception 2"]
        docBuilder.otherTags == ["Tag1": ["Value1", "Value2"], "Tag2": ["Value3"]]
    }

}