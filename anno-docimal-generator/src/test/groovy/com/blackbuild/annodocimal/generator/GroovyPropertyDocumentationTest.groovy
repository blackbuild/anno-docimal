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
package com.blackbuild.annodocimal.generator

import spock.lang.Issue

import static com.blackbuild.annodocimal.generator.ProjectionContractAssertions.assertProjectionCompiles
import static com.google.testing.compile.Compiler.javac

@Issue('9')
class GroovyPropertyDocumentationTest extends ClassGeneratingTest {

    def 'projection maps Groovy property documentation to generated accessors'() {
        given:
        createClass('''
            package contract

            import com.blackbuild.annodocimal.annotations.AnnoDoc

            class GroovyPropertyFixture {
                @AnnoDoc('Readable and writable property.')
                String title

                @AnnoDoc('Boolean property.')
                boolean enabled

                @AnnoDoc('Read-only property.')
                final String identifier = 'id'

                @AnnoDoc('Write-only property.')
                private String secret

                @AnnoDoc('Write-only property.')
                void setSecret(String secret) {
                    this.secret = secret
                }

                @AnnoDoc('Property documentation.')
                String endpoint

                @AnnoDoc('Explicit getter documentation.')
                String getEndpoint() {
                    endpoint
                }

                @AnnoDoc('Property documentation.')
                void setEndpoint(String endpoint) {
                    this.endpoint = endpoint
                }
            }
        ''')
        def classFile = new File(outputDirectory, 'contract/GroovyPropertyFixture.class').toPath()
        def projector = new SourceProjector(ProjectionPolicy.documentation())

        when:
        String projection = projector.projectToText(classFile)

        then:
        projection.contains('''  /**
   * Readable and writable property.
   */
  @Generated
  public String getTitle()''')
        projection.contains('''  /**
   * Readable and writable property.
   */
  @Generated
  public void setTitle(String value)''')
        projection.contains('''  /**
   * Boolean property.
   */
  @Generated
  public boolean getEnabled()''')
        projection.contains('''  /**
   * Boolean property.
   */
  @Generated
  public boolean isEnabled()''')
        projection.contains('''  /**
   * Boolean property.
   */
  @Generated
  public void setEnabled(boolean value)''')
        projection.contains('''  /**
   * Read-only property.
   */
  @Generated
  public String getIdentifier()''')
        !projection.contains('setIdentifier(')
        projection.contains('''  /**
   * Write-only property.
   */
  public void setSecret(String secret)''')
        projection.contains('''  /**
   * Explicit getter documentation.
   */
  public String getEndpoint()''')
        projection.contains('''  /**
   * Property documentation.
   */
  public void setEndpoint(String endpoint)''')
        !projection.contains('Property documentation.\n   */\n  public String getEndpoint()')

        and:
        assertProjectionCompiles(javac().withOptions('-parameters'), 'groovy-property-documentation',
                'contract.GroovyPropertyFixture', projection)
    }
}
