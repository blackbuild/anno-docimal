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

import com.blackbuild.annodocimal.annotations.AnnoDoc;
import groovy.lang.Groovydoc;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * Utility class for annotation docBuilder. Contains methods for creating, reading and parsing AnnoDoc annotations and
 * adding them to AST nodes. Unless otherwise stated, Javadoc means the raw text, without the leading
 * '* ', but correctly formatted and escaped in case of html.
 */
public class AnnoDocUtil {

    public static final ClassNode ANNODOC_ANNOTATION = ClassHelper.make(AnnoDoc.class);
    public static final ClassNode GROOVYDOC_ANNOTATION = ClassHelper.make(Groovydoc.class);
    public static final String DOC_TEXT_METADATA_KEY = DocText.class.getName();

    private AnnoDocUtil() {
        // Utility class
    }

    /**
     * Adds an AnnoDoc annotation to the given node with the given javadoc.
     *
     * @param node    the node to add the annotation to
     * @param javadoc the javadoc to add
     */
    public static void addDocumentation(@NotNull AnnotatedNode node, String javadoc) {
        if (javadoc == null || javadoc.isBlank()) return;
        node.addAnnotation(createDocumentationAnnotation(javadoc));
        node.putNodeMetaData(DOC_TEXT_METADATA_KEY, DocText.fromRawText(javadoc));
    }

    /**
     * Adds an AnnoDoc annotation to the given node with the given docBuilder.
     *
     * @param node      the node to add the annotation to
     * @param docBuilder the docBuilder to add
     */
    public static void addDocumentation(@NotNull AnnotatedNode node, @NotNull DocBuilder docBuilder) {
        List<String> paramNames;
        if (node instanceof MethodNode methodNode)
            paramNames = Arrays.stream(methodNode.getParameters()).map(Parameter::getName).toList();
        else
            paramNames = emptyList();
        addDocumentation(node, docBuilder.toJavadoc(paramNames));
    }

    /**
     * Creates an AnnoDoc annotation with the given javadoc.
     *
     * @param javadoc the javadoc to add
     * @return the created annotation
     */
    @NotNull
    public static AnnotationNode createDocumentationAnnotation(@NotNull String javadoc) {
        AnnotationNode annotation = new AnnotationNode(ANNODOC_ANNOTATION);
        annotation.addMember("value", new ConstantExpression(javadoc));
        return annotation;
    }

    /**
     * Selects normalized documentation from the canonical AnnoDoc carrier, falling back to runtime GroovyDoc.
     *
     * @param node the declaration to inspect
     * @return normalized documentation, or {@code null} when neither carrier contains documentation
     */
    public static @Nullable String getDocumentationCarrierValue(@NotNull AnnotatedNode node) {
        String canonical = normalizeDocumentation(annotationValue(node, ANNODOC_ANNOTATION.getName()));
        if (canonical != null) return canonical;
        return normalizeDocumentation(annotationValue(node, GROOVYDOC_ANNOTATION.getName()));
    }

    /**
     * Returns whether Groovy's runtime documentation carrier is already attached to a declaration.
     *
     * @param node the declaration to inspect
     * @return whether runtime GroovyDoc is present
     */
    public static boolean hasRuntimeGroovydoc(@NotNull AnnotatedNode node) {
        return node.getAnnotations().stream()
                .anyMatch(annotation -> annotation.getClassNode().getName().equals(GROOVYDOC_ANNOTATION.getName()));
    }

    /**
     * Removes source comment delimiters, the runtime GroovyDoc marker, leading line decoration, and shared indentation.
     *
     * @param text carrier or source-comment text
     * @return normalized documentation, or {@code null} for absent or blank text
     */
    public static @Nullable String normalizeDocumentation(@Nullable String text) {
        if (text == null) return null;
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String leadingTrimmed = normalized.stripLeading();
        if (leadingTrimmed.startsWith("/**")) {
            int end = leadingTrimmed.lastIndexOf("*/");
            if (end >= 3) {
                normalized = leadingTrimmed.substring(3, end);
                if (normalized.startsWith("@")) normalized = normalized.substring(1);
                normalized = normalized.replaceAll("(?m)^[\\t ]*\\*[\\t ]?", "");
            }
        }

        String[] lines = normalized.split("\n", -1);
        int minIndent = Arrays.stream(lines)
                .filter(line -> !line.isBlank())
                .mapToInt(AnnoDocUtil::leadingWhitespace)
                .min()
                .orElse(0);
        normalized = Arrays.stream(lines)
                .map(line -> line.length() >= minIndent ? line.substring(minIndent) : "")
                .collect(Collectors.joining("\n"))
                .strip();
        return normalized.isBlank() ? null : normalized;
    }

    private static @Nullable String annotationValue(AnnotatedNode node, String annotationName) {
        return node.getAnnotations().stream()
                .filter(annotation -> annotation.getClassNode().getName().equals(annotationName))
                .map(annotation -> annotation.getMember("value"))
                .filter(ConstantExpression.class::isInstance)
                .map(ConstantExpression.class::cast)
                .map(ConstantExpression::getValue)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static int leadingWhitespace(String line) {
        int result = 0;
        while (result < line.length() && Character.isWhitespace(line.charAt(result))) result++;
        return result;
    }
}
