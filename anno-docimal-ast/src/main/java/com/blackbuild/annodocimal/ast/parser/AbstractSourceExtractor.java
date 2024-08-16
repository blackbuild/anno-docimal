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
package com.blackbuild.annodocimal.ast.parser;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class AbstractSourceExtractor implements SourceExtractor {
    protected String reformat(String rawComment) {
        if (rawComment == null) return null;

        if (rawComment.startsWith("/**"))
            rawComment = rawComment.substring(2);
        if (rawComment.endsWith("*/"))
            rawComment = rawComment.substring(0, rawComment.length() - 2);
        rawComment = rawComment.replaceAll("(?m)^\\s*\\*", "");

        if (rawComment.isBlank()) return null;

        String[] lines = rawComment.split("\n");
        int minIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            if (line.trim().isEmpty())
                continue;

            int indent = 0;
            while (indent < line.length() && Character.isWhitespace(line.charAt(indent)))
                indent++;

            minIndent = Math.min(minIndent, indent);
        }

        int finalMinIndent = minIndent;
        String joined = Arrays.stream(lines)
                .map(line -> line.length() >= finalMinIndent ? line.substring(finalMinIndent) : "")
                .collect(Collectors.joining("\n"));

        int start = 0;
        while (start < joined.length() && joined.charAt(start) == '\n')
            start++;

        int end = joined.length();
        while (end > 0 && Character.isWhitespace(joined.charAt(end - 1)))
            end--;

        return joined.substring(start, end);
    }
}
