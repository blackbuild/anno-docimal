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

import java.util.Map;

public class JavadocDocumentation extends AbstractDocumentation {

    public JavadocDocumentation(String title) {
        this.title = title;
    }

    @Override
    public String toJavadoc() {
        StringBuilder builder = new StringBuilder();
        if (title != null) builder.append(title).append("\n");
        if (paragraphs != null) {
            for (String paragraph : paragraphs) {
                builder.append("<p>\n").append(paragraph).append("\n</p>\n");
            }
        }
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.append("@param ").append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
            }
        }
        if (returnType != null) {
            builder.append("@return ").append(returnType).append("\n");
        }
        if (exceptions != null) {
            for (Map.Entry<String, String> entry : exceptions.entrySet()) {
                builder.append("@throws ").append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
            }
        }
        if (tags != null) {
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                builder.append("@").append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
            }
        }
        return builder.toString();
    }
}
