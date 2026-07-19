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

import java.util.Optional

class DocumentationTest extends Specification {

    def "exposes optional scalar values and clears them explicitly"() {
        given:
        def documentation = Documentation.builder()
                .summary('A summary.')
                .returns('a result')
                .build()

        expect:
        documentation.summary == Optional.of('A summary.')
        documentation.returnDescription == Optional.of('a result')

        when:
        def cleared = documentation.toBuilder()
                .clearSummary()
                .clearReturn()
                .build()

        then:
        cleared.summary == Optional.empty()
        cleared.returnDescription == Optional.empty()
    }

    def "all immutable documentation values have structural value semantics"() {
        given:
        def first = Documentation.builder()
                .summary('{{missing}}')
                .paragraph('Details.')
                .tag('since', '1.0')
                .build()
        def second = first.toBuilder().build()
        def link = Documentation.Link.text('example.Owner#copy()', 'copy')
        def sameLink = Documentation.Link.text('example.Owner#copy()', 'copy')

        expect:
        first == second
        first.hashCode() == second.hashCode()
        first.blocks.first().toString().contains('Details.')
        first.tags.first().toString().contains('since')
        first.toString().contains('{{missing}}')
        link == sameLink
        link.hashCode() == sameLink.hashCode()
        link.label == Optional.of('copy')
        link.toString().contains('example.Owner#copy()')
    }

    def "rejects null input for #operationName"() {
        when:
        operation.call()

        then:
        thrown(NullPointerException)

        where:
        operationName           | operation
        'parse'                 | { Documentation.parse(null) }
        'summary'               | { Documentation.builder().summary(null) }
        'paragraph'             | { Documentation.builder().paragraph(null) }
        'code block'            | { Documentation.builder().codeBlock(null) }
        'block replacement'     | { Documentation.builder().replaceBlocks(null) }
        'parameter name'        | { Documentation.builder().param(null, '') }
        'parameter description' | { Documentation.builder().param('value', null) }
        'parameter replacement' | { Documentation.builder().replaceParameters(null) }
        'return description'    | { Documentation.builder().returns(null) }
        'exception name'        | { Documentation.builder().throwsException(null, '') }
        'exception description' | { Documentation.builder().throwsException('Exception', null) }
        'exception replacement' | { Documentation.builder().replaceExceptions(null) }
        'tag name'              | { Documentation.builder().tag(null, '') }
        'tag value'             | { Documentation.builder().tag('since', null) }
        'deprecation value'     | { Documentation.builder().deprecated(null) }
        'link'                  | { Documentation.builder().see(null) }
        'textual link'          | { Documentation.builder().seeText(null) }
        'tag replacement'       | { Documentation.builder().replaceTags(null) }
        'append'                | { Documentation.builder().append(null) }
        'replace'               | { Documentation.builder().replace(null) }
        'parameter filter'      | { Documentation.builder().filterParameters(null) }
        'render parameter list' | { Documentation.empty().render(null) }
        'link target'           | { Documentation.Link.text(null) }
        'link label'            | { Documentation.Link.text('example.Owner', null) }
    }

    def "keeps blank descriptions distinct from explicit clearing"() {
        given:
        def documentation = Documentation.builder()
                .summary(' ')
                .paragraph('Details.')
                .param('value', '')
                .returns('')
                .throwsException('Exception', '')
                .tag('since', '')
                .build()

        expect:
        documentation.summary == Optional.of('')
        documentation.parameters == [value: '']
        documentation.returnDescription == Optional.of('')
        documentation.exceptions == [Exception: '']
        documentation.tags*.value == ['']

        when:
        def cleared = documentation.toBuilder()
                .clearSummary()
                .replaceBlocks([])
                .replaceParameters([:])
                .clearReturn()
                .replaceExceptions([:])
                .replaceTags([])
                .build()

        then:
        cleared.empty
    }

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

