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
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

/**
 * Utility class for annotation docBuilder. Contains methods for creating, reading and parsing AnnoDoc annotations and
 * adding them to AST nodes. Unless otherwise stated, Javadoc means the raw text, without the leading
 * '* ', but correctly formatted and escaped in case of html.
 */
public class AnnoDocUtil {

    public static final ClassNode JAVADOC_ANNOTATION = ClassHelper.make(AnnoDoc.class);
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
        if (node instanceof MethodNode)
            paramNames = Arrays.stream(((MethodNode) node).getParameters()).map(Parameter::getName).collect(toList());
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
        AnnotationNode annotation = new AnnotationNode(JAVADOC_ANNOTATION);
        annotation.addMember("value", new ConstantExpression(javadoc));
        return annotation;
    }

    /**
     * Returns the value of the AnnoDoc annotation on the given node, or the given default value if no
     * AnnoDoc annotation is present.
     *
     * @param node         the node to get the annotation from
     * @param defaultValue the default value to return if no annotation is present
     * @return the value of the annotation, or the default value
     */
    public static String getAnnoDocValue(@NotNull AnnotatedNode node, @Nullable String defaultValue) {
        return node.getAnnotations().stream()
                .filter(annotation -> annotation.getClassNode().equals(JAVADOC_ANNOTATION))
                .findFirst()
                .map(annotation -> annotation.getMember("value"))
                .map(ConstantExpression.class::cast)
                .map(ConstantExpression::getValue)
                .map(Object::toString)
                .filter(not(String::isBlank))
                .orElse(defaultValue);
    }

    /**
     * Returns the pre parsed documentation of the given node, or the preparsed default value if no
     * AnnoDoc annotation is present.
     *
     * @param node         the node to get the documentation from
     * @param defaultValue the default value to return if no annotation is present
     * @return the pre parse documentation object
     */
    public static DocText getDocText(@NotNull AnnotatedNode node, @Nullable String defaultValue) {
        Object metaData = node.getNodeMetaData(DOC_TEXT_METADATA_KEY);
        if (metaData == null) {
            String annoDocValue = getAnnoDocValue(node, defaultValue);
            metaData = DocText.fromRawText(annoDocValue);
            node.putNodeMetaData(DOC_TEXT_METADATA_KEY, metaData);
        }
        return (DocText) metaData;
    }

    public static String getJavaDoc(AnnotatedElement element, String defaultValue) {
        AnnoDoc annotation = element.getAnnotation(AnnoDoc.class);
        return annotation == null ? defaultValue : annotation.value();
    }

    public static String getDocumentation(AnnotatedElement element) {
        return getJavaDoc(element, null);
    }
}
