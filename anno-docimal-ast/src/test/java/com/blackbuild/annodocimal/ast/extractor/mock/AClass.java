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
package com.blackbuild.annodocimal.ast.extractor.mock;

import com.blackbuild.annodocimal.annotations.InlineJavadocs;

/**
 * A class for testing.
 */
@InlineJavadocs
public class AClass {

    /**
     * Creates a new instance of {@link AClass}.
     */
    public AClass() {}

    /**
     * A method that does nothing.
     */
    public void aMethod() {
        // do nothing
    }

    public void noJavaDocMethod() {
        // do nothing
    }

    /**
     * A method that does something.
     * @param what the thing to do
     * @return the result of doing it
     */
    public String doIt(String what) {
        return "I did it: " + what;
    }

    /**
     * A field.
     */
    public String field = "field";

    /**
     * An inner class.
     */
    public static class InnerClass {
        /**
         * Another method that does nothing.
         */
        public void innerMethod() {}
    }

}