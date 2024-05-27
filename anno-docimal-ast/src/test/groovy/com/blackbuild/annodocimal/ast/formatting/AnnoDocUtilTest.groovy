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
//file:noinspection GrPackage
//file:noinspection UnnecessaryQualifiedReference
package com.blackbuild.annodocimal.ast.formatting

import com.blackbuild.annodocimal.ast.ClassGeneratingSpecification
import com.blackbuild.annodocimal.ast.MockableTransformation
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.customizers.ImportCustomizer

class AnnoDocUtilTest extends ClassGeneratingSpecification {

    def "Documentation is retrieved from AST"() {
        given:
        createClass '''
package dummy

import com.blackbuild.annodocimal.annotations.AnnoDoc
import com.blackbuild.annodocimal.ast.MockAST
import com.blackbuild.annodocimal.ast.MockableTransformation

@AnnoDoc("A class")
class Provider {
    @AnnoDoc("A field")
    String name
    
    @AnnoDoc("A method")
    void method() {}
}

class Consumer {
    @ReadDocFrom(value = AnnoDocUtilTest.MyAction, type = Provider)
    Object fromClass
    
    @ReadDocFrom(value = AnnoDocUtilTest.MyAction, type = Provider, field = "name")
    Object fromField
    
    @ReadDocFrom(value = AnnoDocUtilTest.MyAction, type = Provider, method = "method")
    Object fromMethod
}
'''
        expect:
        astData.fromClass == 'A class'
        astData.fromField == 'A field'
        astData.fromMethod == 'A method'
    }

    def "Documentation is retrieved from Class object"() {
        given:
        createClass '''
package provider

import com.blackbuild.annodocimal.annotations.AnnoDoc
import com.blackbuild.annodocimal.ast.MockAST
import com.blackbuild.annodocimal.ast.MockableTransformation

@AnnoDoc("A class")
class Provider {
    @AnnoDoc("A field")
    String name
    
    @AnnoDoc("A method")
    void method() {}
}
'''
        createClassFromSecondaryloader '''

package dummy

import com.blackbuild.annodocimal.annotations.AnnoDoc
import com.blackbuild.annodocimal.ast.MockAST
import com.blackbuild.annodocimal.ast.MockableTransformation
import provider.Provider

class Consumer {
    @ReadDocFrom(value = AnnoDocUtilTest.MyAction, type = Provider)
    Object fromClass
    
    @ReadDocFrom(value = AnnoDocUtilTest.MyAction, type = Provider, field = "name")
    Object fromField
    
    @ReadDocFrom(value = AnnoDocUtilTest.MyAction, type = Provider, method = "method")
    Object fromMethod
}
'''
        expect:
        astData.fromClass == 'A class'
        astData.fromField == 'A field'
        astData.fromMethod == 'A method'
    }

    static class MyAction implements MockableTransformation.Action {

        @Override
        void handle(AnnotationNode annotation, AnnotatedNode target, SourceUnit sourceUnit) {
            def type = annotation.getMember("type").getType()

            AnnotatedNode provider
            if (annotation.getMember("method")) {
                provider = type.getDeclaredMethods(annotation.getMember("method").getText()).first()
            } else if (annotation.getMember("field")) {
                provider = type.getDeclaredField(annotation.getMember("field").getText())
            } else {
                provider = type
            }

            astData.put(target.getName(), AnnoDocUtil.getAnnoDocValue(provider, null));
        }
    }


}
