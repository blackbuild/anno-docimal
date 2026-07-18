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
package com.blackbuild.annodocimal.ast;

import com.blackbuild.annodocimal.annotations.AnnoDoc;
import com.blackbuild.annodocimal.ast.extractor.ASTExtractor;
import com.blackbuild.annodocimal.ast.extractor.ClassDocExtractor;
import com.blackbuild.annodocimal.ast.formatting.AnnoDocUtil;
import com.blackbuild.annodocimal.ast.formatting.DocText;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ConstantExpression;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Supported documentation capabilities for Groovy AST transformations.
 */
public final class AstDocumentation {

    private AstDocumentation() {
        // Capability facade
    }

    /**
     * Extracts documentation attached directly to a declaration, without hierarchy resolution.
     *
     * @param node the declaration to inspect
     * @return exact documentation when present
     */
    public static Optional<Documentation> extractExact(AnnotatedNode node) {
        String text = annotationText(node);
        if (text == null) text = ClassDocExtractor.extractDocumentation(node);
        return text == null || text.isBlank() ? Optional.empty() : Optional.of(Documentation.parse(text));
    }

    /**
     * Replaces AnnoDocimal's carrier on a declaration without changing third-party annotations.
     *
     * @param node the declaration to update
     * @param documentation the semantic documentation to attach
     */
    public static void attach(AnnotatedNode node, Documentation documentation) {
        String target = targetDescription(node);
        removeAnnoDoc(node);
        if (documentation == null || documentation.isEmpty()) {
            clearCachedDocumentation(node);
            return;
        }
        String rendered = documentation.renderForParameters(parameterNames(node), target);
        if (rendered.isBlank()) {
            clearCachedDocumentation(node);
            return;
        }
        node.addAnnotation(AnnoDocUtil.createDocumentationAnnotation(rendered));
        clearCachedDocumentation(node);
    }

    /**
     * Parses normalized textual content and attaches it as AnnoDocimal documentation.
     *
     * @param node the declaration to update
     * @param text normalized documentation content
     */
    public static void attachText(AnnotatedNode node, String text) {
        attach(node, Documentation.parse(text));
    }

    /**
     * Creates a canonical documentation reference for a class or member declaration.
     *
     * @param node an AST declaration supported by Javadoc references
     * @return the first-class reference
     */
    public static Documentation.Link referenceTo(AnnotatedNode node) {
        if (node instanceof ClassNode classNode) return Documentation.Link.text(className(classNode));
        if (node instanceof FieldNode fieldNode) return Documentation.Link.text(className(requireDeclaringClass(fieldNode)) + "#" + fieldNode.getName());
        if (node instanceof ConstructorNode constructorNode) {
            return Documentation.Link.text(className(requireDeclaringClass(constructorNode)) + "(" + parameterTypes(constructorNode.getParameters()) + ")");
        }
        if (node instanceof MethodNode methodNode) {
            return Documentation.Link.text(className(requireDeclaringClass(methodNode)) + "#" + methodNode.getName() + "(" + parameterTypes(methodNode.getParameters()) + ")");
        }
        throw new IllegalArgumentException("Cannot create a documentation reference for " + node.getClass().getName());
    }

    private static String annotationText(AnnotatedNode node) {
        return node.getAnnotations().stream()
                .filter(annotation -> annotation.getClassNode().getName().equals(AnnoDoc.class.getName()))
                .findFirst()
                .map(annotation -> annotation.getMember("value"))
                .filter(ConstantExpression.class::isInstance)
                .map(ConstantExpression.class::cast)
                .map(ConstantExpression::getValue)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(value -> !value.isBlank())
                .orElse(null);
    }

    private static void removeAnnoDoc(AnnotatedNode node) {
        node.getAnnotations().removeIf(annotation -> annotation.getClassNode().getName().equals(AnnoDoc.class.getName()));
    }

    private static void clearCachedDocumentation(AnnotatedNode node) {
        node.removeNodeMetaData(ASTExtractor.DOC_METADATA_KEY);
        node.removeNodeMetaData(ASTExtractor.DOCTEXT_METADATA_KEY);
        node.removeNodeMetaData(AnnoDocUtil.DOC_TEXT_METADATA_KEY);
        node.removeNodeMetaData(DocText.class.getName());
    }

    private static Collection<String> parameterNames(AnnotatedNode node) {
        if (!(node instanceof MethodNode methodNode)) return List.of();
        return Arrays.stream(methodNode.getParameters()).map(Parameter::getName).toList();
    }

    private static String targetDescription(AnnotatedNode node) {
        try {
            return "documentation target " + referenceTo(node).getTarget();
        } catch (IllegalArgumentException ignored) {
            return "documentation target " + node.getClass().getName();
        }
    }

    private static ClassNode requireDeclaringClass(AnnotatedNode node) {
        ClassNode declaringClass = node.getDeclaringClass();
        if (declaringClass == null) throw new IllegalArgumentException("Cannot create a documentation reference without a declaring class");
        return declaringClass;
    }

    private static String className(ClassNode node) {
        if (node == null || node.getName() == null || node.getName().isBlank()) throw new IllegalArgumentException("Cannot create a documentation reference without a class name");
        return node.isArray() ? className(node.getComponentType()) + "[]" : node.getName().replace('$', '.');
    }

    private static String parameterTypes(Parameter[] parameters) {
        return Arrays.stream(parameters).map(Parameter::getType).map(AstDocumentation::className).collect(Collectors.joining(","));
    }
}
