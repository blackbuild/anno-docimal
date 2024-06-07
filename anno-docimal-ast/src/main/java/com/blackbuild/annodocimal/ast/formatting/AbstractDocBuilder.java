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

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractDocBuilder implements DocBuilder {

    public static final String PARAM_TAG = "param";
    public static final String RETURN_TAG = "return";
    public static final String THROWS_TAG = "throws";
    public static final String SINCE = "since";
    public static final String DEPRECATED = "deprecated";
    public static final List<String> SPECIAL_TAGS = List.of(PARAM_TAG, RETURN_TAG, THROWS_TAG);
    public static final List<String> SINGLE_TAGS = List.of(SINCE, DEPRECATED);
    protected String title;
    protected List<String> paragraphs;
    protected List<String> additionalParagraphs;
    protected Map<String, String> params;
    protected String returnType;
    protected Map<String, String> exceptions;
    protected Map<String, List<String>> otherTags;
    protected Map<String, String> templateValues;

    @Override
    public DocBuilder fromDocText(DocText docText) {
        if (docText == null) return this;
        if (docText.title != null && title == null)
            title(docText.title);

        if (paragraphs == null && docText.body != null) {
            paragraphs = new ArrayList<>();
            paragraphs.addAll(splitIntoParagraphs(docText.body));
        }

        if (returnType == null && !docText.tags.getOrDefault(RETURN_TAG, Collections.emptyList()).isEmpty())
            returnType(docText.tags.get(RETURN_TAG).get(0));

        docText.tags.getOrDefault(PARAM_TAG, Collections.emptyList()).forEach(param -> {
            String[] parts = param.split(" ", 2);
            if (params == null || !params.containsKey(parts[0]))
                param(parts[0], parts[1]);
        });
        docText.tags.getOrDefault(THROWS_TAG, Collections.emptyList()).forEach(throwsException -> {
            String[] parts = throwsException.split(" ", 2);
            if (exceptions == null || !exceptions.containsKey(parts[0]))
                throwsException(parts[0], parts[1]);
        });
        docText.tags.forEach((tag, values) -> {
            if (!SPECIAL_TAGS.contains(tag)) {
                values.forEach(value -> tag(tag, value));
            }
        });

        return this;
    }

    List<String> splitIntoParagraphs(String body) {
        if (body == null) return Collections.emptyList();
        if (body.isBlank()) return Collections.emptyList();
        return Arrays.stream(body.split("<[pP]>"))
                .map(s -> s.replaceAll("</[pP]>", ""))
                .map(String::strip)
                .filter(AbstractDocBuilder::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public DocBuilder fromRawText(String rawText) {
        return fromDocText(DocText.fromRawText(rawText));
    }

    @Override
    public DocBuilder title(String title) {
        this.title = title;
        return this;
    }

    @Override
    public DocBuilder p(String paragraph) {
        if (paragraphs == null) paragraphs = new ArrayList<>();
        if (isNotBlank(paragraph)) paragraphs.add(paragraph);
        return this;
    }

    @Override
    public DocBuilder extraP(String paragraph) {
        if (additionalParagraphs == null) additionalParagraphs = new ArrayList<>();
        if (isNotBlank(paragraph)) additionalParagraphs.add(paragraph);
        return this;
    }

    protected static boolean isNotBlank(String string) {
        return !isBlank(string);
    }

    protected static boolean isBlank(String string) {
        return string == null || string.isBlank();
    }

    protected static boolean areNotBlank(String left, String right) {
        return isNotBlank(left) && isNotBlank(right);
    }

    public DocBuilder code(String code) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public DocBuilder param(String name, String description) {
        if (params == null) params = new LinkedHashMap<>();
        if (areNotBlank(name, description)) params.put(name, description);
        return this;
    }

    @Override
    public DocBuilder returnType(String returnType) {
        if (isNotBlank(returnType) )this.returnType = returnType;
        return this;
    }

    @Override
    public DocBuilder throwsException(String exception, String description) {
        if (exceptions == null) exceptions = new LinkedHashMap<>();
        if (areNotBlank(exception, description)) exceptions.put(exception, description);
        return this;
    }

    @Override
    public DocBuilder tag(String tag, String description) {
        if (description == null) description = "";
        if (isBlank(tag)) return this;
        if (otherTags == null) otherTags = new LinkedHashMap<>();
        if (SINGLE_TAGS.contains(tag)) {
            otherTags.put(tag, List.of(description));
        } else {
            otherTags.computeIfAbsent(tag, k -> new ArrayList<>()).add(description);
        }
        if (tag.equals("template")) {
            String[] split = description.split("\\s+", 2);
            template(split[0], split[1]);
        }
        return this;
    }

    @Override
    public DocBuilder seeAlso(String... links) {
        return tag("see", String.join(" ", links));
    }

    @Override
    public DocBuilder since(String version) {
        return tag(SINCE, version);
    }

    @Override
    public DocBuilder deprecated(String reason) {
        return tag(DEPRECATED, reason);
    }

    @Override
    public DocBuilder author(String author) {
        return tag("author", author);
    }

    @Override
    public boolean isEmpty() {
        return title == null && paragraphs == null && params == null && returnType == null && exceptions == null && otherTags == null;
    }

    @Override
    public DocBuilder template(String key, String value) {
        if (templateValues == null) templateValues = new LinkedHashMap<>();
        if (value == null) templateValues.remove(key);
        else templateValues.put(key, value);
        return this;
    }

    @Override
    public DocBuilder templates(Map<String, String> values) {
        if (this.templateValues == null) this.templateValues = new LinkedHashMap<>();
        this.templateValues.putAll(values);
        return this;
    }
}
