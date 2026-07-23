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
import spock.lang.Issue
import spock.lang.See
import spock.lang.Tag

@Issue('9')
@Tag('documentary')
@See('https://github.com/blackbuild/anno-docimal/blob/master/docs/user/usage.md#groovy-property-documentation')
class GroovyPropertyDocumentationDocumentaryTest extends ClassGeneratingSpecification {

    def 'captures property documentation and respects explicit accessor documentation'() {
        when:
        createClass('''
            package dummy

            import com.blackbuild.annodocimal.annotations.InlineJavadocs

            @InlineJavadocs
            class DocumentedProperties {
                /** A title callers can read and write. */
                String title

                /** A service endpoint. */
                String endpoint

                /** Returns the normalized endpoint. */
                String getEndpoint() { endpoint }

                void setEndpoint(String endpoint) { this.endpoint = endpoint }
            }
        ''')

        then:
        clazz.getDeclaredField('title').getAnnotation(AnnoDoc).value() == 'A title callers can read and write.'
        clazz.getDeclaredField('endpoint').getAnnotation(AnnoDoc).value() == 'A service endpoint.'
        clazz.getMethod('getEndpoint').getAnnotation(AnnoDoc).value() == 'Returns the normalized endpoint.'
        clazz.getMethod('setEndpoint', String).getAnnotation(AnnoDoc).value() == 'A service endpoint.'
    }
}
