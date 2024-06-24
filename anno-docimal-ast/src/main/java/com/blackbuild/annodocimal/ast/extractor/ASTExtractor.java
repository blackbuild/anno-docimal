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

import com.blackbuild.annodocimal.ast.formatting.DocText;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.blackbuild.annodocimal.ast.formatting.AnnoDocUtil.ANNODOC_ANNOTATION;
import static java.util.function.Predicate.not;

public class ASTExtractor {

    public static final String DOC_METADATA_KEY = ASTExtractor.class.getName() + ".doc";
    public static final String DOCTEXT_METADATA_KEY = ASTExtractor.class.getName() + ".doctext";
    public static final String EMPTY_DOC = "\u0000";

    private ASTExtractor() {
        // Utility class
    }

    public static String extractDocumentation(AnnotatedNode element) {
        return extractDocumentation(element, null);
    }

    public static String extractDocumentation(AnnotatedNode element, String defaultValue) {
        String existingMetaData = element.getNodeMetaData(DOC_METADATA_KEY);

        if (EMPTY_DOC.equals(existingMetaData)) return defaultValue;
        else if (existingMetaData != null) return existingMetaData;

        String doc = extractDocumentationFromElement(element);
        if (doc != null) {
            element.putNodeMetaData(DOC_METADATA_KEY, doc);
            return doc;
        }
        element.putNodeMetaData(DOC_METADATA_KEY, EMPTY_DOC);
        return defaultValue;
    }

    public static DocText extractDocText(AnnotatedNode element) {
        DocText docText = element.getNodeMetaData(DOCTEXT_METADATA_KEY);
        if (docText != null) return docText;
        docText = DocText.fromRawText(extractDocumentation(element));
        element.putNodeMetaData(DOCTEXT_METADATA_KEY, docText);
        return docText;
    }

    public static DocText extractDocText(AnnotatedNode element, String defaultValue) {
        DocText docText = extractDocText(element);
        if (docText.isEmpty()) return DocText.fromRawText(defaultValue);
        return docText;
    }

    private static String extractDocumentationFromClassHierarchy(ClassNode element) {
        return InheritanceUtil.getHierarchyStream(element.getDeclaringClass())
                .map(ASTExtractor::extractDocumentationFromElement)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String extractDocumentationFromMethodHierarchy(MethodNode element) {
        return InheritanceUtil.getMethodHierarchy(element)
                .map(ASTExtractor::extractDocumentationFromElement)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static String extractDocumentationFromElement(AnnotatedNode element) {
        String result = getAnnoDocValue(element);
        if (result != null) return result;

        return ClassDocExtractor.extractDocumentation(element);
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
