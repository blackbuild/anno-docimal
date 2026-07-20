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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

class JavaPoetClassVisitor extends ClassVisitor {
    static final String ANNO_DOC_CLASS = "com.blackbuild.annodocimal.annotations.AnnoDoc";
    private static final String CONSTRUCTOR_NAME = "<init>";
    private final SpecConverter specConverter;
    private final ProjectionPolicy policy;
    private final Set<String> includedClasses;
    private final boolean groovyClass;
    private final Set<String> groovyRuntimeMethods;
    private final Set<String> groovyRuntimeFields;
    private TypeSpec.Builder typeBuilder;
    private TypeSpec type;
    private String internalName;
    private String packageName;
    private ClassName className;
    private TypeSpec.Kind kind;
    private boolean recordDeclaration;
    private final List<RecordComponentShape> recordComponents = new ArrayList<>();
    private final List<RecordShape> recordShapes = new ArrayList<>();

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
        internalName = name;
        recordDeclaration = (access & Opcodes.ACC_RECORD) != 0;
        prepareTypeBuilder(access, name);
        if (signature != null) {
            ClassSignatureParser.parseClassSignature(signature, typeBuilder,
                    recordDeclaration ? TypeSpec.Kind.INTERFACE : kind,
                    policy.isGroovyRuntimeArtifactsIncluded() || !groovyClass, specConverter::toClassName);
        } else {
            if (kind == TypeSpec.Kind.CLASS && !recordDeclaration)
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

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        final TypeName[] componentType = {null};
        if (signature == null) {
            componentType[0] = specConverter.toTypeName(Type.getType(descriptor));
        } else {
            new SignatureReader(signature).accept(new TypeSignatureParser(specConverter::toClassName) {
                @Override
                void finished(TypeName result) {
                    componentType[0] = result;
                }
            });
        }
        recordComponents.add(new RecordComponentShape(name, descriptor, componentType[0]));
        return null;
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
        recordShapes.addAll(innerReader.recordShapes);


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
        boolean canonicalRecordConstructor = recordDeclaration && name.equals(CONSTRUCTOR_NAME)
                && desc.equals("(" + recordComponents.stream()
                .map(RecordComponentShape::descriptor)
                .collect(joining()) + ")V");
        List<TypeName> parameterTypes = new ArrayList<>(argumentTypes.length);
        List<String> argumentNames = new ArrayList<>(argumentTypes.length);
        Map<Integer, List<AnnotationSpec>> parameterAnnotations = new HashMap<>();
        List<TypeName> exceptionTypes = new ArrayList<>();

        if (signature != null) {
            Map<String, TypeName> inheritedVariables = specConverter.inheritedTypeVariables(
                    internalName, name, desc, signature);
            Function<String, TypeName> variableResolver = variable -> inheritedVariables.getOrDefault(
                    variable, TypeVariableName.get(variable));
            FormalParameterParser v = new FormalParameterParser(specConverter::toClassName, variableResolver) {
                @Override
                public SignatureVisitor visitParameterType() {
                    return new TypeSignatureParser(specConverter::toClassName, variableResolver) {
                        @Override
                        void finished(TypeName result) {
                            parameterTypes.add(result);
                        }
                    };
                }

                @Override
                public SignatureVisitor visitReturnType() {
                    return new TypeSignatureParser(specConverter::toClassName, variableResolver) {
                        @Override
                        void finished(TypeName result) {
                            if (!name.equals(CONSTRUCTOR_NAME)) {
                                methodBuilder.returns(result);
                            }
                        }
                    };
                }

                @Override
                public SignatureVisitor visitExceptionType() {
                    return new TypeSignatureParser(specConverter::toClassName, variableResolver) {
                        @Override
                        void finished(TypeName result) {
                            exceptionTypes.add(result);
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
            if (exceptionTypes.isEmpty() && exceptions != null) {
                for (String exception : exceptions) {
                    exceptionTypes.add(specConverter.toClassName(exception));
                }
            }
        } else {
            if (!name.equals(CONSTRUCTOR_NAME)) {
                methodBuilder.returns(specConverter.toTypeName(methodType.getReturnType()));
            }
            int firstSourceParameter = hasImplicitOuterParameter ? 1 : 0;
            for (int index = firstSourceParameter; index < argumentTypes.length; index++) {
                parameterTypes.add(specConverter.toTypeName(argumentTypes[index]));
            }
            if (exceptions != null)
                for (String exception : exceptions) {
                    exceptionTypes.add(specConverter.toClassName(exception));
                }
        }
        exceptionTypes.forEach(methodBuilder::addException);

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
                return new MemberAnnotationVisitor.Regular(annotationType, methodBuilder, specConverter::toClassName);
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                int sourceParameter = hasImplicitOuterParameter ? parameter - 1 : parameter;
                if (sourceParameter < 0) return null;
                List<AnnotationSpec> list = parameterAnnotations.computeIfAbsent(sourceParameter, k -> new ArrayList<>());
                return MemberAnnotationVisitor.create(Type.getType(desc), list, specConverter::toClassName);
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
                return new MemberAnnotationVisitor.Regular(Type.getType(Object.class), null,
                        specConverter::toClassName) {

                    @Override
                    public void visitEnd() {
                        methodBuilder.defaultValue(singleMember());
                    }
                };
            }

            @Override
            public void visitEnd() {
                for (int i = 0; i < parameterTypes.size(); i++) {
                    String paramName;
                    if (canonicalRecordConstructor && recordComponents.size() > i) {
                        paramName = recordComponents.get(i).name();
                    } else if (argumentNames.size() > i) {
                        paramName = argumentNames.get(i);
                    } else {
                        paramName = "param" + i;
                    }
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

                boolean requiresReturn = !CONSTRUCTOR_NAME.equals(name)
                        && methodType.getReturnType().getSort() != Type.VOID
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
        if (!CONSTRUCTOR_NAME.equals(methodName) || typeBuilder.modifiers.contains(Modifier.STATIC)
                || className.enclosingClassName() == null || argumentTypes.length == 0) {
            return false;
        }
        return specConverter.toTypeName(argumentTypes[0]).equals(className.enclosingClassName());
    }

    @Override
    public void visitEnd() {
        type = typeBuilder.build();
        if (recordDeclaration) {
            recordShapes.add(new RecordShape(className.simpleName(), List.copyOf(recordComponents)));
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return MemberAnnotationVisitor.create(Type.getType(desc), typeBuilder, specConverter::toClassName);
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
                    return MemberAnnotationVisitor.create(Type.getType(desc), enumClass, specConverter::toClassName);
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
                    return MemberAnnotationVisitor.create(Type.getType(desc), field, specConverter::toClassName);
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

    String finishSource(String source) {
        for (RecordShape shape : recordShapes) {
            source = finishRecordSource(source, shape);
        }
        return source;
    }

    private static String finishRecordSource(String source, RecordShape shape) {
        String sourceForTypes = source;
        String components = shape.components().stream()
                .map(component -> sourceType(component.type(), sourceForTypes) + " " + component.name())
                .collect(joining(", "));

        String classDeclaration = "class " + shape.simpleName();
        int classStart = source.indexOf(classDeclaration);
        int declarationStart = source.lastIndexOf('\n', classStart) + 1;
        int declarationEnd = source.indexOf(" {", declarationStart);
        if (classStart < 0 || declarationEnd < 0) {
            throw new IllegalStateException("Projected record is missing its type declaration: " + shape.simpleName());
        }
        String declaration = source.substring(declarationStart, declarationEnd);
        String recordDeclaration = declaration.replaceFirst(
                "\\bfinal class " + Pattern.quote(shape.simpleName()) + "\\b", "record " + shape.simpleName());
        int interfacesStart = recordDeclaration.indexOf(" implements ");
        int componentsPosition = interfacesStart < 0 ? recordDeclaration.length() : interfacesStart;
        recordDeclaration = recordDeclaration.substring(0, componentsPosition)
                + "(" + components + ")"
                + recordDeclaration.substring(componentsPosition);
        source = source.substring(0, declarationStart) + recordDeclaration + source.substring(declarationEnd);

        String constructorToken = shape.simpleName() + "(";
        int recordBodyStart = source.indexOf(" {", declarationStart) + 2;
        ConstructorRange constructor = findCanonicalConstructor(source, constructorToken, recordBodyStart, components);
        if (constructor == null) return source;

        int constructorBody = constructor.parametersEnd() + 3;
        int constructorLineStart = source.lastIndexOf('\n', constructor.start()) + 1;
        int indentationEnd = constructorLineStart;
        while (indentationEnd < constructor.start() && Character.isWhitespace(source.charAt(indentationEnd))) {
            indentationEnd++;
        }
        String assignmentIndent = source.substring(constructorLineStart, indentationEnd) + "  ";
        String assignments = shape.components().stream()
                .map(component -> assignmentIndent + "this." + component.name()
                        + " = " + component.name() + ";\n")
                .collect(joining());
        return source.substring(0, constructorBody) + "\n" + assignments + source.substring(constructorBody);
    }

    private static ConstructorRange findCanonicalConstructor(String source, String constructorToken,
                                                              int searchStart, String components) {
        String normalizedComponents = normalizeParameters(components);
        int constructorStart = source.indexOf(constructorToken, searchStart);
        while (constructorStart >= 0) {
            int parametersEnd = source.indexOf(") {", constructorStart + constructorToken.length());
            if (parametersEnd < 0) return null;
            String candidateParameters = source.substring(
                    constructorStart + constructorToken.length(), parametersEnd);
            if (normalizeParameters(candidateParameters).equals(normalizedComponents)) {
                return new ConstructorRange(constructorStart, parametersEnd);
            }
            constructorStart = source.indexOf(constructorToken, parametersEnd);
        }
        return null;
    }

    private static String normalizeParameters(String parameters) {
        return parameters.replaceAll("\\s+", " ").trim();
    }

    private static String sourceType(TypeName type, String source) {
        String result = type.toString();
        Matcher imports = Pattern.compile("(?m)^import ([\\w.$]+);$").matcher(source);
        while (imports.find()) {
            String importedType = imports.group(1);
            result = result.replace(importedType, importedType.substring(importedType.lastIndexOf('.') + 1));
        }
        return result;
    }

    private record RecordComponentShape(String name, String descriptor, TypeName type) {}

    private record RecordShape(String simpleName, List<RecordComponentShape> components) {}

    private record ConstructorRange(int start, int parametersEnd) {}

}