    def "conditional parameter fragments are inserted literally"() {
        expect:
        Documentation.builder()
                .summary('Use {{param:value?{{literal}}}}.')
                .build()
                .render(['value']) == 'Use {{literal}}.'
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

    def "declares string template values while validating dynamic calls contextually"() {
        given:
        def method = Documentation.Builder.getDeclaredMethod('templateValues', Map)

        expect:
        method.genericParameterTypes.first().typeName == 'java.util.Map<java.lang.String, java.lang.String>'

        when:
        Documentation.builder().templateValues(([kind: 42]) as Map<String, String>)

        then:
        def failure = thrown(Documentation.TemplateException)
        failure.message.contains('documentation')
        failure.message.contains('kind')
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

    def "parses normalized prose blocks and standard tags"() {
        when:
        def documentation = Documentation.parse('''A summary.\r
\r
<p>First paragraph\r
continues here.</p>\r
<pre>  return source\r
</pre>\r
A final paragraph\r
continues too.\r
\r
@param source the source value\r
  continued detail\r
@return the generated result\r
@exception IllegalStateException when source is invalid\r
@since 1.0''')

        then:
        documentation.summary == Optional.of('A summary.')
        documentation.blocks*.text == ['First paragraph\ncontinues here.', 'return source', 'A final paragraph\ncontinues too.']
        documentation.blocks*.code == [false, true, false]
        documentation.parameters == [source: 'the source value continued detail']
        documentation.returnDescription == Optional.of('the generated result')
        documentation.exceptions == [IllegalStateException: 'when source is invalid']
        documentation.tags*.name == ['since']
        documentation.render() == '''A summary.

<p>First paragraph
continues here.</p>

<pre>return source</pre>

<p>A final paragraph
continues too.</p>

@param source the source value continued detail
@return the generated result
@throws IllegalStateException when source is invalid
@since 1.0'''
    }

    def "parses single-line blocks and empty input deterministically"() {
        when:
        Documentation.parse(null)

        then:
        thrown(NullPointerException)

        expect:
        Documentation.parse(' \n ').empty
        Documentation.parse('<p>A paragraph</p>\n<pre>code()</pre>').render() == '''<p>A paragraph</p>

<pre>code()</pre>'''
    }

    def "omits only absent conditional parameter fragments and rejects stray delimiters"() {
        expect:
        Documentation.builder().summary('A{{param:value? value}}B').build().render([]) == 'AB'

        when:
        Documentation.builder().summary('A }} B').build().render()

        then:
        def failure = thrown(Documentation.TemplateException)
        failure.message.contains('documentation')
        failure.message.contains('<unknown>')
    }

    def "replaces a document and renders link and tag conveniences"() {
        given:
        def replacement = Documentation.builder()
                .summary('Replacement {{kind}}.')
                .codeBlock('return value')
                .param('value', 'the value to return')
                .returns('the returned value')
                .throwsException('IllegalStateException', 'when unavailable')
                .deprecated('use copy() instead')
                .see(Documentation.Link.text('example.Type', 'the target type'))
                .templateValues([kind: 'documentation'])
                .build()

        when:
        def documentation = Documentation.builder()
                .summary('Discarded summary.')
                .param('discarded', 'discarded parameter')
                .replace(replacement)
                .build()
                .toBuilder()
                .paragraph('More detail.')
                .build()

        then:
        documentation.render() == '''Replacement documentation.

<pre>return value</pre>

<p>More detail.</p>

@param value the value to return
@return the returned value
@throws IllegalStateException when unavailable
@deprecated use copy() instead
@see example.Type the target type'''
    }

    def "rejects malformed conditional fragments and template keys with context"() {
        when:
        Documentation.builder().summary(template).build().render(['value'])

        then:
        def failure = thrown(Documentation.TemplateException)
        failure.message.contains('documentation')
        failure.message.contains(key)

        where:
        template                   | key
        '{{param:value}}'          | 'value'
        '{{param:?included}}'      | ''
        '{{param:value?}}'         | 'value'
    }

    def "rejects malformed template keys before rendering"() {
        when:
        Documentation.builder().template('not a key', 'value')

        then:
        def failure = thrown(Documentation.TemplateException)
        failure.message.contains('documentation')
        failure.message.contains('not a key')
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
                .paragraph('The generated method adds caching.')
                .returns('the generated copy')
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
