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
package com.blackbuild.annodocimal.ast;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.io.FileReaderSource;
import org.codehaus.groovy.control.io.ReaderSource;
import org.codehaus.groovy.groovydoc.*;
import org.codehaus.groovy.tools.groovydoc.ArrayClassDocWrapper;
import org.codehaus.groovy.tools.groovydoc.GroovyDocTool;
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyParameter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GroovyDocToolSourceExtractor implements SourceExtractor {

    private static final String INSTANCE_METADATA_NAME = GroovyDocToolSourceExtractor.class.getName() + ".INSTANCE";

    private final GroovyRootDoc rootDoc;

    private GroovyDocToolSourceExtractor(GroovyRootDoc rootDoc) {
        this.rootDoc = rootDoc;
    }

    public static SourceExtractor create(SourceUnit sourceUnit) throws IOException {

        CompileUnit compileUnit = sourceUnit.getAST().getUnit();
        if (compileUnit == null || compileUnit.getModules().isEmpty())
            return EmptySourceExtractor.INSTANCE;

        // since CompileUnit has no Metatdata Groovy 2, we improvise
        // Modules should be complete by this stage, so we use the metadata of the
        // first module in the list
        Object existingExtractor = compileUnit.getModules().get(0).getNodeMetaData(INSTANCE_METADATA_NAME);

        if (existingExtractor instanceof SourceExtractor)
            return (SourceExtractor) existingExtractor;
        if (existingExtractor != null)
            throw new IllegalStateException("Unexpected metadata type: " + existingExtractor.getClass().getName());

        List<SourceUnitHolder> files = compileUnit.getModules().stream()
                .map(SourceUnitHolder::createSourceUnitHolder)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (files.isEmpty())
            return EmptySourceExtractor.INSTANCE;

        String[] sourceRoots = files.stream().map(f -> f.rootPath).collect(Collectors.toSet()).toArray(new String[0]);
        GroovyDocTool docTool = new GroovyDocTool(sourceRoots);

        List<String> sourceFiles = files.stream().map(f -> f.relativePath).collect(Collectors.toList());
        docTool.add(sourceFiles);

        GroovyDocToolSourceExtractor result = new GroovyDocToolSourceExtractor(docTool.getRootDoc());

        compileUnit.getModules().get(0).setNodeMetaData(INSTANCE_METADATA_NAME, result);
        return result;
    }

    static class SourceUnitHolder {
        final String rootPath;
        final String relativePath;

        SourceUnitHolder(File sourceRoot, File sourceFile) {
            this.rootPath = sourceRoot.getAbsolutePath();
            this.relativePath = sourceFile.getAbsolutePath().substring(sourceRoot.getAbsolutePath().length() + 1);
        }

        public static SourceUnitHolder createSourceUnitHolder(ModuleNode module) {
            SourceUnit sourceUnit = module.getContext();
            ReaderSource readerSource = sourceUnit.getSource();
            if (!(readerSource instanceof FileReaderSource)) {
                return null;
            }
            File sourceFile = ((FileReaderSource) readerSource).getFile();
            String packageName = sourceUnit.getAST().getPackageName();
            File sourceRoot = sourceFile.getParentFile();

            if (packageName != null && !packageName.isEmpty())
                for (String ignored : packageName.split("\\."))
                    sourceRoot = sourceRoot.getParentFile();

            return new SourceUnitHolder(sourceRoot, sourceFile);
        }
    }

    @Override
    public String getJavaDoc(AnnotatedNode node) {
        if (rootDoc == null)
            return null;
        if (node instanceof ClassNode)
            return reformat(getJavaDocForClass((ClassNode) node));
        else if (node instanceof MethodNode)
            return reformat(getJavaDocForMethod((MethodNode) node));
        else if (node instanceof FieldNode)
            return reformat(getJavaDocForField((FieldNode) node));
        else
            throw new IllegalArgumentException("Unsupported node type: " + node.getClass().getName());
    }

    private String reformat(String comment) {
        if (comment == null || comment.isBlank())
            return null;

        String[] lines = comment.split("\n");
        int minIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            if (line.trim().isEmpty())
                continue;

            int indent = 0;
            while (indent < line.length() && Character.isWhitespace(line.charAt(indent)))
                indent++;

            minIndent = Math.min(minIndent, indent);
        }

        int finalMinIndent = minIndent;
        String joined = Arrays.stream(lines)
                .map(line -> line.length() >= finalMinIndent ? line.substring(finalMinIndent) : "")
                .collect(Collectors.joining("\n"));

        int start = 0;
        while (start < joined.length() && joined.charAt(start) == '\n')
            start++;

        int end = joined.length();
        while (end > 0 && Character.isWhitespace(joined.charAt(end - 1)))
            end--;

        return joined.substring(start, end);
    }

    private String getJavaDocForField(FieldNode node) {
        if (node.getName().contains("$")) return null;
        GroovyClassDoc classDoc = getClassDoc(node.getDeclaringClass());
        if (classDoc == null)
            return null;
        return Stream.concat(Arrays.stream(classDoc.properties()), Arrays.stream(classDoc.fields()))
                .filter(fieldDoc -> fieldDoc.name().equals(node.getName()))
                .findFirst()
                .map(GroovyDoc::commentText)
                .orElse(null);
    }

    private String getJavaDocForMethod(MethodNode node) {
        if ((node.getModifiers() & 2) != 0) return null;
        if (node.getName().contains("$")) return null;

        GroovyClassDoc classDoc = getClassDoc(node.getDeclaringClass());
        if (classDoc == null)
            return null;
        if (node instanceof ConstructorNode) {
            return Arrays.stream(classDoc.constructors())
                    .filter(constructorDoc -> matches(node, constructorDoc))
                    .findFirst()
                    .map(GroovyDoc::commentText)
                    .orElse(null);
        }
        return Arrays.stream(classDoc.methods())
                .filter(methodDoc -> matches(node, methodDoc))
                .findFirst()
                .map(GroovyDoc::commentText)
                .orElseThrow(() -> new IllegalArgumentException("Method not found: " + node.getName()));
    }

    private boolean matches(MethodNode methodNode, GroovyExecutableMemberDoc methodDoc) {
        if (!methodDoc.name().equals(methodNode.getName())) return false;
        GroovyParameter[] docParameters = methodDoc.parameters();
        Parameter[] astParameters = methodNode.getParameters();
        if (docParameters.length != astParameters.length) return false;

        for (int i = 0; i < docParameters.length; i++) {
            if (!parameterMatches(docParameters[i], astParameters[i])) return false;
        }

        return true;
    }

    private boolean parameterMatches(GroovyParameter gdParameter, Parameter astParameter) {
        GroovyType docType = gdParameter.type();
        ClassNode astType = astParameter.getType();

        if (docType == null && astType == ClassHelper.DYNAMIC_TYPE) return true;
        String docTypeString = docTypeToString(gdParameter, docType);
        String astTypeString = astTypeToString(astType);
        // Dirty hack: in some cases, the docType is not fully qualified (i.e. List<String>)
        return docTypeString.equals(astTypeString) || astTypeString.endsWith("." + docTypeString);
    }

    private static String astTypeToString(ClassNode astType) {
        String type = astType.toString(false);
        if (astType.isUsingGenerics())
            type = type.substring(0, type.indexOf('<')).trim();
        return type;
    }

    private String docTypeToString(GroovyParameter gdParameter, GroovyType docType) {
        if (docType == null)
            return gdParameter.typeName();
        String typeName = docType.qualifiedTypeName();
        if (docType instanceof ArrayClassDocWrapper)
            return typeName + "[]";
        if (gdParameter instanceof SimpleGroovyParameter && ((SimpleGroovyParameter) gdParameter).vararg())
            return typeName + "[]";
        int indexOfGenerics = typeName.indexOf('<');
        if (indexOfGenerics > 0)
            return typeName.substring(0, indexOfGenerics).trim();
        return typeName;
    }

    private String getJavaDocForClass(ClassNode node) {
        GroovyClassDoc classDoc = getClassDoc(node);
        if (classDoc == null)
            return null;
        return classDoc.commentText();
    }

    private GroovyClassDoc getClassDoc(ClassNode node) {
        return rootDoc.classNamed(null, getGroovyDocClassName(node));
    }

    private static String getGroovyDocClassName(ClassNode node) {
        String packageName = node.getPackageName();
        if (packageName == null || packageName.isEmpty())
            return "DefaultPackage/" + node.getName().replace('$', '.');

        String simpleClassName = node.getName().substring(packageName.length() + 1).replace('$', '.');
        return packageName.replace('.', '/') + "/" + simpleClassName;
    }
}
