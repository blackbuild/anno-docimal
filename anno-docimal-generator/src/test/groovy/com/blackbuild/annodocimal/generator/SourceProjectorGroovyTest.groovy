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
            class GroovyFixture<T> {
                public static final int MIN_VALUE = 7
                T value
                void next() {}
                Object getProperty(String name) { null }
                void $api() {}
            }
        ''')
        def classFile = new File(outputDirectory, clazz.name.replace('.', '/') + '.class').toPath()

        when:
        String documented = new SourceProjector(ProjectionPolicy.documentation()).projectToText(classFile)
        ProjectionPolicy withRuntime = ProjectionPolicy.builder().includeGroovyRuntimeArtifacts(true).build()
        String runtimeAware = new SourceProjector(withRuntime).projectToText(classFile)

        then:
        documented == '''package dummy;

import groovy.transform.Generated;
import java.lang.Object;
import java.lang.String;

public class GroovyFixture<T> {
  public static final int MIN_VALUE = 7;

  @Generated
  public GroovyFixture() {
  }

  public void next() {
  }

  public Object getProperty(String name) {
    return null;
  }

  public void $api() {
  }

  @Generated
  public T getValue() {
    return null;
  }

  @Generated
  public void setValue(T value) {
  }
}
'''

        and:
        runtimeAware == '''package dummy;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.transform.Generated;
import groovy.transform.Internal;
import java.beans.Transient;
import java.lang.Object;
import java.lang.String;

public class GroovyFixture<T> implements GroovyObject {
  public static final int MIN_VALUE = 7;

  @Generated
  public GroovyFixture() {
  }

  public void next() {
  }

  public Object getProperty(String name) {
    return null;
  }

  public void $api() {
  }

  @Generated
  @Internal
  @Transient
  public MetaClass getMetaClass() {
    return null;
  }

  @Generated
  @Internal
  public void setMetaClass(MetaClass mc) {
  }

  @Generated
  public T getValue() {
    return null;
  }

  @Generated
  public void setValue(T value) {
  }
}
'''
    }
}
