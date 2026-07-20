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

import com.google.testing.compile.Compilation
import com.google.testing.compile.JavaFileObjects
import spock.lang.Issue

@Issue("41")
class SourceProjectionContractTest extends JavaClassGeneratingTest {

    def "representative Java declaration projection is deterministic and recompilable"() {
        given:
        String fixture = 'java-declaration-matrix'
        compile([
                'contract.JavaDeclarationFixture': '''
                    package contract;

                    import com.blackbuild.annodocimal.annotations.AnnoDoc;
                    import java.io.IOException;
                    import java.lang.annotation.Retention;
                    import java.lang.annotation.RetentionPolicy;
                    import java.util.List;

                    @AnnoDoc("Canonical class documentation")
                    public class JavaDeclarationFixture<T extends Number & Comparable<T>> {
                        @AnnoDoc("Canonical field documentation")
                        public T[] values;

                        @AnnoDoc("Canonical constructor documentation")
                        public JavaDeclarationFixture(T[] values) throws IllegalArgumentException {
                            this.values = values;
                        }

                        @AnnoDoc("Canonical method documentation")
                        public <E extends Exception> List<? extends T>[] convert(List<? super T>[] input)
                                throws IOException, E {
                            return null;
                        }

                        public interface NestedApi<X> {
                            X apply(X value) throws Exception;
                        }

                        protected enum Mode {
                            FAST, SLOW
                        }

                        @Retention(RetentionPolicy.RUNTIME)
                        public @interface Marker {
                            String value() default "marker";
                        }
                    }
                '''
        ], 'contract.JavaDeclarationFixture')
        SourceProjector projector = new SourceProjector(ProjectionPolicy.documentation())

        when:
        String projection = projector.projectToText(file.toPath())

        then:
        projector.projectToText(file.toPath()) == projection
        projection == '''package contract;

import java.io.IOException;
import java.lang.Comparable;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Number;
import java.lang.String;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Canonical class documentation
 */
public class JavaDeclarationFixture<T extends Number & Comparable<T>> {
  /**
   * Canonical field documentation
   */
  public T[] values;

  /**
   * Canonical constructor documentation
   */
  public JavaDeclarationFixture(T[] values) throws IllegalArgumentException {
  }

  /**
   * Canonical method documentation
   */
  public <E extends Exception> List<? extends T>[] convert(List<? super T>[] input) throws
      IOException, E {
    return null;
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface Marker {
    String value() default "marker";
  }

  protected enum Mode {
    FAST,

    SLOW
  }

  public interface NestedApi<X> {
    X apply(X value) throws Exception;
  }
}
'''

        and:
        assertProjectionCompiles(fixture, 'contract.JavaDeclarationFixture', projection)

        and: 'exact text checks retain semantics that compilation does not establish'
        projection.contains('/**\n * Canonical class documentation\n */')
        projection.contains('class JavaDeclarationFixture<T extends Number & Comparable<T>>')
        projection.contains('public T[] values;')
        projection.contains('public JavaDeclarationFixture(T[] values) throws IllegalArgumentException')
        projection.contains('public <E extends Exception> List<? extends T>[] convert(List<? super T>[] input) throws\n      IOException, E')
        projection.contains('public interface NestedApi<X>')
        projection.contains('X apply(X value) throws Exception;')
        projection.contains('protected enum Mode')
        projection.contains('public @interface Marker')
        projection.contains('String value() default "marker";')
    }

    def "supported Java record projections are deterministic and recompilable"() {
        given:
        String fixture = 'java-record'
        compile([
                'contract.RecordFixture': '''
                    package contract;

                    import java.io.Serializable;

                    public record RecordFixture<T extends Comparable<? super T>>(T value, String[] aliases)
                            implements Serializable {
                        public static class Metadata {}
                        public record NestedRecord<U>(U nestedValue) {}
                    }
                '''
        ], 'contract.RecordFixture')
        SourceProjector projector = new SourceProjector(ProjectionPolicy.documentation())

        when:
        String projection = projector.projectToText(file.toPath())

        then:
        projector.projectToText(file.toPath()) == projection

        and:
        assertProjectionCompiles(fixture, 'contract.RecordFixture', projection)

        and: 'record kind and component semantics are preserved exactly'
        projection.contains('public record RecordFixture<T extends Comparable<? super T>>(T value, String[] aliases)')
        projection.contains('public static record NestedRecord<U>(U nestedValue)')
    }

    private void assertProjectionCompiles(String fixture, String qualifiedName, String projection) {
        Compilation result = compiler.compile(JavaFileObjects.forSourceString(qualifiedName, projection))
        assert result.status() == Compilation.Status.SUCCESS:
                "Projection fixture '$fixture' failed to compile:\n${result.diagnostics()}\n$projection"
    }
}
