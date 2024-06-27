package com.blackbuild.annodocimal.ast.formatting

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import spock.lang.Specification

class JavaDocUtilTest extends Specification {

    def "links are correctly generated"() {
        expect:
        JavaDocUtil.toLinkString(node) == result

        where:
        node                        || result
        type(String)                || "java.lang.String"
        method(String, "toString")  || "java.lang.String#toString()"
        constructor(String)         || "java.lang.String()"
        method(String, "substring", int) || "java.lang.String#substring(int)"
        method(String, "substring", int, int) || "java.lang.String#substring(int,int)"
        field(Inner, "field")       || "com.blackbuild.annodocimal.ast.formatting.JavaDocUtilTest.Inner#field"
        type(Inner)                 || "com.blackbuild.annodocimal.ast.formatting.JavaDocUtilTest.Inner"
        constructor(Inner)          || "com.blackbuild.annodocimal.ast.formatting.JavaDocUtilTest.Inner()"
        method(Inner, "method")     || "com.blackbuild.annodocimal.ast.formatting.JavaDocUtilTest.Inner#method()"
        method(Inner, "anotherMethod", String, Inner[]) || "com.blackbuild.annodocimal.ast.formatting.JavaDocUtilTest.Inner#anotherMethod(java.lang.String,com.blackbuild.annodocimal.ast.formatting.JavaDocUtilTest.Inner[])"
        method(Inner, "yetAnotherMethod", Inner[]) || "com.blackbuild.annodocimal.ast.formatting.JavaDocUtilTest.Inner#yetAnotherMethod(com.blackbuild.annodocimal.ast.formatting.JavaDocUtilTest.Inner[])"
    }

    ClassNode type(Class<?> type) {
        return ClassHelper.make(type)
    }

    MethodNode method(Class<?> owner, String name, Class<?>... parameters) {
        return type(owner).getMethod(name, toParameters(parameters))
    }

    ConstructorNode constructor(Class<?> owner, Class<?>... parameters) {
        return type(owner).getDeclaredConstructor(toParameters(parameters))
    }

    FieldNode field(Class<?> owner, String name) {
        return type(owner).getField(name)
    }

    Parameter[] toParameters(Class<?>... types) {
        return types.collect { new Parameter(type(it), "p${it.simpleName}") }
    }

    static class Inner {
        String field
        String method() { return null }
        void anotherMethod(String name, Inner[] values) {}
        void yetAnotherMethod(Inner... values) {}
    }

}
