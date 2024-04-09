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
import org.jetbrains.annotations.Nullable;

import java.text.BreakIterator;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

/**
 * Utility class for annotation docBuilder. Contains methods for creating, reading and parsing AnnoDoc annotations and
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
     * Returns the first sentence of the AnnoDoc annotation on the given node, or the given default value if no
     * AnnoDoc annotation is present.
     *
     * @param node         the node to get the annotation from
     * @param defaultValue the default value to return if no annotation is present
     * @return the first sentence of the annotation, or the default value
     */
    public static String getFirstSentenceOfAnnoDocValue(AnnotatedNode node, String defaultValue) {
        String text = getAnnoDocValue(node, null);

        if (text == null) return defaultValue;

        return getFirstSentenceOfJavadoc(text);
    }

    /**
     * Returns the first sentence of the given Javadoc text.
     *
     * @param text the Javadoc text
     * @return the first sentence
     */
    public static @NotNull String getFirstSentenceOfJavadoc(String text) {
        // form SimpleGroovyDoc
        text = text.replaceFirst("(?ms)<p>.*", "").trim();
        // assume completely blank line signifies end of sentence
        text = text.replaceFirst("(?ms)\\n\\s*\\n.*", "").trim();
        // assume @tag signifies end of sentence
        text = text.replaceFirst("(?ms)\\n\\s*@([a-z]+)\\s.*", "").trim();
        // Comment Summary using first sentence (Locale sensitive)
        BreakIterator boundary = BreakIterator.getSentenceInstance(Locale.getDefault());
        boundary.setText(text);
        int start = boundary.first();
        int end = boundary.next();
        if (start > -1 && end > -1) {
            // need to abbreviate this comment for the summary
            text = text.substring(start, end);
        }
        return text.trim();
    }

    /**
     * Returns the text of the named tag in the AnnoDoc annotation on the given node, or the given default value if no
     * AnnoDoc annotation is present or the tag is not present.
     *
     * @param tagName      the name of the tag to get
     * @param node         the node to get the annotation from
     * @param defaultValue the default value to return if no annotation is present or the tag is not present
     * @return the text of the tag, or the default value
     */
    public static String getNamedTagText(String tagName, AnnotatedNode node, String defaultValue) {
        String text = getAnnoDocValue(node, null);
        return getNamedTagText(tagName, text).orElse(defaultValue);
    }

    /**
     * Returns the text of the named tag in the given Javadoc text, or an empty optional if the tag is not present.
     *
     * @param tagName the name of the tag to get
     * @param text    the Javadoc text
     * @return the text of the tag, or an empty optional
     */
    public static Optional<String> getNamedTagText(String tagName, String text) {
        return Optional.ofNullable(getAllTags(text).get(tagName));
    }

    /**
     * Returns all tags in the given javadoc. Tags are normalized, i.e. multiple whitespaces are condensed.
     * The value is everything after the tag, for param or throws this includes the parameter name. For empty tags
     * the value is an empty string. If a tag is present multiple times, only the first occurrence is returned.
     *
     * @param text The javadoc text
     * @return a map of all tags in the annotation
     */
    public static Map<String, String> getAllTags(String text) {
        return getAllMultiTags(text).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
    }

    /**
     * Returns all tags in the given javadoc. Tags are normalized, i.e. multiple whitespaces are condensed.
     * The value is everything after the tag, for param or throws this includes the parameter name. For empty tags
     * the value is an empty string.
     *
     * @param text The javadoc text
     * @return a map of all tags in the annotation
     */
    public static Map<String, List<String>> getAllMultiTags(String text) {
        if (text == null || !text.contains("@")) return Collections.emptyMap();

        Map<String, List<String>> result = new java.util.HashMap<>();

        StringBuilder tagText = null;
        String tagName = null;
        for (String line : text.split("\\n")) {
            line = line.trim();
            if (line.startsWith("@")) {
                if (tagText != null) {
                    result.computeIfAbsent(tagName, k -> new ArrayList<>()).add(tagText.toString().trim().replaceAll("\\s+", " "));
                }

                tagName = line.substring(1).split("\\s+")[0];
                tagText = new StringBuilder(line.length() > tagName.length() + 2 ? line.substring(tagName.length() + 2) : "").append(" ");
                continue;
            }
            if (tagText != null) {
                tagText.append(line).append(" ");
            }
        }
        if (tagName != null)
            result.computeIfAbsent(tagName, k -> new ArrayList<>()).add(tagText.toString().trim().replaceAll("\\s+", " "));
        return result;
    }

}
