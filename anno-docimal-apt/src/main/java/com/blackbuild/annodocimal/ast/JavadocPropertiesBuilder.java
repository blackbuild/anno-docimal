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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Builds a properties file with the javadocs of a class and its members.
 *
 * Loosely based on <a href="https://github.com/dnault/therapi-runtime-javadoc/blob/main/therapi-runtime-javadoc-scribe/src/main/java/com/github/therapi/runtimejavadoc/scribe/JsonJavadocBuilder.java">JsonJavadocBuilder</a>
 */
public class JavadocPropertiesBuilder {
    private final ProcessingEnvironment processingEnv;

    public JavadocPropertiesBuilder(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public Properties getClassJavadoc(TypeElement classElement) {
        Properties result = new Properties();

        String classDoc = trimEachLine(processingEnv.getElementUtils().getDocComment(classElement));

        if (!isBlank(classDoc)) {
            result.setProperty("classDoc", classDoc);
        }

        for (Element element : classElement.getEnclosedElements()) {
            String elementDoc = trimEachLine(processingEnv.getElementUtils().getDocComment(element));

            if (isBlank(elementDoc)) continue;

            String key = getElementKey(element);

            if (key != null) {
                result.setProperty(key, elementDoc);
            }
        }

        return result;
    }

    private String trimEachLine(String text) {
        if (text == null) return null;
        return text.lines()
                .map(line -> line.startsWith(" ") ? line.substring(1) : line)
                .collect(Collectors.joining("\n"));
    }

    private String getElementKey(Element element) {
        switch (element.getKind()) {
            case METHOD:
            case CONSTRUCTOR:
                return "method." + element.getSimpleName() + "(" + getParameterKey((ExecutableElement) element) + ")";
            case FIELD:
                return "field." + element.getSimpleName();
            default:
                return null;
        }
    }

    private String getParameterKey(ExecutableElement element) {
        return element.getParameters().stream()
                .map(JavadocPropertiesBuilder::variableToString)
                .collect(Collectors.joining(","));
    }

    private static String variableToString(VariableElement variableElement) {
        TypeMirror type = variableElement.asType();
        if (type instanceof DeclaredType) {
            return ((DeclaredType) type).asElement().toString();
        }
        return type.toString();
    }

    private static boolean isBlank(String classDoc) {
        return classDoc == null || classDoc.isBlank();
    }
}
