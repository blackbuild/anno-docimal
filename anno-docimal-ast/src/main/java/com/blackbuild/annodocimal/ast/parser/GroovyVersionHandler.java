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
import org.codehaus.groovy.ast.CompileUnit;

/**
 * Convenience class to handle differences between Groovy 2 and Groovy 3 AST. Can eventually be removed when Groovy 2 support is dropped.
 */
public class GroovyVersionHandler {

    public static String getGroovyVersion() {
        return GroovySystem.getVersion();
    }

    public static boolean isLegacyGroovy() {
        return getGroovyVersion().startsWith("2.");
    }

    public static <T> T getCompileUnitMetadata(CompileUnit compileUnit, Object key, Class<T> expectedType) {
        Object result = getCompileUnitMetadata(compileUnit, key);
        if (result == null) return null;
        if (!expectedType.isInstance(result)) {
            throw new IllegalStateException("Unexpected metadata type: " + result.getClass().getName());
        }
        return expectedType.cast(result);
    }

    public static Object getCompileUnitMetadata(CompileUnit compileUnit, Object key) {
        if (isLegacyGroovy()) {
            // since CompileUnit has no Metadata Groovy 2, we improvise
            // Modules should be complete by this stage, so we use the metadata of the
            // first module in the list
            return compileUnit.getModules().get(0).getNodeMetaData(key);
        } else {
            return compileUnit.getNodeMetaData(key);
        }
    }

    public static void setCompileUnitMetadata(CompileUnit compileUnit, Object key, Object value) {
        if (isLegacyGroovy()) {
            compileUnit.getModules().get(0).setNodeMetaData(key, value);
        } else {
            compileUnit.setNodeMetaData(key, value);
        }
    }


}
