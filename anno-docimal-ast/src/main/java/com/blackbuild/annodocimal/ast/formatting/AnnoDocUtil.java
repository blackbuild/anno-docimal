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
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for annotation docBuilder. Contains methods for creating AnnoDoc annotations and
 * adding them to AST nodes. Unless otherwise stated, Javadoc means the raw text, without the leading
 * '* ', but correctly formatted and escaped in case of html.
 */
public class AnnoDocUtil {

    public static final ClassNode JAVADOC_ANNOTATION = ClassHelper.make(AnnoDoc.class);

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
    }

    public static void addDocumentation(@NotNull AnnotatedNode node, @NotNull DocBuilder docBuilder) {
        addDocumentation(node, docBuilder.toJavadoc());
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
}
