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

import java.lang.reflect.Modifier

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

    private static List<String> actualSignatures() {
        SUPPORTED_TYPES.collectMany { type ->
            ["type ${type.name}"] + type.declaredMethods.findAll { Modifier.isPublic(it.modifiers) }
                    .collect { method -> "method ${type.name}#${method.name}(${method.parameterTypes*.name.join(',')}):${method.returnType.name}" }
        }.sort()
    }

    private static List<String> baselineSignatures() {
        SupportedApiBaselineTest.getResourceAsStream('/com/blackbuild/annodocimal/ast/1.0-authoring-api-baseline.txt')
                .readLines()
                .findAll { !it.isBlank() && !it.startsWith('#') }
                .sort()
    }
}
