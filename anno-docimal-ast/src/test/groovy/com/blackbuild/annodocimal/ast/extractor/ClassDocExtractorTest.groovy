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
package com.blackbuild.annodocimal.ast.extractor

import com.blackbuild.annodocimal.ast.extractor.mock.AClass
import org.codehaus.groovy.ast.ClassHelper
import spock.lang.Specification

class ClassDocExtractorTest extends Specification {

    def "extraction from AClass"() {
        when:
        def clazz = ClassHelper.make(AClass.class)

        then:
        ClassDocExtractor.extractDocumentation(clazz, "bla") == "A class for testing."

        expect:
        ClassDocExtractor.extractDocumentation(clazz.getDeclaredMethod("aMethod"), "bla") == "A method that does nothing."
        ClassDocExtractor.extractDocumentation(clazz.getDeclaredField("field"), "bla") == "A field."
        ClassDocExtractor.extractDocumentation(ClassHelper.make(AClass.InnerClass), "bla") == "An inner class."
        ClassDocExtractor.extractDocumentation(ClassHelper.make(AClass.InnerClass).getDeclaredMethod("innerMethod"), "bla") == "Another method that does nothing."
    }




}
