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
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
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
        return JavaFile.builder(javaClass.getPackageName(), toTypeSpec(javaClass, null)).build();
    }

    public static TypeSpec toTypeSpec(JavaClass type, AccessFlags flags) {
        ClassName typeName = toTypeName(type);
        TypeSpec.Builder builder = TypeSpec.classBuilder(typeName)
                .superclass(toTypeName(type.getSuperclassName()))
                .addModifiers(decodeModifiers(flags != null ? flags : type));

        stream(type.getInterfaceNames())
                .map(ClassName::bestGuess)
                .forEach(builder::addSuperinterface);

        for (AnnotationEntry annotation : type.getAnnotationEntries())
            if (isJavadocs(annotation))
                builder.addJavadoc(getStringValueOfJavadocAnnotation(annotation));
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

    private static ClassName toTypeName(JavaClass type) {
        String className = type.getClassName();
        if (!type.isNested())
            return ClassName.bestGuess(className);

        return toTypeName(className);
    }

    private static ClassName toTypeName(String className) {
        String[] elements = className.split("\\$");
        ClassName result = ClassName.bestGuess(elements[0]);
        for (int i = 1; i < elements.length; i++)
            result = result.nestedClass(elements[i]);
        return result;
    }

    public static TypeSpec toInnerTypeSpec(InnerClass innerClass, JavaClass type) {
        String innerClassName = type.getConstantPool().getConstantString(innerClass.getInnerClassIndex(), org.apache.bcel.Const.CONSTANT_Class);
        try {
            JavaClass innerType = Repository.lookupClass(innerClassName);
            return toTypeSpec(innerType, new InnerClassAccessFlags(innerClass));
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
                && (member.isPublic() || member.isProtected())
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
        TypeName className = toTypeName(field.getType());
        FieldSpec.Builder builder = FieldSpec.builder(className, field.getName(), decodeModifiers(field));

        for (AnnotationEntry annotation : field.getAnnotationEntries())
            if (isJavadocs(annotation))
                builder.addJavadoc(getStringValueOfJavadocAnnotation(annotation));
            else
                builder.addAnnotation(toAnnotationSpec(annotation));

        return builder.build();
    }

    public static MethodSpec toMethodSpec(org.apache.bcel.classfile.Method constructorOrMethod) {
        boolean isConstructor = constructorOrMethod.getName().equals("<init>");
        MethodSpec.Builder builder = isConstructor ? MethodSpec.constructorBuilder() : MethodSpec.methodBuilder(constructorOrMethod.getName());

        builder.addModifiers(decodeModifiers(constructorOrMethod));

        Type returnType = constructorOrMethod.getReturnType();
        if (!isConstructor && !returnType.equals(Type.VOID))
            builder.returns(toTypeName(returnType));

        List<String> argumentNames = getArgumentNames(constructorOrMethod);
        IntStream.range(0, argumentNames.size())
                .forEach(i -> builder.addParameter(toParameterSpec(constructorOrMethod, i, argumentNames.get(i))));

        for (AnnotationEntry annotation : constructorOrMethod.getAnnotationEntries())
            if (isJavadocs(annotation))
                builder.addJavadoc(filterParams(getStringValueOfJavadocAnnotation(annotation), argumentNames));
            else
                builder.addAnnotation(toAnnotationSpec(annotation));

        return builder.build();
    }

    private static final Pattern PARAM_PATTERN = Pattern.compile("(?m)^\\s*@param\\s+(\\w+)\\s*");

    private static String filterParams(String stringValue, List<String> argumentNames) {
        if (argumentNames.isEmpty() && !stringValue.contains("@param"))
            return stringValue;

        Set<String> names = PARAM_PATTERN.matcher(stringValue)
                .results()
                .map(match -> match.group(1))
                .collect(Collectors.toSet());

        argumentNames.forEach(names::remove);

        if (names.isEmpty())
            // all params point to actual arguments
            return stringValue;

        Pattern badParams = Pattern.compile("(?sm)^\\s*@param\\s+(" + String.join("|", names) + ").*?(?=(^@\\w+|$))");
        return badParams.matcher(stringValue).replaceAll("");
    }

    private static TypeName toTypeName(Type type) {
        if (type instanceof BasicType) {
            switch (type.getType()) {
                case Const.T_BOOLEAN:
                    return TypeName.BOOLEAN;
                case Const.T_BYTE:
                    return TypeName.BYTE;
                case Const.T_CHAR:
                    return TypeName.CHAR;
                case Const.T_DOUBLE:
                    return TypeName.DOUBLE;
                case Const.T_FLOAT:
                    return TypeName.FLOAT;
                case Const.T_INT:
                    return TypeName.INT;
                case Const.T_LONG:
                    return TypeName.LONG;
                case Const.T_SHORT:
                    return TypeName.SHORT;
                case Const.T_VOID:
                    return TypeName.VOID;
                default:
                    throw new IllegalArgumentException("Unknown basic type: " + type);
            }
        }
        if (type instanceof ArrayType)
            return ArrayTypeName.of(toTypeName(((ArrayType) type).getElementType()));
        return toTypeName(type.getClassName());
    }

    private static String getStringValueOfJavadocAnnotation(AnnotationEntry annotation) {
        return stream(annotation.getElementValuePairs())
                .filter(pair -> pair.getNameString().equals("value"))
                .map(pair -> pair.getValue().stringifyValue())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Encountered a javadoc annotation without a value: " + annotation));
    }

    static List<String> getArgumentNames(Method method) {
        LocalVariableTable lvt = method.getLocalVariableTable();
        LocalVariable[] localVariables = lvt != null ? lvt.getLocalVariableTable() : null;

        int argumentCount = method.getArgumentTypes().length;

        if (localVariables == null) {
            if (argumentCount == 0)
                return Collections.emptyList();
            List<String> result = new ArrayList<>();
            for (int i = 0; i < argumentCount; i++) result.add("arg" + i);
            return result;
        }

        int first = method.isStatic() ? 0 : 1;
        int last = first + argumentCount;

        List<String> result = new ArrayList<>(argumentCount);

        for (int i = first; i < last; i++) {
            result.add(localVariables[i].getName());
        }
        return result;
    }

    public static ParameterSpec toParameterSpec(Method method, int index, String name) {
        ParameterSpec.Builder builder = ParameterSpec.builder(toTypeName(method.getArgumentTypes()[index]), name);

        ParameterAnnotationEntry[] annotationEntries = method.getParameterAnnotationEntries();
        if (annotationEntries.length > 0)
            for (AnnotationEntry annotation : annotationEntries[index].getAnnotationEntries())
                if (isJavadocs(annotation))
                    builder.addJavadoc(getStringValueOfJavadocAnnotation(annotation));
                else
                    builder.addAnnotation(toAnnotationSpec(annotation));

        return builder.build();
    }

    public static AnnotationSpec toAnnotationSpec(AnnotationEntry annotation) {
        AnnotationSpec.Builder builder = AnnotationSpec.builder(
                toTypeName(Utility.typeSignatureToString(annotation.getAnnotationType(), true))
        );
        stream(annotation.getElementValuePairs())
                .forEach(pair -> addAnnotationMember(pair, builder));
        return builder.build();
    }

    private static void addAnnotationMember(ElementValuePair pair, AnnotationSpec.Builder builder) {
        builder.addMember(pair.getNameString(), stringifyValue(pair.getValue()));
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
        return CodeBlock.of("$T.class", toTypeName(Utility.typeSignatureToString(value.getClassString(), true)));
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

    @SuppressWarnings("java:S3776")
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

    // dummy container to handle inner classes access flags
    static class InnerClassAccessFlags extends AccessFlags {
        public InnerClassAccessFlags(InnerClass innerClass) {
            super(innerClass.getInnerAccessFlags());
        }
    }
}
