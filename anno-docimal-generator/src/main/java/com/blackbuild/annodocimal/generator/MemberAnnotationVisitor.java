package com.blackbuild.annodocimal.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Character.isISOControl;

public abstract class MemberAnnotationVisitor extends AnnotationVisitor {

    protected AnnotationSpec.Builder builder;

    protected MemberAnnotationVisitor(Type type) {
        super(CompilerConfiguration.ASM_API_VERSION);
        builder = AnnotationSpec.builder(toClassName(type));
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
                .map(MemberAnnotationVisitor::toCodeBlock)
                .collect(CodeBlock.joining(",", "{", "}"));
        else
            return CodeBlock.of("$L", value);
    }

    public static Stream<Object> streamArray(Object array) {
        if (array == null) throw new IllegalArgumentException("Array cannot be null");
        if (!array.getClass().isArray()) throw new IllegalArgumentException("Provided object is not an array");

        Class<?> componentType = array.getClass().getComponentType();
        if (componentType.isPrimitive()) {
            int length = Array.getLength(array);
            return IntStream.range(0, length).mapToObj(i -> Array.get(array, i));
        } else {
            return Arrays.stream((Object[]) array);
        }
    }

    static String characterLiteralWithoutSingleQuotes(char c) {
        // see https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6
        switch (c) {
            case '\b': return "\\b"; /* \u0008: backspace (BS) */
            case '\t': return "\\t"; /* \u0009: horizontal tab (HT) */
            case '\n': return "\\n"; /* \u000a: linefeed (LF) */
            case '\f': return "\\f"; /* \u000c: form feed (FF) */
            case '\r': return "\\r"; /* \u000d: carriage return (CR) */
            case '\"': return "\"";  /* \u0022: double quote (") */
            case '\'': return "\\'"; /* \u0027: single quote (') */
            case '\\': return "\\\\";  /* \u005c: backslash (\) */
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
        return new MemberAnnotationVisitor(Type.getType(descriptor)) {
            @Override
            void finish(AnnotationSpec annotationSpec) {
                MemberAnnotationVisitor.this.builder.addMember(name, "$L", annotationSpec);
            }
        };
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        return super.visitArray(name);
    }

    @Override
    public void visitEnd() {
        finish(builder.build());
    }

    abstract void finish(AnnotationSpec annotationSpec);
}
