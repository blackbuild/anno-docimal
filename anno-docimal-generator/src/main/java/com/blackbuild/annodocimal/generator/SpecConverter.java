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
import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.Type;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;

/**
 * Converts real reflection elements to their JavaPoet Spec counterparts.
 */
public class SpecConverter {

    private static final String INTERNAL_ANNOTATION = "groovy.transform.Internal";
    private static final String ANNO_DOC_ANNOTATION = Utility.getSignature(AnnoDoc.class.getName());

    private SpecConverter() {
        // static only
    }

    public static JavaFile toJavaFile(File source) throws IOException {
        JavaClass javaClass = new ClassParser(source.getAbsolutePath()).parse();
        return JavaFile.builder(javaClass.getPackageName(), toTypeSpec(javaClass, false)).build();
    }

    public static TypeSpec toTypeSpec(JavaClass type, boolean staticInnerClass) {
        ClassName typeName = ClassName.bestGuess(type.getClassName().replace('$', '.'));
        TypeSpec.Builder builder = TypeSpec.classBuilder(typeName)
                .superclass(ClassName.bestGuess(type.getSuperclassName()))
                .addModifiers(decodeModifiers(type));
        if (staticInnerClass) builder.addModifiers(Modifier.STATIC);

        stream(type.getInterfaceNames())
                .map(ClassName::bestGuess)
                .forEach(builder::addSuperinterface);

        for (AnnotationEntry annotation : type.getAnnotationEntries())
            if (isJavadocs(annotation))
                builder.addJavadoc(getStringValue(annotation));
            else
                builder.addAnnotation(toAnnotationSpec(annotation));

        stream(type.getFields())
                .filter(SpecConverter::shouldBeIncluded)
                .map(SpecConverter::toFieldSpec)
                .forEach(builder::addField);

        stream(type.getMethods())
                .filter(SpecConverter::shouldBeIncluded)
                .map(SpecConverter::toMethodSpec)
                .forEach(builder::addMethod);


        stream(type.getAttributes())
                .filter(InnerClasses.class::isInstance)
                .map(InnerClasses.class::cast)
                .findFirst()
                .ifPresent(innerClassesAttribute ->
                        stream(innerClassesAttribute.getInnerClasses())
                        .filter(innerClass -> isInnerClassOf(innerClass, type))
                        .map(innerClass -> toInnerTypeSpec(innerClass, type))
                        .forEach(builder::addType)
                );

        return builder.build();
    }

