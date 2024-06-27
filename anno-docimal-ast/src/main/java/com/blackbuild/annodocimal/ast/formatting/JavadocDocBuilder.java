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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Standard DocBuilder producing regular Javadoc.
 */
public class JavadocDocBuilder extends AbstractDocBuilder {

    @Override
    public JavadocDocBuilder getCopy() {
        JavadocDocBuilder copy = new JavadocDocBuilder();
        copy.title = title;
        if (paragraphs != null) copy.paragraphs = new java.util.ArrayList<>(paragraphs);
        if (params != null) copy.params = new java.util.LinkedHashMap<>(params);
        copy.returnType = returnType;
        if (exceptions != null) copy.exceptions = new java.util.LinkedHashMap<>(exceptions);
        if (otherTags != null) {
            copy.otherTags = new java.util.LinkedHashMap<>();
            otherTags.forEach((key, value) -> copy.otherTags.put(key, new java.util.ArrayList<>(value)));
        }
        return copy;
    }

    @Override
    public String toJavadoc() {
        return toJavadoc(null);
    }

    @Override
    public String toJavadoc(List<String> validParameters) {
        StringBuilder builder = new StringBuilder();
        addTitle(builder);
        addParagraphs(builder);
        addSignatureElements(validParameters, builder);
        addOtherTags(builder);
        return replaceTemplateValues(builder.toString());
    }


    private String replaceTemplateValues(String rawString) {
        return TemplateHandler.renderTemplates(rawString, templateValues, params != null ? params.keySet() : Collections.emptySet());
    }

    private void addOtherTags(StringBuilder builder) {
        if (otherTags == null) return;
        otherTags.forEach((tagName, tagList) ->
            tagList.forEach(value -> builder.append("@").append(tagName).append(" ").append(value).append("\n"))
        );
    }

    private void addSignatureElements(List<String> validParameters, StringBuilder builder) {
        addParams(validParameters, builder);
        addReturnType(builder);
        addExceptions(builder);
    }

    private void addExceptions(StringBuilder builder) {
        if (exceptions != null)
            for (Map.Entry<String, String> entry : exceptions.entrySet())
                builder.append("@throws ").append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
    }

    private void addReturnType(StringBuilder builder) {
        if (returnType != null)
            builder.append("@return ").append(returnType).append("\n");
    }

    private void addParams(List<String> validParameters, StringBuilder builder) {
        if (params != null)
            for (Map.Entry<String, String> entry : params.entrySet())
                if (validParameters == null || validParameters.contains(entry.getKey()))
                    builder.append("@param ").append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
    }

    private void addParagraphs(StringBuilder builder) {
        if (paragraphs != null)
            for (String paragraph : paragraphs)
                builder.append(surroundWithParagraph(paragraph)).append("\n");
        if (additionalParagraphs != null)
            for (String paragraph : additionalParagraphs)
                builder.append(surroundWithParagraph(paragraph)).append("\n");
    }

    private String surroundWithParagraph(String paragraph) {
        if (paragraph.startsWith("<"))
            return paragraph;
        else
            return "<p>\n" + paragraph + "\n</p>";
    }

    private void addTitle(StringBuilder builder) {
        if (title != null) builder.append(title).append("\n");
    }
}
