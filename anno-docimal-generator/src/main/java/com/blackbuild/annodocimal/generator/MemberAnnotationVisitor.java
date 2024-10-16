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
import org.codehaus.groovy.control.CompilerConfiguration;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Character.isISOControl;
import static java.util.stream.Collectors.toList;

public class MemberAnnotationVisitor {

    private MemberAnnotationVisitor() {}

    public static AnnotationVisitor create(Type type, Object target) {

        if (type.getClassName().equals(AnnoDoc.class.getName()))
            return new Javadoc(target);
        else
            return new Regular(type, target);
    }

    public static class Regular extends AnnotationVisitor {

        protected final AnnotationSpec.Builder builder;
        private final Object target;

        private Regular(Type type, Object target) {
            super(CompilerConfiguration.ASM_API_VERSION);
            builder = AnnotationSpec.builder(toClassName(type));
            this.target = target;
        }

        private static ClassName toClassName(Type type) {
            return ClassName.bestGuess(type.getClassName());
        }

        @Override
        public void visit(String name, Object value) {
            builder.addMember(name, toCodeBlock(value));
        }

        static CodeBlock toCodeBlock(Object value) {
            if (value instanceof Type && ((Type) value).getSort() == Type.OBJECT)
                return CodeBlock.of("$T.class", toClassName((Type) value));
            else if (value instanceof String)
                return CodeBlock.of("$S", value);
            else if (value instanceof Float)
                return CodeBlock.of("$Lf", value);
            else if (value instanceof Long)
                return CodeBlock.of("$LL", value);
            else if (value instanceof Character)
                return CodeBlock.of("'$L'", characterLiteralWithoutSingleQuotes((char) value));
            else if (value.getClass().isArray())
                return streamArray(value)
                        .map(Regular::toCodeBlock)
                        .collect(CodeBlock.joining(", ", "{", "}"));
            else
                return CodeBlock.of("$L", value);
        }

        public static Stream<Object> streamArray(Object array) {
            if (array == null) throw new IllegalArgumentException("Array cannot be null");
            if (!array.getClass().isArray()) throw new IllegalArgumentException("Provided object is not an array");

            Class<?> componentType = array.getClass().getComponentType();
            if (componentType.isPrimitive()) {
                int length = java.lang.reflect.Array.getLength(array);
                return IntStream.range(0, length).mapToObj(i -> java.lang.reflect.Array.get(array, i));
            } else {
                return Arrays.stream((Object[]) array);
            }
        }

        static String characterLiteralWithoutSingleQuotes(char c) {
            // see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
            switch (c) {
                case '\b':
                    return "\\b"; /* \u0008: backspace (BS) */
                case '\t':
                    return "\\t"; /* \u0009: horizontal tab (HT) */
                case '\n':
                    return "\\n"; /* \u000a: linefeed (LF) */
                case '\f':
                    return "\\f"; /* \u000c: form feed (FF) */
                case '\r':
                    return "\\r"; /* \u000d: carriage return (CR) */
                case '\"':
                    return "\"";  /* \u0022: double quote (") */
                case '\'':
                    return "\\'"; /* \u0027: single quote (') */
                case '\\':
                    return "\\\\";  /* \u005c: backslash (\) */
                default:
                    return isISOControl(c) ? String.format("\\u%04x", (int) c) : Character.toString(c);
            }
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            builder.addMember(
                    name,
                    "$T.$L",
                    toClassName(Type.getType(descriptor)),
                    value);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            final AnnotationSpec.Builder outer = this.builder;
            return new MemberAnnotationVisitor.Regular(Type.getType(descriptor), null) {
                @Override
                public void visitEnd() {
                    outer.addMember(name, "$L", this.builder.build());
                }
            };
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return new MemberArray(builder, name);
        }

        @Override
        public void visitEnd() {
            if (target instanceof MethodSpec.Builder)
                ((MethodSpec.Builder) target).addAnnotation(builder.build());
            else if (target instanceof FieldSpec.Builder)
                ((FieldSpec.Builder) target).addAnnotation(builder.build());
            else if (target instanceof TypeSpec.Builder)
                ((TypeSpec.Builder) target).addAnnotation(builder.build());
            else if (target instanceof List)
                ((List) target).add(builder.build());
        }
    }

    public static class MemberArray extends AnnotationVisitor {

        private final AnnotationSpec.Builder target;
        private final String memberName;
        private final List<CodeBlock> elements = new ArrayList<>();

        private MemberArray(AnnotationSpec.Builder target, String memberName) {
            super(CompilerConfiguration.ASM_API_VERSION);
            this.target = target;
            this.memberName = memberName;
        }

        @Override
        public void visit(String name, Object value) {
            elements.add(Regular.toCodeBlock(value));
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            elements.add(CodeBlock.of("$T.$L", Regular.toClassName(Type.getType(descriptor)), value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            return new MemberAnnotationVisitor.Regular(Type.getType(descriptor), null) {
                @Override
                public void visitEnd() {
                    elements.add(CodeBlock.of("$L", this.builder.build()));
                }
            };
        }

        @Override
        public void visitEnd() {
            target.addMember(memberName, elements.stream().collect(CodeBlock.joining(", ", "{", "}")));
        }
    }

    public static class Javadoc extends AnnotationVisitor {

        private final Object target;
        private String javadocText;

        private Javadoc(Object target) {
            super(CompilerConfiguration.ASM_API_VERSION);
            this.target = target;
        }

        @Override
        public void visit(String name, Object value) {
            if (name.equals("value"))
                javadocText = (String) value;
        }

        @Override
        public void visitEnd() {
            if (javadocText == null) return;
            if (target instanceof MethodSpec.Builder) {
                ((MethodSpec.Builder) target).addJavadoc(filterParams(javadocText));
            } else if (target instanceof FieldSpec.Builder) {
                ((FieldSpec.Builder) target).addJavadoc(javadocText);
            } else if (target instanceof TypeSpec.Builder) {
                ((TypeSpec.Builder) target).addJavadoc(javadocText);
            }
        }

        private static final Pattern PARAM_PATTERN = Pattern.compile("(?m)^\\s*@param\\s+(\\w+)\\s*");

        private String filterParams(String stringValue) {
            MethodSpec.Builder methodSpec = ((MethodSpec.Builder) target);
            if (methodSpec.parameters.isEmpty() && !stringValue.contains("@param"))
                return stringValue;

            List<String> argumentNames = methodSpec.parameters.stream().map(p -> p.name).collect(toList());

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

    }
}
