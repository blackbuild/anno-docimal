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

import com.squareup.javapoet.JavaFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Converts class-file declarations to their JavaPoet counterparts.
 */
final class SpecConverter {

    private static final String GROOVY_TRAIT_DESCRIPTOR = "Lgroovy/transform/Trait;";

    private final Path inputPath;
    private final ProjectionPolicy policy;
    private final Map<String, ClassData> classes = new LinkedHashMap<>();
    private final Set<String> includedClasses = new LinkedHashSet<>();
    private final ClassData root;

    private SpecConverter(Path inputPath, ProjectionPolicy policy) throws IOException {
        this.inputPath = inputPath;
        this.policy = policy;
        root = readRoot(inputPath);
        validateTopLevelRoot(root.node);
        loadNestedDeclarations(root);
        selectNestedDeclarations();
        validateSelectedMethods();
    }

    static SourceProjector.ProjectionResult project(Path inputPath, ProjectionPolicy policy) throws IOException {
        SpecConverter converter;
        try {
            converter = new SpecConverter(inputPath, policy);
        } catch (SourceProjectionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new SourceProjectionException(inputPath, null, "Could not read class metadata from " + inputPath,
                    exception);
        }
        try {
            JavaPoetClassVisitor rootVisitor = converter.readClass(converter.root.node.name);
            JavaFile javaFile = JavaFile.builder(rootVisitor.getPackageName(), rootVisitor.getType()).build();
            StringBuilder source = new StringBuilder();
            javaFile.writeTo(source);
            return new SourceProjector.ProjectionResult(converter.root.node.name, source.toString());
        } catch (SourceProjectionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw converter.failure(converter.root.node.name,
                    "Could not project selected declaration " + converter.identifier(converter.root.node.name), exception);
        }
    }

    JavaPoetClassVisitor readClass(String internalName) {
        ClassData classData = Objects.requireNonNull(classes.get(internalName), internalName);
        JavaPoetClassVisitor visitor = new JavaPoetClassVisitor(this, policy, includedClasses);
        new ClassReader(classData.bytecode).accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
        return visitor;
    }

    private ClassData readRoot(Path classFile) throws IOException {
        byte[] bytecode = Files.readAllBytes(classFile);
        ClassNode node = readMetadata(bytecode);
        ClassData result = new ClassData(node, bytecode, node.access, null, null, false);
        classes.put(node.name, result);
        return result;
    }

    private void validateTopLevelRoot(ClassNode node) {
        InnerClassNode self = node.innerClasses.stream()
                .filter(inner -> node.name.equals(inner.name))
                .findFirst()
                .orElse(null);
        if (node.outerClass != null || self != null && self.outerName != null) {
            throw failure(node.name, "Projection root must be a top-level declaration: " + identifier(node.name));
        }
    }

    private void loadNestedDeclarations(ClassData owner) throws IOException {
        for (InnerClassNode inner : directNamedMembers(owner.node)) {
            if (classes.containsKey(inner.name)) continue;
            Path nestedPath = inputPath.resolveSibling(simpleBinaryName(inner.name) + ".class");
            byte[] bytecode = Files.readAllBytes(nestedPath);
            ClassNode node = readMetadata(bytecode);
            if (!inner.name.equals(node.name)) {
                throw failure(inner.name, "Nested class metadata does not match " + nestedPath);
            }
            boolean groovyRuntimeArtifact = "Helper".equals(inner.innerName) && hasAnnotation(owner.node, GROOVY_TRAIT_DESCRIPTOR);
            ClassData nested = new ClassData(node, bytecode, inner.access, inner.outerName, inner.innerName,
                    groovyRuntimeArtifact);
            classes.put(node.name, nested);
            loadNestedDeclarations(nested);
        }
    }

    private void selectNestedDeclarations() {
        includedClasses.add(root.node.name);
        if (policy.isNestedDeclarationsIncluded()) {
            classes.values().stream()
                    .filter(classData -> classData != root)
                    .filter(this::isSelectedNestedDeclaration)
                    .map(classData -> classData.node.name)
                    .forEach(includedClasses::add);
        }

        boolean changed;
        do {
            changed = false;
            for (String includedName : List.copyOf(includedClasses)) {
                ClassData classData = classes.get(includedName);
                for (String referencedType : referencedTypes(classData.node)) {
                    if (!classes.containsKey(referencedType)) continue;
                    for (String requiredName : enclosingChain(referencedType)) {
                        changed |= includedClasses.add(requiredName);
                    }
                }
            }
        } while (changed);
    }

