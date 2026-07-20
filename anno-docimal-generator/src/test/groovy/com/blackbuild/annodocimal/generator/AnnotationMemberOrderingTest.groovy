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

import spock.lang.Issue

import java.nio.charset.StandardCharsets

@Issue("33")
class AnnotationMemberOrderingTest extends ClassGeneratingTest {

    def "annotation members are projected by name across value kinds and repeated runs"() {
        given:
        createClass('''
            package dummy

            import java.lang.annotation.Retention
            import java.lang.annotation.RetentionPolicy

            enum Shade {
                LIGHT, DARK
            }

            @Retention(RetentionPolicy.RUNTIME)
            @interface NestedValue {
                String text()
                int count()
            }

            @Retention(RetentionPolicy.RUNTIME)
            @interface MixedValues {
                int primitive()
                Shade enumValue()
                Class<?> classValue()
                NestedValue nestedValue()
                int[] arrayValue()
            }

            @MixedValues(
                primitive = 7,
                enumValue = Shade.DARK,
                classValue = String,
                nestedValue = @NestedValue(text = 'inside', count = 2),
                arrayValue = [3, 1, 4]
            )
            class AnnotationOrderFixture {}
        ''')
        def classFile = new File(outputDirectory, 'dummy/AnnotationOrderFixture.class').toPath()
        SourceProjector projector = new SourceProjector(ProjectionPolicy.documentation())

        when:
        List<String> projections = (1..5).collect { projector.projectToText(classFile) }
        List<byte[]> projectionBytes = projections.collect { it.getBytes(StandardCharsets.UTF_8) }

        then:
        projectionBytes.tail().every { Arrays.equals(it, projectionBytes.first()) }
        projections.first() == '''package dummy;

import groovy.transform.Generated;
import java.lang.String;

@MixedValues(
    arrayValue = {3, 1, 4},
    classValue = String.class,
    enumValue = Shade.DARK,
    nestedValue = @NestedValue(count = 2, text = "inside"),
    primitive = 7
)
public class AnnotationOrderFixture {
  @Generated
  public AnnotationOrderFixture() {
  }
}
'''
    }
}
