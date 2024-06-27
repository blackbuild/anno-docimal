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
