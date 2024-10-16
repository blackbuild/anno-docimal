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
import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.*;

public class JavaPoetClassVisitor extends ClassVisitor {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private final SpecConverter specConverter;
    private TypeSpec.Builder typeBuilder;
    private TypeSpec type;
    private String packageName;
    private final int innerClassModifiers;
    private ClassName className;

    public JavaPoetClassVisitor(SpecConverter specConverter, int innerClassModifiers) {
        super(CompilerConfiguration.ASM_API_VERSION);
        this.specConverter = specConverter;
        this.innerClassModifiers = innerClassModifiers;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaceNames) {
        className = fromInternalNameToClassName(name);
        typeBuilder = TypeSpec.classBuilder(className.simpleName());
        if (signature != null) {
            ClassSignatureParser.parseClassSignature(signature, typeBuilder);
        } else {
            typeBuilder.superclass(ClassName.bestGuess(fromInternalName(superName)));
            for (String interf : interfaceNames)
                if (!interf.equals("groovy/lang/GroovyObject"))
                    typeBuilder.addSuperinterface(ClassName.bestGuess(fromInternalName(interf)));
        }
        packageName = name.substring(0, name.lastIndexOf('/')).replace('/', '.');
        typeBuilder.addModifiers(decodeModifiers(innerClassModifiers != -1 ? innerClassModifiers : access));
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        /*
         * Class files generated for inner classes have an INNERCLASS
         * reference to self. The top level class access modifiers for
         * an inner class will not accurately reflect their access. For
         * example, top-level access modifiers for private inner classes
         * are package-private, protected inner classes are public, and
         * the static modifier is not included. So the INNERCLASS self
         * reference is used to capture the correct modifiers.
         *
         * Must compare against the fully qualified name because there may
         * be other INNERCLASS references to same named nested classes from
         * other classes.
         *
         * Example:
         *
         *   public final class org/foo/Groovy8632$Builder extends org/foo/Groovy8632Abstract$Builder  {
         *     public final static INNERCLASS org/foo/Groovy8632$Builder org/foo/Groovy8632 Builder
         *     public static abstract INNERCLASS org/foo/Groovy8632Abstract$Builder org/foo/Groovy8632Abstract Builder
         */
        if (innerName == null) return; // anonymous inner class
        if (name.replace('/', '.').equals(className.reflectionName())) {
            typeBuilder.modifiers.clear();
            typeBuilder.addModifiers(decodeModifiers(access));
            return;
        }

        try {
            String simpleName = name.substring(name.lastIndexOf('/') + 1);
            JavaPoetClassVisitor innerReader = specConverter.readClass(simpleName, access);
            typeBuilder.addType(innerReader.getType());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        /*
        if (fromInternalName(name).equals(typeBuilder.className)) {
            result.innerClassModifiers = access;
        }

         */
        //typeBuilder.addType()
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if ("<clinit>".equals(name)) return null;
        if (name.contains("$")) return null;
        if ((access & Opcodes.ACC_PRIVATE) != 0) return null;
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) return null;

        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(name).addModifiers(decodeModifiers(access));

        Type methodType = Type.getMethodType(desc);

        Type[] argumentTypes = methodType.getArgumentTypes();
        List<TypeName> parameterTypes = new ArrayList<>(argumentTypes.length);
        List<String> argumentNames = new ArrayList<>(argumentTypes.length);
        Map<Integer, List<AnnotationSpec>> parameterAnnotations = new HashMap<>();

        if (signature != null) {
            FormalParameterParser v = new FormalParameterParser() {
                @Override
                public SignatureVisitor visitParameterType() {
                    return new TypeSignatureParser() {
                        @Override
                        void finished(TypeName result) {
                            parameterTypes.add(result);
                        }
                    };
                }

                @Override
                public SignatureVisitor visitReturnType() {
                    return new TypeSignatureParser() {
                        @Override
                        void finished(TypeName result) {
                            methodBuilder.returns(result);
                        }
                    };
                }

                @Override
                public SignatureVisitor visitExceptionType() {
                    return new TypeSignatureParser() {
                        @Override
                        void finished(TypeName result) {
                            methodBuilder.addException(result);
                        }
                    };
                }
            };
            new SignatureReader(signature).accept(v);
            v.getTypeParameters().entrySet().stream()
                    .map(ClassSignatureParser::toTypeVariable)
                    .forEach(methodBuilder::addTypeVariable);
        } else {
            if (!name.equals("<init>")) {
                methodBuilder.returns(toTypeName(methodType.getReturnType()));
            }
            for (Type argumentType: argumentTypes) {
                parameterTypes.add(toTypeName(argumentType));
            }
            if (exceptions != null)
                for (String exception : exceptions) {
                    methodBuilder.addException(ClassName.bestGuess(fromInternalName(exception)));
                }
        }

        return new MethodVisitor(api) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                return MemberAnnotationVisitor.create(Type.getType(desc), methodBuilder);
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                List<AnnotationSpec> list = parameterAnnotations.computeIfAbsent(parameter, k -> new ArrayList<>());
                return MemberAnnotationVisitor.create(Type.getType(desc), list);
            }

