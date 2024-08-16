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

import groovy.lang.GroovySystem;
import org.codehaus.groovy.control.SourceUnit;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class SourceExtractorFactory {

    private static final SourceExtractorFactory INSTANCE = new SourceExtractorFactory();
    public static final String G3_EXTRACTOR_CLASS_NAME = "com.blackbuild.annodocimal.ast.parser.groovy3.Groovy3SourceExtractor";

    public static SourceExtractorFactory getInstance() {
        return INSTANCE;
    }

    public SourceExtractor createSourceExtractor(SourceUnit sourceUnit) {
        if (GroovySystem.getVersion().startsWith("2.")) {
            try {
                return GroovyDocToolSourceExtractor.create(sourceUnit);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // use reflection since we are compatible to Groovy2
        // FIXME #2
        Class<?> aClass = null;
        try {
            aClass = this.getClass().getClassLoader().loadClass(G3_EXTRACTOR_CLASS_NAME);
            return (SourceExtractor) aClass.getDeclaredMethod("getInstance").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
