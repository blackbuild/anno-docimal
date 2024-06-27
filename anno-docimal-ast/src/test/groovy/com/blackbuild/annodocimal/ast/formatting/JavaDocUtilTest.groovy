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
