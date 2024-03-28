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
package com.blackbuild.annodocimal.generator;

import com.blackbuild.annodocimal.annotations.AnnoDoc;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.EnumSet;

import static java.util.Arrays.stream;

/**
 * Converts real reflection elements to their JavaPoet Spec counterparts.
 */
public class SpecConverter {

    private static final String INTERNAL_ANNOTATION = "groovy.transform.Internal";

    private SpecConverter() {
        // static only
    }

    public static JavaFile toJavaFile(Class<?> type) {
        return JavaFile.builder(type.getPackage().getName(), toTypeSpec(type)).build();
    }

    public static TypeSpec toTypeSpec(Class<?> type) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(type.getSimpleName())
                .superclass(type.getSuperclass())
                .addModifiers(decodeModifiers(type.getModifiers()));

        stream(type.getInterfaces())
                .forEach(builder::addSuperinterface);

        for (Annotation annotation : type.getAnnotations())
            if (isJavadocs(annotation))
                builder.addJavadoc(((AnnoDoc) annotation).value());
            else
                builder.addAnnotation(toAnnotationSpec(annotation));

        stream(type.getDeclaredFields())
                .filter(SpecConverter::shouldBeIncluded)
                .map(SpecConverter::toFieldSpec)
                .forEach(builder::addField);

        stream(type.getDeclaredConstructors())
                .filter(SpecConverter::shouldBeIncluded)
                .map(SpecConverter::toMethodSpec)
                .forEach(builder::addMethod);

        stream(type.getDeclaredMethods())
                .filter(SpecConverter::shouldBeIncluded)
                .map(SpecConverter::toMethodSpec)
                .forEach(builder::addMethod);

        stream(type.getDeclaredClasses())
                .map(SpecConverter::toTypeSpec)
                .forEach(builder::addType);

        return builder.build();
    }

    private static boolean isJavadocs(Annotation annotation) {
        return annotation.annotationType().equals(AnnoDoc.class);
    }

    private static boolean shouldBeIncluded(Member member) {
        return !member.isSynthetic()
                && (member.getModifiers() & 1) != 0
                && !member.getName().contains("$")
                && !isInternal(member);
    }

    // Need to use type name since the annotations are not present in Groovy 2.4
    // FIXME #2
    private static boolean isInternal(Member member) {
        if (!(member instanceof AnnotatedElement)) return false;
        return stream(((AnnotatedElement) member).getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getName().equals(SpecConverter.INTERNAL_ANNOTATION));
    }

    public static FieldSpec toFieldSpec(Field field) {
        FieldSpec.Builder builder = FieldSpec.builder(field.getType(), field.getName(), decodeModifiers(field.getModifiers()));

        for (Annotation annotation : field.getAnnotations())
            if (isJavadocs(annotation))
                builder.addJavadoc(((AnnoDoc) annotation).value());
            else
                builder.addAnnotation(toAnnotationSpec(annotation));

        return builder.build();
    }

    public static MethodSpec toMethodSpec(Executable constructorOrMethod) {
        MethodSpec.Builder builder = constructorOrMethod instanceof Method ? MethodSpec.methodBuilder(constructorOrMethod.getName()) : MethodSpec.constructorBuilder();

        builder.addModifiers(decodeModifiers(constructorOrMethod.getModifiers()));

        if (constructorOrMethod instanceof Method)
            builder.returns(((Method) constructorOrMethod).getReturnType());

        for (Annotation annotation : constructorOrMethod.getAnnotations())
            if (isJavadocs(annotation))
                builder.addJavadoc(((AnnoDoc) annotation).value());
            else
                builder.addAnnotation(toAnnotationSpec(annotation));

        stream(constructorOrMethod.getParameters())
                .map(SpecConverter::toParameterSpec)
                .forEach(builder::addParameter);

        return builder.build();
    }

    public static ParameterSpec toParameterSpec(Parameter parameter) {
        ParameterSpec.Builder builder = ParameterSpec.builder(parameter.getType(), parameter.getName(), decodeModifiers(parameter.getModifiers()));
        if (parameter.isAnnotationPresent(AnnoDoc.class))
            builder.addJavadoc(parameter.getAnnotation(AnnoDoc.class).value());
        for (Annotation annotation : parameter.getAnnotations())
            builder.addAnnotation(toAnnotationSpec(annotation));
        return builder.build();
    }

    public static AnnotationSpec toAnnotationSpec(Annotation annotation) {
        return AnnotationSpec.get(annotation);
    }

    static Modifier[] decodeModifiers(int mod) {
        EnumSet<Modifier> result = EnumSet.noneOf(Modifier.class);

        if ((mod & 0x0001) != 0) result.add(Modifier.PUBLIC);
        if ((mod & 0x0002) != 0) result.add(Modifier.PRIVATE);
        if ((mod & 0x0004) != 0) result.add(Modifier.PROTECTED);
        if ((mod & 0x0008) != 0) result.add(Modifier.STATIC);

        return result.toArray(new Modifier[0]);
    }
}
