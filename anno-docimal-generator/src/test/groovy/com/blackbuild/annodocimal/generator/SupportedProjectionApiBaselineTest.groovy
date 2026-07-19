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
package com.blackbuild.annodocimal.generator

import org.jspecify.annotations.Nullable
import spock.lang.Specification

import java.lang.reflect.Modifier
import java.nio.file.Path
import java.util.Optional

class SupportedProjectionApiBaselineTest extends Specification {

    private static final List<Class<?>> SUPPORTED_TYPES = [
            SourceProjector,
            ProjectionPolicy,
            ProjectionPolicy.Builder,
            DeclarationVisibility,
            SourceProjectionException
    ]

    def "the supported projection API matches its checked-in compatibility baseline"() {
        expect:
        actualSignatures() == baselineSignatures()
    }

    def "Java and Groovy consumers observe the supported projection API as null-marked"() {
        expect:
        SUPPORTED_TYPES.every(SupportedNullabilityJavaConsumer::isEffectivelyNullMarked)

        and:
        SupportedNullabilityJavaConsumer.defaultPolicy() == ProjectionPolicy.documentation()
        SupportedNullabilityJavaConsumer.unknownIdentifier(Path.of('Example.class')) == Optional.empty()
    }

    def "projection value equality accepts a genuinely nullable comparison value"() {
        expect:
        ProjectionPolicy.getDeclaredMethod('equals', Object).annotatedParameterTypes[0]
                .isAnnotationPresent(Nullable)
    }

    def "implementation-only projection types remain outside the null-marked boundary"() {
        expect:
        !SupportedNullabilityJavaConsumer.isEffectivelyNullMarked(SpecConverter)
    }

    def "runtime null rejection remains the projection contract"() {
        when:
        operation.call()

        then:
        thrown(NullPointerException)

        where:
        operation << [
                { new SourceProjector(null) },
                { ProjectionPolicy.builder().includedVisibilities(null) },
                { ProjectionPolicy.builder().includedVisibilities([null]) }
        ]
    }

    private static List<String> actualSignatures() {
        SUPPORTED_TYPES.collectMany { type ->
            ["type ${Modifier.toString(type.modifiers)} ${type.name}"] +
                    type.declaredAnnotations.findAll { it.annotationType().packageName == 'org.jspecify.annotations' }
                            .collect { annotation -> "annotation ${type.name}:${annotation.annotationType().name}" } +
                    type.declaredConstructors.findAll { Modifier.isPublic(it.modifiers) }.collect { constructor ->
                        "constructor ${type.name}(${constructor.genericParameterTypes*.typeName.join(',')})"
                    } +
                    type.declaredMethods.findAll { Modifier.isPublic(it.modifiers) }.collectMany { method ->
                        def signature = "${type.name}#${method.name}(${method.genericParameterTypes*.typeName.join(',')})"
                        ["method ${signature}:${method.genericReturnType.typeName}"] +
                                method.annotatedParameterTypes.toList().withIndex().collectMany { parameter, index ->
                                    parameter.annotations.findAll { it.annotationType().packageName == 'org.jspecify.annotations' }
                                            .collect { annotation ->
                                                "parameter-annotation ${signature}[${index}]:${annotation.annotationType().name}"
                                            }
                                }
                    } +
                    type.declaredFields.findAll { Modifier.isPublic(it.modifiers) }.collect { field ->
                        "field ${type.name}#${field.name}:${field.genericType.typeName}"
                    }
        }.sort()
    }

    private static List<String> baselineSignatures() {
        SupportedProjectionApiBaselineTest.getResourceAsStream(
                '/com/blackbuild/annodocimal/generator/1.0-projection-api-baseline.txt')
                .readLines()
                .findAll { !it.isBlank() && !it.startsWith('#') }
                .sort()
    }
}
