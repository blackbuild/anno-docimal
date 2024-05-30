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
package com.blackbuild.annodocimal.ast.extractor;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;

import static com.blackbuild.annodocimal.ast.formatting.AnnoDocUtil.ANNODOC_ANNOTATION;
import static java.util.function.Predicate.not;

public class ASTExtractor {

    public static final String DOC_METADATA_KEY = ASTExtractor.class.getName() + ".doc";
    public static final String EMPTY_DOC = "\u0000";

    private ASTExtractor() {
        // Utility class
    }

    public static String extractDocumentation(AnnotatedNode element, String defaultValue) {
        String metaData = element.getNodeMetaData(DOC_METADATA_KEY);

        if (EMPTY_DOC.equals(metaData)) return defaultValue;
        else if (metaData != null) return metaData;

        metaData = extractDocumentationFromElement(element);
        if (metaData != null) {
            element.putNodeMetaData(DOC_METADATA_KEY, metaData);
            return metaData;
        }
        element.putNodeMetaData(DOC_METADATA_KEY, EMPTY_DOC);
        return defaultValue;
    }

    private static String extractDocumentationFromElement(AnnotatedNode element) {
        String result = getAnnoDocValue(element);
        if (result != null) return result;

        AnnotatedElement annotatedElement = toAnnotatedElement(element);
        if (annotatedElement == null) return null;
        return ClassDocExtractor.extractDocumentation(annotatedElement, null);
    }

    public static AnnotatedElement toAnnotatedElement(AnnotatedNode element) {
        if (element instanceof ClassNode) return toClass((ClassNode) element);
        if (element instanceof ConstructorNode) return toConstructor((ConstructorNode) element);
        if (element instanceof MethodNode) return toMethod((MethodNode) element);
        if (element instanceof FieldNode) return toField((FieldNode) element);
        return null;
    }

    private static AnnotatedElement toConstructor(ConstructorNode element) {
        Class<?> declaringClass = toClass(element.getDeclaringClass());
        if (declaringClass == null) return null;

        String[] paramTypes = getParamTypes(element);

        return Arrays.stream(declaringClass.getDeclaredConstructors())
                .filter(method -> paramsMatch(method.getParameterTypes(), paramTypes))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Found not matching constructor for " + element + " in " + declaringClass));
    }

    private static String @NotNull [] getParamTypes(MethodNode element) {
        return Arrays.stream(element.getParameters())
                .map(Parameter::getOriginType)
                .map(ClassNode::getName)
                .toArray(String[]::new);
    }

    private static AnnotatedElement toField(FieldNode element) {
        Class<?> declaringClass = toClass(element.getDeclaringClass());
        if (declaringClass == null) return null;

        return Arrays.stream(declaringClass.getDeclaredFields())
                .filter(field -> field.getName().equals(element.getName()))
                .findFirst()
                .orElse(null);
    }

    private static AnnotatedElement toMethod(MethodNode element) {
        Class<?> declaringClass = toClass(element.getDeclaringClass());
        if (declaringClass == null) return null;

        String[] paramTypes = getParamTypes(element);

        return Arrays.stream(declaringClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(element.getName()))
                .filter(method -> paramsMatch(method.getParameterTypes(), paramTypes))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Found not matching method for " + element + " in " + declaringClass));
    }

    private static boolean paramsMatch(Class<?>[] actualTypes, String[] expectedTypeNames) {
        String[] actualTypeNames = Arrays.stream(actualTypes).map(Class::getName).toArray(String[]::new);
        return Arrays.equals(actualTypeNames, expectedTypeNames);
    }

    private static Class<?> toClass(ClassNode element) {
        if (element.isResolved())
            return element.getTypeClass();
        return null;
    }

    public static String getAnnoDocValue(@NotNull AnnotatedNode node) {
        return node.getAnnotations().stream()
                .filter(annotation -> annotation.getClassNode().equals(ANNODOC_ANNOTATION))
                .findFirst()
                .map(annotation -> annotation.getMember("value"))
                .map(ConstantExpression.class::cast)
                .map(ConstantExpression::getValue)
                .map(Object::toString)
                .filter(not(String::isBlank))
                .orElse(null);
    }

}
