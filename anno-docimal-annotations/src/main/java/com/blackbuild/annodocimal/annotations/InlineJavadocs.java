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
package com.blackbuild.annodocimal.annotations;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;
import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a package or type for local documentation capture during Groovy compilation.
 *
 * <p>The same marker also selects Java annotation-processor capture when the APT artifact is on the processor path.
 * Groovy global capture is enabled by the global-AST artifact instead. The public suffix constant is protocol
 * implementation detail; use the documented documentation-properties behavior rather than that Java constant.</p>
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass("com.blackbuild.annodocimal.ast.InlineJavadocsTransformation")
@NullMarked
public @interface InlineJavadocs {
    /**
     * Suffix of the generated documentation-properties resource.
     *
     * <p>This protocol constant is not supported Java API; consumers must not couple source code to it.</p>
     */
    String JAVADOC_PROPERTIES_SUFFIX = "__annodoc.properties";
}