    public static TypeSpec toInnerTypeSpec(InnerClass innerClass, JavaClass type) {
        String innerClassName = type.getConstantPool().getConstantString(innerClass.getInnerClassIndex(), org.apache.bcel.Const.CONSTANT_Class);
        try {
            JavaClass innerType = Repository.lookupClass(innerClassName);
            return toTypeSpec(innerType, (innerClass.getInnerAccessFlags() & Const.ACC_STATIC) != 0);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isInnerClassOf(InnerClass innerClass, JavaClass type) {
        return type.getClassNameIndex() == innerClass.getOuterClassIndex();
    }

    private static boolean isJavadocs(AnnotationEntry annotation) {
        return annotation.getAnnotationType().equals(ANNO_DOC_ANNOTATION);
    }

    private static boolean shouldBeIncluded(FieldOrMethod member) {
        return !member.isSynthetic()
                && member.isPublic()
                && (member.getModifiers() & 1) != 0
                && !member.getName().contains("$")
                && !isInternal(member);
    }

    // Need to use type name since the annotations are not present in Groovy 2.4
    // FIXME #2
    private static boolean isInternal(FieldOrMethod member) {
        return stream(member.getAnnotationEntries())
                .anyMatch(annotation -> annotation.getAnnotationType().equals(SpecConverter.INTERNAL_ANNOTATION));
    }

    public static FieldSpec toFieldSpec(org.apache.bcel.classfile.Field field) {
        ClassName className = ClassName.bestGuess(field.getType().toString());
        FieldSpec.Builder builder = FieldSpec.builder(className, field.getName(), decodeModifiers(field));

        for (AnnotationEntry annotation : field.getAnnotationEntries())
            if (isJavadocs(annotation))
                builder.addJavadoc(getStringValue(annotation));
            else
                builder.addAnnotation(toAnnotationSpec(annotation));

        return builder.build();
    }

    public static MethodSpec toMethodSpec(org.apache.bcel.classfile.Method constructorOrMethod) {
        boolean isConstructor = constructorOrMethod.getName().equals("<init>");
        MethodSpec.Builder builder = isConstructor ? MethodSpec.constructorBuilder() : MethodSpec.methodBuilder(constructorOrMethod.getName());

        builder.addModifiers(decodeModifiers(constructorOrMethod));

        if (!isConstructor && !constructorOrMethod.getReturnType().equals(Type.VOID))
            builder.returns(ClassName.bestGuess(constructorOrMethod.getReturnType().getClassName()));

        for (AnnotationEntry annotation : constructorOrMethod.getAnnotationEntries())
            if (isJavadocs(annotation))
                builder.addJavadoc(getStringValue(annotation));
            else
                builder.addAnnotation(toAnnotationSpec(annotation));

        List<String> argumentNames = getArgumentNames(constructorOrMethod);

        IntStream.range(0, argumentNames.size())
                .forEach(i -> builder.addParameter(toParameterSpec(constructorOrMethod, i, argumentNames.get(i))));

        return builder.build();
    }

    private static String getStringValue(AnnotationEntry annotation) {
        return stream(annotation.getElementValuePairs())
                .filter(pair -> pair.getNameString().equals("value"))
                .map(pair -> pair.getValue().stringifyValue())
                .findFirst()
                .orElse(null);
    }

    static List<String> getArgumentNames(Method method) {
        LocalVariableTable lvt = method.getLocalVariableTable();
        LocalVariable[] localVariables = lvt != null ? lvt.getLocalVariableTable() : null;

        if (localVariables == null) {
            if (method.getArgumentTypes().length == 0)
                return Collections.emptyList();
            List<String> result = new ArrayList<>();
            for (int i = 0; i < method.getArgumentTypes().length; i++) result.add("arg" + i);
            return result;
        }

        return stream(localVariables).skip(1).map(LocalVariable::getName).collect(Collectors.toList());
    }

    public static ParameterSpec toParameterSpec(Method method, int index, String name) {
        ParameterSpec.Builder builder = ParameterSpec.builder(ClassName.bestGuess(method.getArgumentTypes()[index].getClassName()), name);

        ParameterAnnotationEntry[] annotationEntries = method.getParameterAnnotationEntries();
        if (annotationEntries.length > 0)
            for (AnnotationEntry annotation : annotationEntries[index].getAnnotationEntries())
                if (isJavadocs(annotation))
                    builder.addJavadoc(getStringValue(annotation));
                else
                    builder.addAnnotation(toAnnotationSpec(annotation));

        return builder.build();
    }

    public static AnnotationSpec toAnnotationSpec(AnnotationEntry annotation) {
        AnnotationSpec.Builder builder = AnnotationSpec.builder(
                ClassName.bestGuess(Utility.typeSignatureToString(annotation.getAnnotationType(), true))
        );
        stream(annotation.getElementValuePairs())
                .forEach(pair -> toAnnotationMember(pair, builder));
        return builder.build();
    }

    private static AnnotationSpec.@NotNull Builder toAnnotationMember(ElementValuePair pair, AnnotationSpec.Builder builder) {
        return builder.addMember(pair.getNameString(), stringifyValue(pair.getValue()));
    }

    private static CodeBlock stringifyValue(ElementValue value) {
        if (value instanceof SimpleElementValue)
            return stringifySimpleElementValue(value);
        else if (value instanceof ArrayElementValue)
            return stringifyArrayElementValue((ArrayElementValue) value);
        else if (value instanceof EnumElementValue)
            return stringifyEnumElementValue((EnumElementValue) value);
        else if (value instanceof AnnotationElementValue)
            return stringifyAnnotationElementValue((AnnotationElementValue) value);
        else if (value instanceof ClassElementValue) return stringifyClassElementValue((ClassElementValue) value);
        throw new UnsupportedOperationException("Unsupported element value type: " + value);
    }

    private static @NotNull CodeBlock stringifyClassElementValue(ClassElementValue value) {
        return CodeBlock.of("$T.class", ClassName.bestGuess(Utility.typeSignatureToString(value.getClassString(), true)));
    }

    private static @NotNull CodeBlock stringifyAnnotationElementValue(AnnotationElementValue value) {
        return CodeBlock.of("$L", toAnnotationSpec(value.getAnnotationEntry()));
    }

    private static @NotNull CodeBlock stringifyEnumElementValue(EnumElementValue enumValue) {
        return CodeBlock.of("$T.$L", ClassName.bestGuess(Utility.typeSignatureToString(enumValue.getEnumTypeString(), true)), enumValue.getEnumValueString());
    }

    private static @NotNull CodeBlock stringifyArrayElementValue(ArrayElementValue value) {
        return CodeBlock.of("{$L}", stream(value.getElementValuesArray())
                .map(SpecConverter::stringifyValue)
                .collect(CodeBlock.joining(", ")));
    }

    private static @NotNull CodeBlock stringifySimpleElementValue(ElementValue value) {
        switch (value.getElementValueType()) {
            case ElementValue.STRING:
                return CodeBlock.of("$S", value.stringifyValue());
            case ElementValue.PRIMITIVE_CHAR:
                return CodeBlock.of("'$L'", value.stringifyValue());
            case ElementValue.PRIMITIVE_FLOAT:
                return CodeBlock.of("$Lf", value.stringifyValue());
            case ElementValue.PRIMITIVE_LONG:
                return CodeBlock.of("$LL", value.stringifyValue());
            default:
                return CodeBlock.of("$L", value.stringifyValue());
        }
    }

    static Modifier[] decodeModifiers(AccessFlags flags) {
        EnumSet<Modifier> result = EnumSet.noneOf(Modifier.class);

        if (flags.isPublic()) result.add(Modifier.PUBLIC);
        if (flags.isPrivate()) result.add(Modifier.PRIVATE);
        if (flags.isProtected()) result.add(Modifier.PROTECTED);
        if (flags.isStatic()) result.add(Modifier.STATIC);
        if (flags.isFinal()) result.add(Modifier.FINAL);

        if (flags instanceof Field) {
            if (flags.isVolatile()) result.add(Modifier.VOLATILE);
            if (flags.isTransient()) result.add(Modifier.TRANSIENT);
        } else if (flags instanceof Method) {
            if (flags.isNative()) result.add(Modifier.NATIVE);
            if (flags.isAbstract()) result.add(Modifier.ABSTRACT);
            if (flags.isSynchronized()) result.add(Modifier.SYNCHRONIZED);
        }

        return result.toArray(new Modifier[0]);
    }
}
