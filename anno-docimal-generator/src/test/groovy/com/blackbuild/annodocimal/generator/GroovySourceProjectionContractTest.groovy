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

import com.google.testing.compile.Compiler
import spock.lang.Issue

import static com.blackbuild.annodocimal.generator.ProjectionContractAssertions.assertProjectionCompiles
import static com.google.testing.compile.Compiler.javac

@Issue("41")
class GroovySourceProjectionContractTest extends ClassGeneratingTest {

    def "representative Groovy declaration projection is deterministic and recompilable"() {
        given:
        String fixture = 'groovy-declaration-matrix'
        createClass('''
            package contract

            import com.blackbuild.annodocimal.annotations.AnnoDoc
            import groovy.lang.Groovydoc

            import java.lang.annotation.Retention
            import java.lang.annotation.RetentionPolicy

            @AnnoDoc('Canonical Groovy class documentation')
            class GroovyDeclarationFixture<T extends Number> {
                @AnnoDoc('Canonical Groovy property documentation')
                T[] values

                GroovyDeclarationFixture(T[] values) throws IllegalArgumentException {
                    this.values = values
                }

                List<? extends T> convert(List<? super T> input) throws IOException {
                    null
                }

                @Groovydoc('Interoperable nested interface documentation')
                interface NestedApi<X> {
                    X apply(X value) throws Exception
                }

                protected enum Mode {
                    FAST, SLOW
                }

                @Retention(RetentionPolicy.RUNTIME)
                @interface Marker {
                    String value() default 'marker'
                }
            }
        ''')
        def classFile = new File(outputDirectory, 'contract/GroovyDeclarationFixture.class').toPath()
        SourceProjector projector = new SourceProjector(ProjectionPolicy.documentation())
        Compiler projectionCompiler = javac().withOptions('-parameters')

        when:
        String projection = projector.projectToText(classFile)

        then:
        projector.projectToText(classFile) == projection
        projection == '''package contract;

import groovy.lang.Groovydoc;
import groovy.transform.Generated;
import java.io.IOException;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Number;
import java.lang.String;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Canonical Groovy class documentation
 */
public class GroovyDeclarationFixture<T extends Number> {
  public GroovyDeclarationFixture(T[] values) throws IllegalArgumentException {
  }

  public List<? extends T> convert(List<? super T> input) throws IOException {
    return null;
  }

  @Generated
  public T[] getValues() {
    return null;
  }

  @Generated
  public void setValues(T[] value) {
  }

  @Groovydoc("Interoperable nested interface documentation")
  public interface NestedApi<X> {
    X apply(X value) throws Exception;
  }

  protected enum Mode {
    FAST,

    SLOW
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface Marker {
    String value() default "marker";
  }
}
'''

        and:
        assertProjectionCompiles(projectionCompiler, fixture, 'contract.GroovyDeclarationFixture', projection)

        and: 'exact text checks retain semantics that compilation does not establish'
        projection.contains('/**\n * Canonical Groovy class documentation\n */')
        projection.contains('class GroovyDeclarationFixture<T extends Number>')
        projection.contains('public GroovyDeclarationFixture(T[] values) throws IllegalArgumentException')
        projection.contains('List<? extends T> convert(List<? super T> input) throws IOException')
        projection.contains('public T[] getValues()')
        projection.contains('public void setValues(T[] value)')
        projection.contains('@Groovydoc("Interoperable nested interface documentation")')
        projection.contains('public interface NestedApi<X>')
        projection.contains('X apply(X value) throws Exception;')
        projection.contains('protected enum Mode')
        projection.contains('@interface Marker')
        projection.contains('String value() default "marker";')
    }
}
