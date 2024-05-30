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
package com.blackbuild.annodocimal.ast.extractor

import com.blackbuild.annodocimal.ast.ClassGeneratingSpecification
import com.blackbuild.annodocimal.ast.MockableTransformation
import com.blackbuild.annodocimal.ast.extractor.mock.AClass
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.control.SourceUnit

class ASTExtractorTest extends ClassGeneratingSpecification {

    def "AST has access to javadoc provided by properties"() {
        when:
        createClass '''
package dummy

import com.blackbuild.annodocimal.ast.extractor.AstHelper
import com.blackbuild.annodocimal.ast.extractor.mock.AClass


class Consumer {
    @AstHelper(value = ASTExtractorTest.MyAction, type = AClass)
    Object fromType

    @AstHelper(value = ASTExtractorTest.MyAction, type = AClass, fromConstructor = true)
    Object fromConstructor
    
    @AstHelper(value = ASTExtractorTest.MyAction, type = AClass, method = "doIt")
    Object fromMethod
    
    @AstHelper(value = ASTExtractorTest.MyAction, type = AClass, method = "noJavaDocMethod")
    Object noJavaDocMethod
    
    @AstHelper(value = ASTExtractorTest.MyAction, type = AClass, field = "field")
    Object fromField
    
    @AstHelper(value = ASTExtractorTest.MyAction, type = AClass.InnerClass)
    Object fromInnerClass
}'''

        then:
        astData.fromType == AClass
        astData.fromTypeDoc == "A class for testing."
        astData.fromConstructor == AClass.getConstructor()
        astData.fromConstructorDoc == "Creates a new instance of {@link AClass}."
        astData.fromMethod == AClass.getDeclaredMethod("doIt", String)
        astData.fromMethodDoc == '''A method that does something.
@param what the thing to do
@return the result of doing it'''
        astData.containsKey("noJavaDocMethod")
        astData.noJavaDocMethodDoc == null
        astData.fromField == AClass.getDeclaredField("field")
        astData.fromFieldDoc == "A field."
        astData.fromInnerClass == AClass.InnerClass
        astData.fromInnerClassDoc == 'An inner class.'

        and:
        astData.keySet().findAll { it.endsWith("Doc") }.each { astData[it] == astData[it] + "2" }
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
            } else if (annotation.getMember("fromConstructor")) {
                provider = type.getDeclaredConstructors().first()
            } else {
                provider = type
            }

            astData.put(target.name, ASTExtractor.toAnnotatedElement(provider))
            astData.put(target.name + "Doc", ASTExtractor.extractDocumentation(provider, null))
            astData.put(target.name + "Doc2", ASTExtractor.extractDocumentation(provider, null))
        }
    }


}