            @Override
            public void visitParameter(String name, int ignored) {
                // ignore access, not relevant for our cause
                argumentNames.add(name);
            }

            @Override
            public void visitEnd() {
                for (int i = 0; i < parameterTypes.size(); i++) {
                    String paramName = argumentNames.size() > i ? argumentNames.get(i) : "param" + i;
                    List<AnnotationSpec> annotations = parameterAnnotations.get(i);
                    ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(parameterTypes.get(i), paramName);
                    if (annotations != null)
                        for (AnnotationSpec annotation : annotations)
                            parameterBuilder.addAnnotation(annotation);
                    methodBuilder.addParameter(parameterBuilder.build());
                }
                typeBuilder.addMethod(methodBuilder.build());
            }
        };
    }

    @Override
    public void visitEnd() {
        type = typeBuilder.build();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return MemberAnnotationVisitor.create(Type.getType(desc), typeBuilder);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (name.contains("$")) return null;
        if ((access & Opcodes.ACC_PRIVATE) != 0) return null;
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) return null;

        final TypeName[] fieldType = {null}; // Array to allow write access from inner class

        if (signature != null) {
            TypeSignatureParser signatureParser = new TypeSignatureParser() {
                @Override
                void finished(TypeName result) {
                    fieldType[0] = result;
                }
            };
            new SignatureReader(signature).accept(signatureParser);
        } else {
            fieldType[0] = toTypeName(Type.getType(desc));
        }

        return new FieldVisitor(api) {
            private final List<AnnotationSpec> annotations = new ArrayList<>();
            private String javadoc;

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                return MemberAnnotationVisitor.create(Type.getType(desc), annotations);
            }

            @Override
            public void visitEnd() {
                FieldSpec.Builder field = FieldSpec.builder(fieldType[0], name, decodeModifiers(access))
                        .addAnnotations(annotations);
                if (javadoc != null)
                    field.addJavadoc(javadoc);
                typeBuilder.addField(field.build());
            }
        };
    }

    static String fromInternalName(String name) {
        return name.replace('/', '.');
    }

    static ClassName fromInternalNameToClassName(String name) {
        String packageName = name.substring(0, name.lastIndexOf('/')).replace('/', '.');
        String className = name.substring(name.lastIndexOf('/') + 1);

        if (!className.contains("$")) {
            return ClassName.get(packageName, className);
        }

        String outerClassName = className.substring(0, className.lastIndexOf('$'));
        String nestedClassName = className.substring(className.lastIndexOf('$') + 1);

        return ClassName.get(packageName, outerClassName, nestedClassName.split("\\$"));
    }

    static Modifier[] decodeModifiers(int flags) {
        EnumSet<Modifier> result = EnumSet.noneOf(Modifier.class);

        if ((flags & Opcodes.ACC_PUBLIC) != 0) result.add(Modifier.PUBLIC);
        if ((flags & Opcodes.ACC_PRIVATE) != 0) result.add(Modifier.PRIVATE);
        if ((flags & Opcodes.ACC_PROTECTED) != 0) result.add(Modifier.PROTECTED);
        if ((flags & Opcodes.ACC_STATIC) != 0) result.add(Modifier.STATIC);
        if ((flags & Opcodes.ACC_FINAL) != 0) result.add(Modifier.FINAL);
        if ((flags & Opcodes.ACC_ABSTRACT) != 0) result.add(Modifier.ABSTRACT);

        return result.toArray(new Modifier[0]);
    }

    public TypeSpec getType() {
        return Objects.requireNonNull(type);
    }

    public String getPackageName() {
        return packageName;
    }

    private static TypeName toTypeName(Type type) {
        switch (type.getSort()) {
            case Type.OBJECT:
                return ClassName.bestGuess(type.getClassName());
            case Type.ARRAY:
                return ArrayTypeName.of(toTypeName(type.getElementType()));
            case Type.METHOD:
                throw new IllegalArgumentException("Method type not supported");
            case Type.VOID:
                return TypeName.VOID;
            case Type.BOOLEAN:
                return TypeName.BOOLEAN;
            case Type.BYTE:
                return TypeName.BYTE;
            case Type.CHAR:
                return TypeName.CHAR;
            case Type.DOUBLE:
                return TypeName.DOUBLE;
            case Type.FLOAT:
                return TypeName.FLOAT;
            case Type.INT:
                return TypeName.INT;
            case Type.LONG:
                return TypeName.LONG;
            case Type.SHORT:
                return TypeName.SHORT;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

}
