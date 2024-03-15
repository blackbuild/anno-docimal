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

import com.blackbuild.annodocimal.annotations.Javadocs;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.EnumSet;

/**
 * Converts real reflection elements to their JavaPoet Spec counterparts.
 */
public class SpecConverter {

    private SpecConverter() {
        // static only
    }

    public static JavaFile toJavaFile(Class<?> type) {
        return JavaFile.builder(type.getPackage().getName(), toTypeSpec(type)).build();
    }

    public static TypeSpec toTypeSpec(Class<?> type) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(type.getSimpleName())
                .superclass(type.getSuperclass())
                .addModifiers(getModifiers(type.getModifiers()));

        for (Class<?> aClass : type.getInterfaces())
            builder.addSuperinterface(aClass);

        for (Annotation annotation : type.getAnnotations())
            if (annotation instanceof Javadocs)
                builder.addJavadoc(((Javadocs) annotation).value());
            else
                builder.addAnnotation(toAnnotationSpec(annotation));

        for (Field field : type.getDeclaredFields()) {
            if (shouldIgnore(field)) continue;
            builder.addField(toFieldSpec(field));
        }

        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (shouldIgnore(constructor)) continue;
            builder.addMethod(toMethodSpec(constructor));
        }

        for (Method method : type.getDeclaredMethods()) {
            if (shouldIgnore(method)) continue;
            builder.addMethod(toMethodSpec(method));
        }

        for (Class<?> declaredClass : type.getDeclaredClasses()) {
            builder.addType(toTypeSpec(declaredClass));
        }

        return builder.build();
    }

    private static boolean shouldIgnore(Member member) {
        return member.isSynthetic() || (member.getModifiers() & 1) == 0 || member.getName().contains("$");
    }

    public static FieldSpec toFieldSpec(Field field) {
        FieldSpec.Builder builder = FieldSpec.builder(field.getType(), field.getName(), getModifiers(field.getModifiers()));

        for (Annotation annotation : field.getAnnotations())
            if (annotation instanceof Javadocs)
                builder.addJavadoc(((Javadocs) annotation).value());
            else
                builder.addAnnotation(toAnnotationSpec(annotation));

        return builder.build();
    }

    public static MethodSpec toMethodSpec(Executable method) {
        MethodSpec.Builder builder = method instanceof Method ? MethodSpec.methodBuilder(method.getName()) : MethodSpec.constructorBuilder();

        builder.addModifiers(getModifiers(method.getModifiers()));

        if (method instanceof Method)
                builder.returns(((Method) method).getReturnType());

        for (Annotation annotation : method.getAnnotations())
            if (annotation instanceof Javadocs)
                builder.addJavadoc(((Javadocs) annotation).value());
            else
                builder.addAnnotation(toAnnotationSpec(annotation));

        for (Parameter param : method.getParameters())
            builder.addParameter(toParameterSpec(param));

        return builder.build();
    }

    public static ParameterSpec toParameterSpec(Parameter parameter) {
        ParameterSpec.Builder builder = ParameterSpec.builder(parameter.getType(), parameter.getName(), getModifiers(parameter.getModifiers()));
        if (parameter.isAnnotationPresent(Javadocs.class))
            builder.addJavadoc(parameter.getAnnotation(Javadocs.class).value());
        for (Annotation annotation : parameter.getAnnotations())
            builder.addAnnotation(toAnnotationSpec(annotation));
        return builder.build();
    }

    public static AnnotationSpec toAnnotationSpec(Annotation annotation) {
        return AnnotationSpec.get(annotation);
    }

    static Modifier[] getModifiers(int mod) {
        EnumSet<Modifier> result = EnumSet.noneOf(Modifier.class);

        if ((mod & 0x0001) != 0) result.add(Modifier.PUBLIC);
        if ((mod & 0x0002) != 0) result.add(Modifier.PRIVATE);
        if ((mod & 0x0004) != 0) result.add(Modifier.PROTECTED);
        if ((mod & 0x0008) != 0) result.add(Modifier.STATIC);

        return result.toArray(new Modifier[0]);
    }
}
