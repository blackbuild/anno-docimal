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

import com.blackbuild.annodocimal.annotations.AnnoDoc
import com.blackbuild.annodocimal.annotations.InlineJavadocs
import com.blackbuild.annodocimal.ast.extractor.ASTExtractor
import org.jspecify.annotations.Nullable
import spock.lang.Specification

import java.lang.reflect.Modifier
import java.util.Optional

class SupportedApiBaselineTest extends Specification {

    private static final List<Class<?>> SUPPORTED_TYPES = [
            AstDocumentation,
            Documentation,
            Documentation.Builder,
            Documentation.Block,
            Documentation.Tag,
            Documentation.Link,
            Documentation.TemplateException
    ]

    def "the supported authoring API matches its checked-in binary baseline"() {
        expect:
        actualSignatures() == baselineSignatures()
    }

    def "the supported builder excludes pre-baseline aliases"() {
        expect:
        Documentation.Builder.declaredMethods*.name.intersect(['appendParagraph', 'replaceReturn']).empty
    }

    def "Java and Groovy consumers observe the supported API as null-marked"() {
        expect:
        [AnnoDoc, InlineJavadocs, *SUPPORTED_TYPES].every(SupportedNullabilityJavaConsumer::isEffectivelyNullMarked)

        and:
        SupportedNullabilityJavaConsumer.rewrite('A summary.').summary == Optional.of('A summary.')
        SupportedNullabilityJavaConsumer.rewrite('').summary == Optional.empty()
    }

    def "equals accepts the genuinely nullable comparison value"() {
        expect:
        [Documentation, Documentation.Block, Documentation.Tag, Documentation.Link].every { type ->
            type.getDeclaredMethod('equals', Object).annotatedParameterTypes[0]
                    .isAnnotationPresent(Nullable)
        }
    }

    def "implementation-only AST types remain outside the null-marked boundary"() {
        expect:
        !SupportedNullabilityJavaConsumer.isEffectivelyNullMarked(ASTExtractor)
        !SupportedNullabilityJavaConsumer.isEffectivelyNullMarked(InlineJavadocsTransformation)
    }

    private static List<String> actualSignatures() {
        def authoringSignatures = SUPPORTED_TYPES.collectMany { type ->
            ["type ${type.name}"] +
                    type.declaredAnnotations.findAll { it.annotationType().packageName == 'org.jspecify.annotations' }
                            .collect { annotation -> "annotation ${type.name}:${annotation.annotationType().name}" } +
                    type.declaredMethods.findAll { Modifier.isPublic(it.modifiers) }.collectMany { method ->
                        def signature = "${type.name}#${method.name}(${method.parameterTypes*.name.join(',')})"
                        ["method ${signature}:${method.returnType.name}"] +
                                method.annotatedParameterTypes.toList().withIndex().collectMany { parameter, index ->
                                    parameter.annotations.findAll { it.annotationType().packageName == 'org.jspecify.annotations' }
                                            .collect { annotation ->
                                                "parameter-annotation ${signature}[${index}]:${annotation.annotationType().name}"
                                            }
                                }
                    }
        }
        def protocolNullability = [AnnoDoc, InlineJavadocs].collectMany { type ->
            type.declaredAnnotations.findAll { it.annotationType().packageName == 'org.jspecify.annotations' }
                    .collect { annotation -> "annotation ${type.name}:${annotation.annotationType().name}" }
        }
        (authoringSignatures + protocolNullability).sort()
    }

    private static List<String> baselineSignatures() {
        SupportedApiBaselineTest.getResourceAsStream('/com/blackbuild/annodocimal/ast/1.0-authoring-api-baseline.txt')
                .readLines()
                .findAll { !it.isBlank() && !it.startsWith('#') }
                .sort()
    }
}
