/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Stephan Pauxberger
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

class DocumentationTest extends Specification {

    def "authors ordered prose and code blocks with named values"() {
        when:
        def documentation = Documentation.builder()
                .summary('Creates {{kind}} documentation.')
                .paragraph('The generated member is stable.')
                .codeBlock('return source.copy()')
                .template('kind', 'reference')
                .param('source', 'the source document')
                .returns('a copy')
                .throwsException('IllegalArgumentException', 'when the source is invalid')
                .tag('since', '1.0')
                .build()

        then:
        documentation.render() == '''Creates reference documentation.

<p>The generated member is stable.</p>

<pre>return source.copy()</pre>

@param source the source document
@return a copy
@throws IllegalArgumentException when the source is invalid
@since 1.0'''
    }

    def "literal template values are inserted once without escaping or recursive evaluation"() {
        expect:
        Documentation.builder()
                .summary('Use {{value}}.')
                .template('value', '<code>{{literal}}</code>')
                .build()
                .render() == 'Use <code>{{literal}}</code>.'
    }

    def "reports missing template input with the target and key"() {
        when:
        Documentation.builder().summary('Missing {{value}}.').build().render()

        then:
        def failure = thrown(Documentation.TemplateException)
        failure.message.contains('documentation')
        failure.message.contains('value')
    }

    def "reports malformed template input with the target and key"() {
        when:
        Documentation.builder().summary('Malformed {{value.').build().render()

        then:
        def failure = thrown(Documentation.TemplateException)
        failure.message.contains('documentation')
        failure.message.contains('value')
    }

    def "rejects null and non-string template values with key context"() {
        when:
        Documentation.builder().templateValues([value: input])

        then:
        def failure = thrown(Documentation.TemplateException)
        failure.message.contains('documentation')
        failure.message.contains('value')

        where:
        input << [null, 42]
    }

    def "keeps template tags generic until an explicit template operation supplies values"() {
        when:
        def parsed = Documentation.parse('''A summary.

@template kind reference''')

        then:
        parsed.tags*.name == ['template']
        parsed.templateValues.isEmpty()
        parsed.render() == '''A summary.

@template kind reference'''
    }

    def "normalizes a rendered document without changing its meaning"() {
        given:
        def original = Documentation.builder()
                .summary('A summary.')
                .paragraph('First paragraph.')
                .codeBlock('line one\nline two')
                .param('later', 'second in source')
                .param('first', 'first in source')
                .tag('since', '1.0')
                .build()

        when:
        def reparsed = Documentation.parse(original.render())

        then:
        reparsed == original
        reparsed.render(['first', 'later']) == '''A summary.

<p>First paragraph.</p>

<pre>line one
line two</pre>

@param first first in source
@param later second in source
@since 1.0'''
    }

    def "explicit append replace and parameter filtering compose generated documentation"() {
        given:
        def captured = Documentation.builder()
                .summary('Copies a source document.')
                .paragraph('The source is not changed.')
                .param('source', 'the input document')
                .param('obsolete', 'not part of the generated signature')
                .build()

        when:
        def generated = Documentation.builder()
                .append(captured)
                .appendParagraph('The generated method adds caching.')
                .replaceReturn('the generated copy')
                .filterParameters(['source'])
                .build()

        then:
        generated.render(['source']) == '''Copies a source document.

<p>The source is not changed.</p>

<p>The generated method adds caching.</p>

@param source the input document
@return the generated copy'''
    }
}
