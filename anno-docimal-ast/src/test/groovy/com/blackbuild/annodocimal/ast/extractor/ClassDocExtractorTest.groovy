package com.blackbuild.annodocimal.ast.extractor

import com.blackbuild.annodocimal.ast.extractor.mock.AClass
import spock.lang.Specification

class ClassDocExtractorTest extends Specification {

    def "extraction from AClass"() {
        when:
        def clazz = AClass.class

        then:
        ClassDocExtractor.extractDocumentation(clazz, "bla") == "A class for testing."

        expect:
        ClassDocExtractor.extractDocumentation(clazz.getDeclaredMethod("aMethod"), "bla") == "A method that does nothing."
    }




}
