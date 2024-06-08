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

import com.blackbuild.annodocimal.ast.ClassGeneratingSpecification
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.Parameter

import java.util.function.Predicate

class InheritanceUtilTest extends ClassGeneratingSpecification {

    ClassNode origin
    Predicate<ClassNode> hasGoal = { ClassNode cn ->
        cn.getAnnotations().any { it.classNode.name == "Goal" }
    }


    @Override
    def setup() {
        createClass '''
import java.lang.annotation.*
@Retention(RetentionPolicy.RUNTIME)
@interface Goal {}
'''

    }

    def "Finds itself"() {
        when:
        createClass '''
            @Goal class A {}
        '''
        origin = classNode("A")

        then:
        InheritanceUtil.findMatchingClass(origin, hasGoal).get().name == "A"
    }

    def "Finds itself in hierarchy"() {
        when:
        createClass '''
            class A {}
            @Goal class B extends A {}
        '''
        origin = classNode("B")

        then:
        InheritanceUtil.getClassHierarchy(origin)*.name == ["B", "A", "java.lang.Object", "groovy.lang.GroovyObject"]
        InheritanceUtil.findMatchingClass(origin, hasGoal).get().name == "B"
    }

    def "Finds parent in hierarchy"() {
        when:
        createClass '''
            @Goal class A {}
            class B extends A {}
        '''
        origin = classNode("B")

        then:
        InheritanceUtil.findMatchingClass(origin, hasGoal).get().name == "A"
    }

    def "Finds interface in hierarchy"() {
        when:
        createClass '''
            @Goal interface I {}
            class A {}
            class B extends A implements I {}
        '''
        origin = classNode("B")

        then:
        InheritanceUtil.findMatchingClass(origin, hasGoal).get().name == "I"
    }

    def "Finds prioritizes class over interface in hierarchy"() {
        when:
        createClass '''
            @Goal interface I {}
            @Goal class A {}
            class B extends A implements I {}
        '''
        origin = classNode("B")

        then:
        InheritanceUtil.findMatchingClass(origin, hasGoal).get().name == "A"
    }

    def "finds overridden and implemented methods"() {
        when:
        createClass '''
            interface I {
                void method()
            }
            class A {
                void method() {}
            }
            class B extends A implements I {
                @Override
                void method() {}
            }
        '''
        origin = classNode("B")
        def method = origin.getMethod("method", Parameter.EMPTY_ARRAY)

        then:
        InheritanceUtil.getMethodHierarchy(method)*.declaringClass.name == ["B", "A", "I"]
    }


    ClassNode classNode(String name) {
        return ClassHelper.make(getClass(name))
    }
}