    private boolean isSelectedNestedDeclaration(ClassData classData) {
        return includesVisibility(classData.declarationAccess)
                && (policy.isSyntheticDeclarationsIncluded()
                || (classData.declarationAccess & Opcodes.ACC_SYNTHETIC) == 0)
                && (policy.isGroovyRuntimeArtifactsIncluded() || !classData.groovyRuntimeArtifact);
    }

    private Collection<String> enclosingChain(String className) {
        Deque<String> chain = new ArrayDeque<>();
        ClassData current = classes.get(className);
        while (current != null && current != root) {
            chain.addFirst(current.node.name);
            current = classes.get(current.outerName);
        }
        return chain;
    }

    private Set<String> referencedTypes(ClassNode node) {
        Set<String> result = new LinkedHashSet<>();
        scanSignature(node.signature, result);
        addInternalName(node.superName, result);
        node.interfaces.forEach(name -> addInternalName(name, result));
        scanAnnotations(node.visibleAnnotations, result);
        scanAnnotations(node.invisibleAnnotations, result);
        scanAnnotations(node.visibleTypeAnnotations, result);
        scanAnnotations(node.invisibleTypeAnnotations, result);

        for (FieldNode field : node.fields) {
            if (!ProjectionSelection.includesField(policy, field.access, field.name, node.access)) continue;
            scanType(Type.getType(field.desc), result);
            scanSignature(field.signature, result);
            scanAnnotations(field.visibleAnnotations, result);
            scanAnnotations(field.invisibleAnnotations, result);
            scanAnnotations(field.visibleTypeAnnotations, result);
            scanAnnotations(field.invisibleTypeAnnotations, result);
        }
        for (MethodNode method : node.methods) {
            if (!ProjectionSelection.includesMethod(policy, method.access, method.name, node.access)) continue;
            scanType(Type.getMethodType(method.desc), result);
            scanSignature(method.signature, result);
            if (method.exceptions != null) method.exceptions.forEach(name -> addInternalName(name, result));
            scanAnnotations(method.visibleAnnotations, result);
            scanAnnotations(method.invisibleAnnotations, result);
            scanAnnotations(method.visibleTypeAnnotations, result);
            scanAnnotations(method.invisibleTypeAnnotations, result);
            scanParameterAnnotations(method.visibleParameterAnnotations, result);
            scanParameterAnnotations(method.invisibleParameterAnnotations, result);
            scanAnnotationValue(method.annotationDefault, result);
        }
        return result;
    }

    private void validateSelectedMethods() {
        for (String className : includedClasses) {
            ClassNode node = classes.get(className).node;
            Map<String, MethodNode> signatures = new LinkedHashMap<>();
            for (MethodNode method : node.methods) {
                if (!ProjectionSelection.includesMethod(policy, method.access, method.name, node.access)) continue;
                Type[] arguments = Type.getArgumentTypes(method.desc);
                String key = method.name + Arrays.toString(Arrays.stream(arguments).map(Type::getDescriptor).toArray());
                MethodNode previous = signatures.putIfAbsent(key, method);
                if (previous != null) {
                    throw failure(identifier(node.name) + "#" + method.name,
                            "Selected bytecode methods cannot both be represented in Java: " + identifier(node.name)
                                    + "#" + method.name);
                }
            }
        }
    }

