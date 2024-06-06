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
import spock.lang.Subject

class DocTextTest extends Specification {

    @Subject DocText docText

    def "Tag not present returns default"() {
        when:
        docText = DocText.fromRawText("This is a test class")

        then:
        docText.title == "This is a test class"
        docText.body == ""
        docText.getTag("tag").isEmpty()
    }

    def "Tag present returns value"() {
        when:
        docText = DocText.fromRawText('''This is a test class
@tag value''')

        then:
        docText.title == "This is a test class"
        docText.body == ""
        docText.getTag("tag").get() == "value"
    }

    def "Tag present returns value with multiple whitespace"() {
        when:
        docText = DocText.fromRawText('''This is a test class
@tag     value''')

        then:
        docText.title == "This is a test class"
        docText.body == ""
        docText.getTag("tag").get() == "value"
    }

    def "Tag present returns value with leading whitespace"() {
        when:
        docText = DocText.fromRawText('''This is a test class
  @tag     value''')

        then:
        docText.title == "This is a test class"
        docText.body == ""
        docText.getTag("tag").get() == "value"
    }

    def "tags are converted into a multi map"() {
        when:
        docText = DocText.fromRawText('''this is a test class.
@param name the name of the person
@param age the age of the person
@throws IllegalArgumentException if the age is negative
@throws IllegalStateException if the age is too high
@deprecated
''')

        then:
        docText.tags == ["param":["name the name of the person", "age the age of the person"],
                   "throws":["IllegalArgumentException if the age is negative", "IllegalStateException if the age is too high"],
                   "deprecated":[""]]
    }

    def "Returns correctly formatted multiline tags"() {
        when:
        docText = DocText.fromRawText('''This is a test class

@tag value goes here
     and into the next line''')

        then:
        docText.getTag("tag").get() == "value goes here and into the next line"
    }

    def "should return the first sentence of a Javadoc text with correct formatting and escaping"() {
        when:
        docText = DocText.fromRawText("This is the first sentence. This is the second sentence.")

        then:
        docText.title == "This is the first sentence."
    }

    def "should use empty line as first sentence delimiter"() {
        when:
        docText = DocText.fromRawText("""This is the first sentence

This is the second sentence.""")

        then:
        docText.title == "This is the first sentence"
    }

    def "should use <p> as first sentence delimiter"() {
        when:
        docText = DocText.fromRawText("""This is the first sentence
<p>
This is the second sentence.""")

        then:
        docText.title == "This is the first sentence"
    }

    def "should use tag first sentence delimiter"() {
        when:
        docText = DocText.fromRawText("""This is the first sentence
@param bla blub""")

        then:
        docText.title == "This is the first sentence"
    }

    def "should return an empty string when Javadoc text has no sentences"() {
        when:
        docText = DocText.fromRawText("")

        then:
        docText.title == ""
    }

    def "should return the value of the named tag if it exists"() {
        when:
        def docText = DocText.fromRawText("""this is a test class.

@param flame the flame of the person
@param name the name of the person
""")

        then:
        docText.getNamedTag("param", "name").get() == "the name of the person"
    }

    def "should return the value trimmed and onelined"() {
        when:
        def docText = DocText.fromRawText("""this is a test class.

@param after  double whitespace after name
@param   before double whitespace after tagname
 @param beforetag double whitespace before tagname
@param multiline this is a multiline
    tag
""")

        then:
        docText.getTags("param") == ["after double whitespace after name",
                                     "before double whitespace after tagname",
                                     "beforetag double whitespace before tagname",
                                     "multiline this is a multiline tag"]

        and:
        docText.getNamedTag("param", "after").get() == "double whitespace after name"
        docText.getNamedTag("param", "before").get() == "double whitespace after tagname"
        docText.getNamedTag("param", "beforetag").get() == "double whitespace before tagname"
        docText.getNamedTag("param", "multiline").get() == "this is a multiline tag"

        and:
        docText.getNamedTags("param") == ["after": "double whitespace after name",
                                          "before": "double whitespace after tagname",
                                          "beforetag": "double whitespace before tagname",
                                          "multiline": "this is a multiline tag"]
    }

    def "should handle named tags with empty values"() {
        when:
        docText = DocText.fromRawText("""this is a test class.

        @param flame the flame of the person
        @param name
        """)

        then:
        docText.getNamedTag("param", "name").get() == ""
    }

}
