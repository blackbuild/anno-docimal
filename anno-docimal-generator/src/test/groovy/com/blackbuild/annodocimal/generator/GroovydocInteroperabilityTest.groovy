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

import static com.blackbuild.annodocimal.generator.ProjectionContractAssertions.assertProjectionCompiles
import static com.google.testing.compile.Compiler.javac

@Issue("19")
class GroovydocInteroperabilityTest extends ClassGeneratingTest {

    def "projection selects and normalizes runtime GroovyDoc deterministically"() {
        given:
        compilerConfiguration.optimizationOptions.runtimeGroovydoc = Boolean.TRUE
        createClass('''
            package contract

            import com.blackbuild.annodocimal.annotations.AnnoDoc

            /**@
             * Runtime class documentation must lose to the canonical carrier.
             */
            @AnnoDoc('Canonical class documentation.')
            class RuntimeGroovyDocFixture {
                /**@
                 * Runtime method documentation.
                 */
                String action() { null }

                /**@
                 * Runtime method documentation must lose to the canonical carrier.
                 */
                @AnnoDoc('Canonical method documentation.')
                String canonicalAction() { null }

                /**@
                 * Runtime nested documentation.
                 */
                static class Nested {}
            }
        ''')
        def classFile = new File(outputDirectory, 'contract/RuntimeGroovyDocFixture.class').toPath()
        def projector = new SourceProjector(ProjectionPolicy.documentation())

        when:
        String projection = projector.projectToText(classFile)

        then:
        projector.projectToText(classFile) == projection
        projection.contains('''/**
 * Canonical class documentation.
 */''')
        projection.contains('''  /**
   * Runtime method documentation.
   */''')
        projection.contains('''  /**
   * Canonical method documentation.
   */''')
        projection.contains('''  /**
   * Runtime nested documentation.
   */''')
        !projection.contains('Runtime class documentation must lose')
        !projection.contains('Runtime method documentation must lose')
        !projection.contains('@Groovydoc')
        !projection.contains('@AnnoDoc')

        and:
        assertProjectionCompiles(javac().withOptions('-parameters'), 'runtime-groovydoc',
                'contract.RuntimeGroovyDocFixture', projection)
    }
}
