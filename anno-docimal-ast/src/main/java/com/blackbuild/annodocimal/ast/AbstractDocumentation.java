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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractDocumentation implements Documentation {

    protected String title;
    protected List<String> paragraphs;
    protected Map<String, String> params;
    protected String returnType;
    protected Map<String, String> exceptions;
    protected Map<String, String> tags;

    @Override
    public Documentation title(String title) {
        this.title = title;
        return this;
    }

    @Override
    public Documentation p(String paragraph) {
        if (paragraphs == null) paragraphs = new ArrayList<>();
        if (isNotBlank(paragraph)) paragraphs.add(paragraph);
        return this;
    }

    protected static boolean isNotBlank(String string) {
        return string != null && !string.isBlank();
    }

    protected static boolean areNotBlank(String left, String right) {
        return isNotBlank(left) && isNotBlank(right);
    }

    public Documentation code(String code) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Documentation param(String name, String description) {
        if (params == null) params = new LinkedHashMap<>();
        if (areNotBlank(name, description)) params.put(name, description);
        return this;
    }

    @Override
    public Documentation returnType(String returnType) {
        if (isNotBlank(returnType) )this.returnType = returnType;
        return this;
    }

    @Override
    public Documentation throwsException(String exception, String description) {
        if (exceptions == null) exceptions = new LinkedHashMap<>();
        if (areNotBlank(exception, description)) exceptions.put(exception, description);
        return this;
    }

    protected Documentation tag(String tag, String description) {
        if (tags == null) tags = new LinkedHashMap<>();
        if (areNotBlank(tag, description)) tags.put(tag, description);
        return this;
    }

    @Override
    public Documentation seeAlso(String... links) {
        return tag("see", String.join(" ", links));
    }

    @Override
    public Documentation since(String version) {
        return tag("since", version);
    }

    @Override
    public Documentation deprecated(String reason) {
        return tag("deprecated", reason);
    }

    @Override
    public Documentation author(String author) {
        return tag("author", author);
    }
}
