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
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toSet;

class JavaPoetClassVisitor extends ClassVisitor {
    static final String ANNO_DOC_CLASS = "com.blackbuild.annodocimal.annotations.AnnoDoc";
    private final SpecConverter specConverter;
    private final ProjectionPolicy policy;
    private final Set<String> includedClasses;
    private final boolean groovyClass;
    private final Set<String> groovyRuntimeMethods;
    private final Set<String> groovyRuntimeFields;
    private TypeSpec.Builder typeBuilder;
    private TypeSpec type;
    private String packageName;
    private ClassName className;
    private TypeSpec.Kind kind;

    JavaPoetClassVisitor(SpecConverter specConverter, ProjectionPolicy policy, Set<String> includedClasses,
                         boolean groovyClass, Set<String> groovyRuntimeMethods, Set<String> groovyRuntimeFields) {
        super(CompilerConfiguration.ASM_API_VERSION);
        this.specConverter = specConverter;
        this.policy = policy;
        this.includedClasses = includedClasses;
        this.groovyClass = groovyClass;
        this.groovyRuntimeMethods = groovyRuntimeMethods;
        this.groovyRuntimeFields = groovyRuntimeFields;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaceNames) {
        prepareTypeBuilder(access, name);
        if (signature != null) {
            ClassSignatureParser.parseClassSignature(signature, typeBuilder, kind,
                    policy.isGroovyRuntimeArtifactsIncluded() || !groovyClass, specConverter::toClassName);
        } else {
            if (kind == TypeSpec.Kind.CLASS)
                typeBuilder.superclass(specConverter.toClassName(superName));
            for (String interf : interfaceNames) {
                if (groovyClass && !policy.isGroovyRuntimeArtifactsIncluded()
                        && interf.equals("groovy/lang/GroovyObject")) continue;
                if (kind == TypeSpec.Kind.ANNOTATION && interf.equals("java/lang/annotation/Annotation")) continue;
                typeBuilder.addSuperinterface(specConverter.toClassName(interf));
            }
        }
        int packageSeparator = name.lastIndexOf('/');
        packageName = packageSeparator < 0 ? "" : name.substring(0, packageSeparator).replace('/', '.');
    }

