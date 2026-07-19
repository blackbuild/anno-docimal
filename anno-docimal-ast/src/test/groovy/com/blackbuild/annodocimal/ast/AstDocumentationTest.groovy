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

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ConstantExpression
import spock.lang.Specification

class AstDocumentationTest extends Specification {

    def "attaches exact documentation with final-signature parameter filtering"() {
        given:
        def method = method('copy', 'source')
        def documentation = Documentation.builder()
                .summary('Copies {{kind}}.{{param:source? The source is required.}}')
                .template('kind', 'documentation')
                .param('obsolete', 'not in the generated signature')
                .param('source', 'the input document')
                .build()

        when:
        AstDocumentation.attach(method, documentation)

        then:
        AstDocumentation.extractExact(method).map { it.render(['source']) }.orElseThrow() == '''Copies documentation. The source is required.

@param source the input document'''
        method.annotations.count { it.classNode.name == 'com.blackbuild.annodocimal.annotations.AnnoDoc' } == 1
    }

    def "replaces only AnnoDocimal documentation and creates canonical member links"() {
        given:
        def method = method('copy', 'source')
        AstDocumentation.attachText(method, 'Old documentation.')

        when:
        AstDocumentation.attachText(method, 'New documentation.')

        then:
        AstDocumentation.extractExact(method).map { it.render() }.orElseThrow() == 'New documentation.'
        AstDocumentation.referenceTo(method).inline() == '{@link example.Owner#copy(java.lang.String)}'
        AstDocumentation.referenceTo(method).getTarget() == 'example.Owner#copy(java.lang.String)'
        method.annotations.count { it.classNode.name == 'com.blackbuild.annodocimal.annotations.AnnoDoc' } == 1
    }

    def "keeps explicit textual link targets author-owned"() {
        expect:
        Documentation.Link.text('not a resolvable javadoc target', 'read more').inline() == '{@link not a resolvable javadoc target read more}'
    }

    def "reports the AST target when attachment cannot render a required template"() {
        when:
        AstDocumentation.attach(method('copy', 'source'), Documentation.builder().summary('Copies {{kind}}.').build())

        then:
        def failure = thrown(Documentation.TemplateException)
        failure.message.contains('example.Owner#copy(java.lang.String)')
        failure.message.contains('kind')
    }

    def "renders canonical references for classes fields and constructors"() {
        given:
        def owner = new ClassNode('example.Owner', 1, ClassHelper.OBJECT_TYPE)
        def field = new FieldNode('value', 1, ClassHelper.STRING_TYPE, owner, null)
        owner.addField(field)
        def constructor = new ConstructorNode(1, [new Parameter(ClassHelper.make(String[].class), 'values')] as Parameter[], ClassNode.EMPTY_ARRAY, null)
        owner.addConstructor(constructor)
        def nested = new ClassNode('example.Owner$Nested', 1, ClassHelper.OBJECT_TYPE)

        expect:
        AstDocumentation.referenceTo(owner).target == 'example.Owner'
        AstDocumentation.referenceTo(field).target == 'example.Owner#value'
        AstDocumentation.referenceTo(constructor).target == 'example.Owner(java.lang.String[])'
        AstDocumentation.referenceTo(nested).target == 'example.Owner.Nested'
    }

    def "rejects invalid AST reference targets and clears empty attachments"() {
        given:
        def method = method('copy', 'source')
        AstDocumentation.attachText(method, 'Existing documentation.')

        when:
        AstDocumentation.attach(method, null)

        then:
        AstDocumentation.extractExact(method).empty
        method.annotations.every { it.classNode.name != 'com.blackbuild.annodocimal.annotations.AnnoDoc' }

        when:
        AstDocumentation.attachText(method, 'Existing documentation.')
        AstDocumentation.attach(method, Documentation.empty())

        then:
        AstDocumentation.extractExact(method).empty

        when:
        AstDocumentation.referenceTo(new ClassNode('', 1, ClassHelper.OBJECT_TYPE))

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('class name')
    }

    private static MethodNode method(String name, String parameterName) {
        def owner = new ClassNode('example.Owner', 1, ClassHelper.OBJECT_TYPE)
        def method = new MethodNode(name, 1, ClassHelper.STRING_TYPE,
                [new Parameter(ClassHelper.STRING_TYPE, parameterName)] as Parameter[], ClassNode.EMPTY_ARRAY, null)
        owner.addMethod(method)
        method
    }
}
