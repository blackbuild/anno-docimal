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

class SourceProjectorGroovyTest extends ClassGeneratingTest {

    def "Groovy runtime scaffolding is optional while generated language APIs remain visible"() {
        given:
        createClass('''
            package dummy
            class GroovyFixture {
                String value
            }
        ''')
        def classFile = new File(outputDirectory, clazz.name.replace('.', '/') + '.class').toPath()

        when:
        String documented = new SourceProjector(ProjectionPolicy.documentation()).projectToText(classFile)
        ProjectionPolicy withRuntime = ProjectionPolicy.builder().includeGroovyRuntimeArtifacts(true).build()
        String runtimeAware = new SourceProjector(withRuntime).projectToText(classFile)

        then:
        documented.contains('public String getValue()')
        documented.contains('public void setValue(String value)')
        !documented.contains('GroovyObject')
        !documented.contains('getMetaClass')
        !documented.contains('setMetaClass')

        and:
        runtimeAware.contains('implements GroovyObject')
        runtimeAware.contains('public MetaClass getMetaClass()')
        runtimeAware.contains('public void setMetaClass(MetaClass mc)')
    }
}
