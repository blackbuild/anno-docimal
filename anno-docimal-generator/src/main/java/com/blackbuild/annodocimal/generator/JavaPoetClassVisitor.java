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
    private ClassName className;
    private TypeSpec.Kind kind;

    public JavaPoetClassVisitor(SpecConverter specConverter) {
        super(CompilerConfiguration.ASM_API_VERSION);
        this.specConverter = specConverter;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaceNames) {
        prepareTypeBuilder(access, name);
        if (signature != null) {
            ClassSignatureParser.parseClassSignature(signature, typeBuilder, kind);
        } else {
            if (kind == TypeSpec.Kind.CLASS)
                typeBuilder.superclass(ClassName.bestGuess(fromInternalName(superName)));
            for (String interf : interfaceNames) {
                if (interf.equals("groovy/lang/GroovyObject")) continue;
                if (kind == TypeSpec.Kind.ANNOTATION && interf.equals("java/lang/annotation/Annotation")) continue;
                typeBuilder.addSuperinterface(ClassName.bestGuess(fromInternalName(interf)));
            }
        }
        packageName = name.substring(0, name.lastIndexOf('/')).replace('/', '.');
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        super.visitAttribute(attribute);
    }

    private void prepareTypeBuilder(int access, String name) {
        className = fromInternalNameToClassName(name);
        kind = toJavaPoetKind(access);
        if (kind == TypeSpec.Kind.ENUM)
            access &= ~Opcodes.ACC_FINAL;
        if (kind == TypeSpec.Kind.ANNOTATION || kind == TypeSpec.Kind.INTERFACE)
            access &= ~Opcodes.ACC_ABSTRACT;
        typeBuilder = createTypeBuilder(className, kind).addModifiers(decodeModifiers(access));
    }

    private static TypeSpec.Kind toJavaPoetKind(int access) {
        if ((access & Opcodes.ACC_ANNOTATION) != 0)
            return TypeSpec.Kind.ANNOTATION;
        else if ((access & Opcodes.ACC_INTERFACE) != 0)
            return TypeSpec.Kind.INTERFACE;
        else if ((access & Opcodes.ACC_ENUM) != 0)
            return TypeSpec.Kind.ENUM;
        else
            return TypeSpec.Kind.CLASS;
    }

    private static TypeSpec.Builder createTypeBuilder(ClassName className, TypeSpec.Kind kind) {
        switch (kind) {
            case INTERFACE:
                return TypeSpec.interfaceBuilder(className);
            case ENUM:
                return TypeSpec.enumBuilder(className);
            case ANNOTATION:
                return TypeSpec.annotationBuilder(className);
            case CLASS:
                return TypeSpec.classBuilder(className);
            default:
                throw new IllegalArgumentException("Unknown kind: " + kind);
        }
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

        if (innerName.equals("Helper") && typeBuilder.annotations.stream().anyMatch(a -> a.type.toString().equals("groovy.transform.Trait"))) {
            return;
        }

        try {
            String simpleName = name.substring(name.lastIndexOf('/') + 1);
            JavaPoetClassVisitor innerReader = specConverter.readClass(simpleName);
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
        if (shouldIgnoreMethod(access, name)) return null;

        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(name).addModifiers(decodeModifiers(access));

        if (kind == TypeSpec.Kind.INTERFACE && (access & Opcodes.ACC_ABSTRACT) == 0 && (access & Opcodes.ACC_STATIC) == 0) {
            methodBuilder.addModifiers(Modifier.DEFAULT);
        }

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
                Type annotationType = Type.getType(desc);
                if (annotationType.getClassName().equals("org.codehaus.groovy.transform.trait.Traits$Implemented")) {
                    methodBuilder.modifiers.remove(Modifier.ABSTRACT);
                    methodBuilder.addModifiers(Modifier.DEFAULT);
                    return null;
                }
                return MemberAnnotationVisitor.create(annotationType, methodBuilder);
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
            public AnnotationVisitor visitAnnotationDefault() {
                return new MemberAnnotationVisitor.Regular(Type.getType(Object.class), null) {

                    @Override
                    public void visitEnd() {
                        if (this.builder.members.size() != 1)
                            throw new IllegalStateException("Expected exactly one member in default value");
                        this.builder.members.values().stream().map(l -> l.get(0)).forEach(methodBuilder::defaultValue);
                    }
                };
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

    private boolean shouldIgnoreMethod(int access, String name) {
        if ("<clinit>".equals(name)) return true;
        if (name.contains("$")) return true;
        if ((access & Opcodes.ACC_PRIVATE) != 0) return true;
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) return true;
        if (name.equals("getMetaClass")) return true;
        if (name.equals("setMetaClass")) return true;
        if (kind == TypeSpec.Kind.ENUM) {
            if (name.equals("<init>")) return true;
            if (name.equals("valueOf")) return true;
            if (name.equals("values")) return true;
            if (name.equals("previous")) return true;
            if (name.equals("next")) return true;
        }

        return false;
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
        if (shouldIgnoreField(access, name)) return null;

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

        if ((access & Opcodes.ACC_ENUM) != 0) {
            return new FieldVisitor(api) {
                private TypeSpec.Builder enumClass = TypeSpec.anonymousClassBuilder(CodeBlock.builder().build());

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    return MemberAnnotationVisitor.create(Type.getType(desc), enumClass);
                }

                @Override
                public void visitEnd() {
                    typeBuilder.addEnumConstant(name, enumClass.build());
                }
            };
        } else {
            return new FieldVisitor(api) {
                private FieldSpec.Builder field = FieldSpec.builder(fieldType[0], name, decodeModifiers(access));

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    return MemberAnnotationVisitor.create(Type.getType(desc), field);
                }

                @Override
                public void visitEnd() {
                    typeBuilder.addField(field.build());
                }
            };
        }

    }

    private boolean shouldIgnoreField(int access, String name) {
        if (name.contains("$")) return true;
        if ((access & Opcodes.ACC_PRIVATE) != 0) return true;
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) return true;
        if (kind == TypeSpec.Kind.ENUM) {
            if (name.equals("MAX_VALUE")) return true;
            if (name.equals("MIN_VALUE")) return true;
        }
        return false;
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
