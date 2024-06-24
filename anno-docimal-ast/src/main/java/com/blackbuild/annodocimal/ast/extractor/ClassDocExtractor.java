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
package com.blackbuild.annodocimal.ast.extractor;

import com.blackbuild.annodocimal.annotations.AnnoDoc;
import com.blackbuild.annodocimal.annotations.InlineJavadocs;
import org.codehaus.groovy.ast.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Extracts the documentation from a class from either an existing {@link AnnoDoc}
 * annotation or existing AnnoDoc.properties.
 */
public class ClassDocExtractor {
    private ClassDocExtractor() {
        // Utility class
    }

    public static String extractDocumentation(AnnotatedNode element) {
        return extractDocumentation(element, null);
    }

    public static String extractDocumentation(AnnotatedNode element, String defaultValue) {
        String result;
        if (element instanceof ClassNode) {
            result = extractDocumentationFromClass((ClassNode) element);
        } else if (element instanceof MethodNode) {
            result = extractDocumentationFromExecutable((MethodNode) element);
        } else if (element instanceof FieldNode) {
            result = extractDocumentationFromField((FieldNode) element);
        } else {
            return defaultValue;
        }

        return result == null ? defaultValue : result;

    }

    private static String extractDocumentationFromExecutable(MethodNode method) {
        Map<String, String> classDoc = getClassDoc(method.getDeclaringClass());
        if (classDoc == null) return null;
        String argType = Arrays.stream(method.getParameters())
                .map(ClassDocExtractor::argTypeString)
                .collect(Collectors.joining(","));
        return classDoc.get("method." + method.getName() + "(" + argType + ")");
    }

    private static String argTypeString(Parameter param) {
        ClassNode type = param.getType();
        return type.isArray() ? type.toString() : type.getName();
    }

    private static String extractDocumentationFromField(FieldNode field) {
        Map<String, String> classDoc = getClassDoc(field.getOwner());
        if (classDoc == null) return null;
        return classDoc.get("field." + field.getName());
    }

    private static String extractDocumentationFromClass(ClassNode element) {
        Map<String, String> classDoc = getClassDoc(element);
        if (classDoc == null) return null;
        return classDoc.get("classDoc");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, String> getClassDoc(ClassNode element) {
        if (element == null) return null;
        if (!element.isResolved()) return null;
        Class<?> type = element.getTypeClass();
        Properties result;
        try (InputStream stream = type.getResourceAsStream(getPropertiesFileName(type))) {
            if (stream == null) return null;
            result = new Properties();
            result.load(stream);
            return (Map) result;
        } catch (IOException e) {
            return null;
        }
    }

    private static @NotNull String getPropertiesFileName(Class<?> element) {
        return getSimpleName(element) + InlineJavadocs.JAVADOC_PROPERTIES_SUFFIX;
    }

    private static String getSimpleName(Class<?> element) {
        if (element.getDeclaringClass() == null)
            return element.getSimpleName();
        return getSimpleName(element.getDeclaringClass()) + "$" + element.getSimpleName();
    }

}
