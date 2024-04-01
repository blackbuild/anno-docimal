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

public class JavadocDocumentation implements Documentation {

    private final StringBuilder output = new StringBuilder();

    private enum State {
        TITLE, PARAGRAPH, PARAM, RETURN_TYPE, THROWS, TAGS;

        public State next() {
            return values()[ordinal() + 1];
        }

        public void assertNotAfter(State other) {
            if (ordinal() > other.ordinal())
                throw new IllegalStateException("Cannot add " + other + " after " + this);
        }
    }

    private State state = State.TITLE;

    @Override
    public Documentation title(String title) {
        state.assertNotAfter(State.TITLE);
        output.append(title);
        output.append("\n\n");
        state = state.next();
        return this;
    }

    @Override
    public Documentation p(String paragraph) {
        state.assertNotAfter(State.PARAGRAPH);
        output.append("<p>\n");
        output.append(paragraph);
        output.append("\n</p>\n\n");
        state = state.next();
        return this;
    }

    public Documentation code(String code) {
        state.assertNotAfter(State.PARAGRAPH);
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Documentation param(String name, String description) {
        state.assertNotAfter(State.PARAM);
        output.append("@param ");
        output.append(name);
        output.append(" ");
        output.append(description);
        output.append("\n");
        state = state.next();
        return this;
    }

    @Override
    public Documentation returnType(String returnType) {
        state.assertNotAfter(State.RETURN_TYPE);
        output.append("@return ");
        output.append(returnType);
        output.append("\n");
        state = state.next();
        return this;
    }

    @Override
    public Documentation throwsException(String exception, String description) {
        state.assertNotAfter(State.THROWS);
        output.append("@throws ");
        output.append(exception);
        output.append(" ");
        output.append(description);
        output.append("\n");
        state = state.next();
        return this;
    }

    @Override
    public Documentation seeAlso(String... links) {
        state.assertNotAfter(State.TAGS);
        output.append("@see ");
        output.append(String.join(" ", links));
        output.append("\n");
        state = state.next();
        return this;
    }

    @Override
    public Documentation since(String version) {
        state.assertNotAfter(State.TAGS);
        output.append("@since ");
        output.append(version);
        output.append("\n");
        state = state.next();
        return this;
    }

    @Override
    public Documentation deprecated(String reason) {
        state.assertNotAfter(State.TAGS);
        output.append("@deprecated ");
        output.append(reason);
        output.append("\n");
        state = state.next();
        return this;
    }

    @Override
    public Documentation author(String author) {
        state.assertNotAfter(State.TAGS);
        output.append("@author ");
        output.append(author);
        output.append("\n");
        state = state.next();
        return this;
    }

    @Override
    public String toJavadoc() {
        return output.toString();
    }
}
