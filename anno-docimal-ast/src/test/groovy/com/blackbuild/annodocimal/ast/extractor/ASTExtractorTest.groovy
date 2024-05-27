//file:noinspection GrPackage
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
    
    @AstHelper(value = ASTExtractorTest.MyAction, type = AClass, field = "field")
    Object fromField
    
    @AstHelper(value = ASTExtractorTest.MyAction, type = AClass.InnerClass)
    Object fromInnerClass
}'''

        then:
        astData.fromType == AClass
        astData.fromConstructor == AClass.getConstructor()
        astData.fromMethod == AClass.getDeclaredMethod("doIt", String)
        astData.fromField == AClass.getDeclaredField("field")
        astData.fromInnerClass == AClass.InnerClass


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
        }
    }


}
