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
package com.blackbuild.annodocimal.ast.formatting;


import org.codehaus.groovy.ast.*;

import java.util.stream.Collectors;

import static java.util.Arrays.stream;

/**
 * Helper methods for formatting JavaDoc.
 */
public class JavaDocUtil {
    private JavaDocUtil() {
        // Utility class
    }

    public static String toLinkString(AnnotatedNode node) {
        if (node instanceof ClassNode)
            return toName((ClassNode) node);
        if (node instanceof FieldNode)
            return toName(node.getDeclaringClass()) + "#" + ((FieldNode) node).getName();
        if (node instanceof ConstructorNode)
            return toName(node.getDeclaringClass()) + "(" + toParamString(((ConstructorNode) node).getParameters()) + ")";
        if (node instanceof MethodNode)
            return toName(node.getDeclaringClass()) + "#" + ((MethodNode) node).getName() + "(" + toParamString(((MethodNode) node).getParameters()) + ")";
        throw new IllegalArgumentException("Unsupported node type: " + node.getClass().getName());
    }

    public static String toName(ClassNode node) {
        if (node.isArray())
            return toName(node.getComponentType()) + "[]";
        return node.getName().replace('$', '.');
    }

    public static String toParamString(Parameter[] parameters) {
        return stream(parameters)
                .map(Parameter::getType)
                .map(JavaDocUtil::toName)
                .collect(Collectors.joining(","));
    }
}
