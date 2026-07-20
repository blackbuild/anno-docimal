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

import com.squareup.javapoet.*;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Character.isISOControl;

final class MemberAnnotationVisitor {

    private MemberAnnotationVisitor() {}

    static AnnotationVisitor create(Type type, Object target, Function<String, ClassName> classNameResolver) {

        if (type.getClassName().equals(JavaPoetClassVisitor.ANNO_DOC_CLASS))
            return new Javadoc(target);
        else
            return new Regular(type, target, classNameResolver);
    }

    static class Regular extends AnnotationVisitor {

        private final AnnotationSpec.Builder builder;
        private final Map<String, CodeBlock> members =
                new TreeMap<>(Comparator.nullsFirst(Comparator.naturalOrder()));
        private final Object target;
        private final Function<String, ClassName> classNameResolver;

        Regular(Type type, Object target, Function<String, ClassName> classNameResolver) {
            super(CompilerConfiguration.ASM_API_VERSION);
            this.classNameResolver = Objects.requireNonNull(classNameResolver, "classNameResolver");
            builder = AnnotationSpec.builder(toClassName(type));
            this.target = target;
        }

        private ClassName toClassName(Type type) {
            return classNameResolver.apply(type.getInternalName());
        }

        @Override
        public void visit(String name, Object value) {
            addMember(name, toCodeBlock(value, classNameResolver));
        }

        static CodeBlock toCodeBlock(Object value) {
            return toCodeBlock(value, TypeConversion::fromInternalNameToClassName);
        }

        private static CodeBlock toCodeBlock(Object value, Function<String, ClassName> classNameResolver) {
            if (value instanceof Type type && type.getSort() == Type.OBJECT)
                return CodeBlock.of("$T.class", classNameResolver.apply(type.getInternalName()));
            else if (value instanceof String string)
                return CodeBlock.of("$S", string);
            else if (value instanceof Float f)
                return CodeBlock.of("$Lf", f);
            else if (value instanceof Long l)
                return CodeBlock.of("$LL", l);
            else if (value instanceof Character c)
                return CodeBlock.of("'$L'", characterLiteralWithoutSingleQuotes(c));
            else if (value.getClass().isArray())
                return streamArray(value)
                        .map(element -> toCodeBlock(element, classNameResolver))
                        .collect(CodeBlock.joining(", ", "{", "}"));
            else
                return CodeBlock.of("$L", value);
        }

        static Stream<Object> streamArray(Object array) {
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
            return switch (c) {
                case '\b' -> "\\b"; /* \u0008: backspace (BS) */
                case '\t' -> "\\t"; /* \u0009: horizontal tab (HT) */
                case '\n' -> "\\n"; /* \u000a: linefeed (LF) */
                case '\f' -> "\\f"; /* \u000c: form feed (FF) */
                case '\r' -> "\\r"; /* \u000d: carriage return (CR) */
                case '\"' -> "\"";  /* \u0022: double quote (") */
                case '\'' -> "\\'"; /* \u0027: single quote (') */
                case '\\' -> "\\\\";  /* \u005c: backslash (\) */
                default -> isISOControl(c) ? String.format("\\u%04x", (int) c) : Character.toString(c);
            };
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            addMember(name, CodeBlock.of("$T.$L", toClassName(Type.getType(descriptor)), value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            return new MemberAnnotationVisitor.Regular(Type.getType(descriptor), null, classNameResolver) {
                @Override
                public void visitEnd() {
                    addMember(name, CodeBlock.of("$L", buildAnnotation()));
                }
            };
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return new MemberArray(this, name, classNameResolver);
        }

        @Override
        public void visitEnd() {
            AnnotationSpec annotation = buildAnnotation();
            if (target instanceof MethodSpec.Builder methodBuilder)
                methodBuilder.addAnnotation(annotation);
            else if (target instanceof FieldSpec.Builder fieldBuilder)
                fieldBuilder.addAnnotation(annotation);
            else if (target instanceof TypeSpec.Builder typeBuilder)
                typeBuilder.addAnnotation(annotation);
            else if (target instanceof List list)
                list.add(annotation);
        }

        private void addMember(String name, CodeBlock value) {
            members.put(name, value);
        }

        protected final CodeBlock singleMember() {
            if (members.size() != 1)
                throw new IllegalStateException("Expected exactly one member in default value");
            return members.values().iterator().next();
        }

        protected final AnnotationSpec buildAnnotation() {
            members.forEach((name, value) -> builder.addMember(name, "$L", value));
            return builder.build();
        }
    }

    static class MemberArray extends AnnotationVisitor {

        private final Regular target;
        private final String memberName;
        private final List<CodeBlock> elements = new ArrayList<>();
        private final Function<String, ClassName> classNameResolver;

        private MemberArray(Regular target, String memberName,
                            Function<String, ClassName> classNameResolver) {
            super(CompilerConfiguration.ASM_API_VERSION);
            this.target = target;
            this.memberName = memberName;
            this.classNameResolver = classNameResolver;
        }

        @Override
        public void visit(String name, Object value) {
            elements.add(Regular.toCodeBlock(value, classNameResolver));
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            elements.add(CodeBlock.of("$T.$L", classNameResolver.apply(Type.getType(descriptor).getInternalName()), value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            return new MemberAnnotationVisitor.Regular(Type.getType(descriptor), null, classNameResolver) {
                @Override
                public void visitEnd() {
                    elements.add(CodeBlock.of("$L", buildAnnotation()));
                }
            };
        }

        @Override
        public void visitEnd() {
            target.addMember(memberName, elements.stream().collect(CodeBlock.joining(", ", "{", "}")));
        }
    }

    static class Javadoc extends AnnotationVisitor {

        private final Object target;
        protected String javadocText;

        Javadoc(Object target) {
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
            if (target instanceof MethodSpec.Builder methodBuilder) {
                methodBuilder.addJavadoc(javadocText);
            } else if (target instanceof FieldSpec.Builder fieldBuilder) {
                fieldBuilder.addJavadoc(javadocText);
            } else if (target instanceof TypeSpec.Builder typeBuilder) {
                typeBuilder.addJavadoc(javadocText);
            }
        }


    }

    static final class DocumentationCarrierSelection {
        private static final Pattern SOURCE_COMMENT = Pattern.compile(
                "^\\s*/\\*\\*(@?)(.*)\\*/\\s*$", Pattern.DOTALL);
        private static final Pattern LINE_DECORATION = Pattern.compile("(?m)^[\\t ]*\\*[\\t ]?");
        private String canonical;
        private String interoperable;

        AnnotationVisitor visitor(Type type) {
            boolean canonicalCarrier = type.getClassName().equals(JavaPoetClassVisitor.ANNO_DOC_CLASS);
            boolean interoperableCarrier = type.getClassName().equals(JavaPoetClassVisitor.GROOVYDOC_CLASS);
            if (!canonicalCarrier && !interoperableCarrier) return null;

            return new Javadoc(null) {
                @Override
                public void visitEnd() {
                    String normalized = normalize(javadocText);
                    if (canonicalCarrier) canonical = normalized;
                    else interoperable = normalized;
                }
            };
        }

        String selected() {
            return canonical != null ? canonical : interoperable;
        }

        private static String normalize(String text) {
            if (text == null) return null;
            String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
            Matcher sourceComment = SOURCE_COMMENT.matcher(normalized);
            if (sourceComment.matches()) {
                normalized = LINE_DECORATION.matcher(sourceComment.group(2)).replaceAll("");
            }
            normalized = normalized.stripIndent().strip();
            return normalized.isBlank() ? null : normalized;
        }
    }
}
