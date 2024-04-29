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
    public static final List<String> SPECIAL_TAGS = List.of(PARAM_TAG, RETURN_TAG, THROWS_TAG);
    protected String title;
    protected List<String> paragraphs;
    protected Map<String, String> params;
    protected String returnType;
    protected Map<String, String> exceptions;
    protected Map<String, List<String>> otherTags;

    @Override
    public DocBuilder fromDocText(DocText docText) {
        if (docText == null || docText.isEmpty()) return this;
        title(docText.title);

        paragraphs = new ArrayList<>();
        paragraphs.addAll(splitIntoParagraphs(docText.body));

        if (!docText.tags.getOrDefault(RETURN_TAG, Collections.emptyList()).isEmpty())
            returnType(docText.tags.get(RETURN_TAG).get(0));
        docText.tags.getOrDefault(PARAM_TAG, Collections.emptyList()).forEach(param -> {
            String[] parts = param.split(" ", 2);
            param(parts[0], parts[1]);
        });
        docText.tags.getOrDefault(THROWS_TAG, Collections.emptyList()).forEach(throwsException -> {
            String[] parts = throwsException.split(" ", 2);
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
        if (otherTags == null) otherTags = new LinkedHashMap<>();
        if (isBlank(tag)) return this;
        otherTags.computeIfAbsent(tag, k -> new ArrayList<>()).add(description);
        return this;
    }

    @Override
    public DocBuilder seeAlso(String... links) {
        return tag("see", String.join(" ", links));
    }

    @Override
    public DocBuilder since(String version) {
        return tag("since", version);
    }

    @Override
    public DocBuilder deprecated(String reason) {
        return tag("deprecated", reason);
    }

    @Override
    public DocBuilder author(String author) {
        return tag("author", author);
    }

    @Override
    public boolean isEmpty() {
        return title == null && paragraphs == null && params == null && returnType == null && exceptions == null && otherTags == null;
    }
}
