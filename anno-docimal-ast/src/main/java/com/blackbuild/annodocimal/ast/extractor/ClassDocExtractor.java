package com.blackbuild.annodocimal.ast.extractor;

import com.blackbuild.annodocimal.annotations.AnnoDoc;
import com.blackbuild.annodocimal.annotations.InlineJavadocs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
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

    public static String extractDocumentation(AnnotatedElement element) {
        return extractDocumentation(element, null);
    }

    public static String extractDocumentation(AnnotatedElement element, String defaultValue) {
        AnnoDoc annotation = element.getAnnotation(AnnoDoc.class);
        if (annotation != null) return annotation.value();

        String result;
        if (element instanceof Class) {
            result = extractDocumentationFromClass((Class<?>) element);
        } else if (element instanceof Executable) {
            result = extractDocumentationFromExecutable((Executable) element);
        } else if (element instanceof Field) {
            result = extractDocumentationFromField((Field) element);
        } else {
            return defaultValue;
        }

        return result == null ? defaultValue : result;

    }

    private static String extractDocumentationFromExecutable(Executable method) {
        Map<String, String> classDoc = getClassDoc(method.getDeclaringClass());
        if (classDoc == null) return null;
        String argType = Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .collect(Collectors.joining(","));
        return classDoc.get("method." + method.getName() + "(" + argType + ")");
    }

    private static String extractDocumentationFromField(Field field) {
        Map<String, String> classDoc = getClassDoc(field.getDeclaringClass());
        if (classDoc == null) return null;
        return classDoc.get("field." + field.getName());
    }

    private static String extractDocumentationFromClass(Class<?> element) {
        Map<String, String> classDoc = getClassDoc(element);
        if (classDoc == null) return null;
        return classDoc.get("classDoc");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, String> getClassDoc(Class<?> element) {
        Properties result;
        try (InputStream stream = element.getResourceAsStream(element.getSimpleName() + InlineJavadocs.JAVADOC_PROPERTIES_SUFFIX)) {
            if (stream == null) return null;
            result = new Properties();
            result.load(stream);
            return (Map) result;
        } catch (IOException e) {
            return null;
        }

    }
}