    private static ClassNode readMetadata(byte[] bytecode) {
        ClassNode node = new ClassNode();
        new ClassReader(bytecode).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    private static List<InnerClassNode> directNamedMembers(ClassNode owner) {
        return owner.innerClasses.stream()
                .filter(inner -> inner.innerName != null && owner.name.equals(inner.outerName))
                .sorted((left, right) -> left.name.compareTo(right.name))
                .toList();
    }

    private static boolean hasAnnotation(ClassNode node, String descriptor) {
        return containsAnnotation(node.visibleAnnotations, descriptor) || containsAnnotation(node.invisibleAnnotations, descriptor);
    }

    private static boolean containsAnnotation(List<AnnotationNode> annotations, String descriptor) {
        return annotations != null && annotations.stream().anyMatch(annotation -> descriptor.equals(annotation.desc));
    }

    private boolean includesVisibility(int access) {
        return policy.getIncludedVisibilities().contains(ProjectionSelection.visibility(access));
    }

    private static void scanSignature(String signature, Set<String> result) {
        if (signature == null) return;
        new SignatureReader(signature).accept(new SignatureVisitor(Opcodes.ASM9) {
            private String currentClass;

            @Override
            public void visitClassType(String name) {
                currentClass = name;
                result.add(name);
            }

            @Override
            public void visitInnerClassType(String name) {
                currentClass = currentClass + '$' + name;
                result.add(currentClass);
            }
        });
    }

    private static void scanType(Type type, Set<String> result) {
        switch (type.getSort()) {
            case Type.METHOD:
                Arrays.stream(type.getArgumentTypes()).forEach(argument -> scanType(argument, result));
                scanType(type.getReturnType(), result);
                break;
            case Type.ARRAY:
                scanType(type.getElementType(), result);
                break;
            case Type.OBJECT:
                result.add(type.getInternalName());
                break;
            default:
                break;
        }
    }

    private static void addInternalName(String name, Set<String> result) {
        if (name != null) result.add(name);
    }

    private static void scanParameterAnnotations(List<AnnotationNode>[] annotations, Set<String> result) {
        if (annotations == null) return;
        Arrays.stream(annotations).forEach(parameterAnnotations -> scanAnnotations(parameterAnnotations, result));
    }

    private static void scanAnnotations(Collection<? extends AnnotationNode> annotations, Set<String> result) {
        if (annotations == null) return;
        annotations.forEach(annotation -> {
            scanType(Type.getType(annotation.desc), result);
            if (annotation.values == null) return;
            for (int index = 1; index < annotation.values.size(); index += 2) {
                scanAnnotationValue(annotation.values.get(index), result);
            }
        });
    }

    private static void scanAnnotationValue(Object value, Set<String> result) {
        if (value instanceof Type) {
            scanType((Type) value, result);
        } else if (value instanceof AnnotationNode) {
            scanAnnotations(List.of((AnnotationNode) value), result);
        } else if (value instanceof List<?>) {
            ((List<?>) value).forEach(element -> scanAnnotationValue(element, result));
        } else if (value instanceof String[] && ((String[]) value).length > 0) {
            scanType(Type.getType(((String[]) value)[0]), result);
        }
    }

    private SourceProjectionException failure(String declaration, String message) {
        return new SourceProjectionException(inputPath, identifier(declaration), message);
    }

    private SourceProjectionException failure(String declaration, String message, Throwable cause) {
        return new SourceProjectionException(inputPath, identifier(declaration), message, cause);
    }

    private String identifier(String internalName) {
        if (internalName == null) return null;
        if (internalName.contains("#")) {
            int separator = internalName.indexOf('#');
            return identifier(internalName.substring(0, separator)) + internalName.substring(separator);
        }
        return internalName.replace('/', '.').replace('$', '.');
    }

    private static String simpleBinaryName(String internalName) {
        int separator = internalName.lastIndexOf('/');
        return separator < 0 ? internalName : internalName.substring(separator + 1);
    }

    private static final class ClassData {
        private final ClassNode node;
        private final byte[] bytecode;
        private final int declarationAccess;
        private final String outerName;
        private final String innerName;
        private final boolean groovyRuntimeArtifact;

        private ClassData(ClassNode node, byte[] bytecode, int declarationAccess, String outerName, String innerName,
                          boolean groovyRuntimeArtifact) {
            this.node = node;
            this.bytecode = bytecode;
            this.declarationAccess = declarationAccess;
            this.outerName = outerName;
            this.innerName = innerName;
            this.groovyRuntimeArtifact = groovyRuntimeArtifact;
        }
    }
}