    private void prepareTypeBuilder(int access, String name) {
        className = specConverter.toClassName(name);
        kind = toJavaPoetKind(access);
        if (kind == TypeSpec.Kind.ENUM)
            access &= ~Opcodes.ACC_FINAL;
        if (kind == TypeSpec.Kind.ANNOTATION || kind == TypeSpec.Kind.INTERFACE)
            access &= ~Opcodes.ACC_ABSTRACT;
        typeBuilder = createTypeBuilder(className, kind).addModifiers(TypeConversion.decodeModifiers(access));
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
        if (innerName == null || outerName == null) return; // anonymous inner class
        if (name.replace('/', '.').equals(className.reflectionName())) {
            typeBuilder.modifiers.clear();
            kind = toJavaPoetKind(access);
            if (kind == TypeSpec.Kind.ENUM)
                access &= ~Opcodes.ACC_FINAL;
            if (kind == TypeSpec.Kind.ANNOTATION || kind == TypeSpec.Kind.INTERFACE)
                access &= ~Opcodes.ACC_ABSTRACT;

            typeBuilder.addModifiers(TypeConversion.decodeModifiers(access));
            return;
        }

        if (!outerName.equals(className.reflectionName().replace('.', '/'))) {
            return;
        }

        if (!includedClasses.contains(name)) return;

        JavaPoetClassVisitor innerReader = specConverter.readClass(name);
        typeBuilder.addType(innerReader.getType());


        /*
        if (fromInternalName(name).equals(typeBuilder.className)) {
            result.innerClassModifiers = access;
        }

         */
        //typeBuilder.addType()
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (!ProjectionSelection.includesMethod(policy, access, name, typeAccess(),
                groovyRuntimeMethods.contains(ProjectionSelection.memberKey(name, desc)))) return null;

        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(name).addModifiers(TypeConversion.decodeModifiers(access));

        if (kind == TypeSpec.Kind.INTERFACE && (access & Opcodes.ACC_ABSTRACT) == 0 && (access & Opcodes.ACC_STATIC) == 0) {
            methodBuilder.addModifiers(Modifier.DEFAULT);
        }

        Type methodType = Type.getMethodType(desc);

        Type[] argumentTypes = methodType.getArgumentTypes();
        boolean hasImplicitOuterParameter = hasImplicitOuterParameter(name, argumentTypes);
        List<TypeName> parameterTypes = new ArrayList<>(argumentTypes.length);
        List<String> argumentNames = new ArrayList<>(argumentTypes.length);
        Map<Integer, List<AnnotationSpec>> parameterAnnotations = new HashMap<>();

        if (signature != null) {
            FormalParameterParser v = new FormalParameterParser(specConverter::toClassName) {
                @Override
                public SignatureVisitor visitParameterType() {
                    return new TypeSignatureParser(specConverter::toClassName) {
                        @Override
                        void finished(TypeName result) {
                            parameterTypes.add(result);
                        }
                    };
                }

                @Override
                public SignatureVisitor visitReturnType() {
                    return new TypeSignatureParser(specConverter::toClassName) {
                        @Override
                        void finished(TypeName result) {
                            methodBuilder.returns(result);
                        }
                    };
                }

                @Override
                public SignatureVisitor visitExceptionType() {
                    return new TypeSignatureParser(specConverter::toClassName) {
                        @Override
                        void finished(TypeName result) {
                            methodBuilder.addException(result);
                        }
                    };
                }
            };
            new SignatureReader(signature).accept(v);
            if (hasImplicitOuterParameter && parameterTypes.size() == argumentTypes.length) {
                parameterTypes.remove(0);
            }
            v.getTypeParameters().entrySet().stream()
                    .map(ClassSignatureParser::toTypeVariable)
                    .forEach(methodBuilder::addTypeVariable);
        } else {
            if (!name.equals("<init>")) {
                methodBuilder.returns(specConverter.toTypeName(methodType.getReturnType()));
            }
            int firstSourceParameter = hasImplicitOuterParameter ? 1 : 0;
            for (int index = firstSourceParameter; index < argumentTypes.length; index++) {
                parameterTypes.add(specConverter.toTypeName(argumentTypes[index]));
            }
            if (exceptions != null)
                for (String exception : exceptions) {
                    methodBuilder.addException(specConverter.toClassName(exception));
                }
        }

        return new MethodVisitor(api) {
            String extractedJavadoc = null;
            int visitedParameters;

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                Type annotationType = Type.getType(desc);
                if (annotationType.getClassName().equals("org.codehaus.groovy.transform.trait.Traits$Implemented")) {
                    methodBuilder.modifiers.remove(Modifier.ABSTRACT);
                    methodBuilder.addModifiers(Modifier.DEFAULT);
                    return null;
                }
                if (annotationType.getClassName().equals(ANNO_DOC_CLASS)) {
                    return new MemberAnnotationVisitor.Javadoc(null) {
                        @Override
                        public void visitEnd() {
                            extractedJavadoc = javadocText;
                        }
                    };
                }
                return new MemberAnnotationVisitor.Regular(annotationType, methodBuilder);
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                int sourceParameter = hasImplicitOuterParameter ? parameter - 1 : parameter;
                if (sourceParameter < 0) return null;
                List<AnnotationSpec> list = parameterAnnotations.computeIfAbsent(sourceParameter, k -> new ArrayList<>());
                return MemberAnnotationVisitor.create(Type.getType(desc), list);
            }

            @Override
            public void visitParameter(String name, int access) {
                boolean implicitOuter = hasImplicitOuterParameter && visitedParameters++ == 0
                        && ((access & (Opcodes.ACC_SYNTHETIC | Opcodes.ACC_MANDATED)) != 0 || name.startsWith("this$"));
                if (implicitOuter) return;
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

                if (extractedJavadoc != null) {
                    methodBuilder.addJavadoc(filterParams(extractedJavadoc));
                }

                boolean requiresReturn = !"<init>".equals(name) && methodType.getReturnType().getSort() != Type.VOID
                        && !methodBuilder.modifiers.contains(Modifier.ABSTRACT)
                        && !methodBuilder.modifiers.contains(Modifier.NATIVE);
                if (requiresReturn) {
                    methodBuilder.addStatement("return $L", fieldInitializer(methodType.getReturnType(), null));
                }

                typeBuilder.addMethod(methodBuilder.build());
            }

            private static final Pattern PARAM_PATTERN = Pattern.compile("(?m)^\\s*@param\\s+(\\w+)\\s*");

            private String filterParams(String rawJavadoc) {
                if (methodBuilder.parameters.isEmpty() && !rawJavadoc.contains("@param"))
                    return rawJavadoc;

                if (parameterTypes.size() > argumentNames.size())
                    // method has more parameters than arguments, or we have missing 'parameters' option in compiler (or Groovy 2.4)
                    return rawJavadoc;

                List<String> argumentNames = methodBuilder.parameters.stream().map(p -> p.name).toList();

                Set<String> names = PARAM_PATTERN.matcher(rawJavadoc)
                        .results()
                        .map(match -> match.group(1))
                        .collect(toSet());

                argumentNames.forEach(names::remove);

                if (names.isEmpty())
                    // all params point to actual arguments
                    return rawJavadoc;

                Pattern badParams = Pattern.compile("(?sm)^\\s*@param\\s+(" + String.join("|", names) + ").*?(?=(^@\\w+|$))");
                return badParams.matcher(rawJavadoc).replaceAll("");
            }

        };
    }

    private boolean hasImplicitOuterParameter(String methodName, Type[] argumentTypes) {
        if (!"<init>".equals(methodName) || typeBuilder.modifiers.contains(Modifier.STATIC)
                || className.enclosingClassName() == null || argumentTypes.length == 0) {
            return false;
        }
        return specConverter.toTypeName(argumentTypes[0]).equals(className.enclosingClassName());
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
        if (!ProjectionSelection.includesField(policy, access, name, typeAccess(),
                groovyRuntimeFields.contains(ProjectionSelection.memberKey(name, desc)))) return null;

        final TypeName[] fieldType = {null}; // Array to allow write access from inner class

        if (signature != null) {
            TypeSignatureParser signatureParser = new TypeSignatureParser(specConverter::toClassName) {
                @Override
                void finished(TypeName result) {
                    fieldType[0] = result;
                }
            };
            new SignatureReader(signature).accept(signatureParser);
        } else {
            fieldType[0] = specConverter.toTypeName(Type.getType(desc));
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
                private FieldSpec.Builder field = FieldSpec.builder(fieldType[0], name, TypeConversion.decodeModifiers(access));

                {
                    if ((access & Opcodes.ACC_FINAL) != 0) {
                        field.initializer("$L", fieldInitializer(Type.getType(desc), value));
                    }
                }

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

    private int typeAccess() {
        int access = 0;
        if (typeBuilder.modifiers.contains(Modifier.PUBLIC)) access |= Opcodes.ACC_PUBLIC;
        if (typeBuilder.modifiers.contains(Modifier.PROTECTED)) access |= Opcodes.ACC_PROTECTED;
        if (typeBuilder.modifiers.contains(Modifier.PRIVATE)) access |= Opcodes.ACC_PRIVATE;
        if (kind == TypeSpec.Kind.ENUM) access |= Opcodes.ACC_ENUM;
        return access;
    }

    private static CodeBlock fieldInitializer(Type fieldType, Object constantValue) {
        if (constantValue instanceof Integer integerValue) {
            if (fieldType.getSort() == Type.BOOLEAN) return CodeBlock.of("$L", integerValue != 0);
            if (fieldType.getSort() == Type.CHAR) {
                return MemberAnnotationVisitor.Regular.toCodeBlock((char) integerValue.intValue());
            }
        }
        if ((constantValue instanceof Float floatValue && !Float.isFinite(floatValue))
                || (constantValue instanceof Double doubleValue && !Double.isFinite(doubleValue))) {
            constantValue = null;
        }
        if (constantValue != null) return MemberAnnotationVisitor.Regular.toCodeBlock(constantValue);

        return switch (fieldType.getSort()) {
            case Type.BOOLEAN -> CodeBlock.of("false");
            case Type.CHAR -> CodeBlock.of("'\\0'");
            case Type.LONG -> CodeBlock.of("0L");
            case Type.FLOAT -> CodeBlock.of("0.0f");
            case Type.DOUBLE -> CodeBlock.of("0.0d");
            case Type.BYTE, Type.SHORT, Type.INT -> CodeBlock.of("0");
            default -> CodeBlock.of("null");
        };
    }

    TypeSpec getType() {
        return Objects.requireNonNull(type);
    }

    String getPackageName() {
        return packageName;
    }

}
