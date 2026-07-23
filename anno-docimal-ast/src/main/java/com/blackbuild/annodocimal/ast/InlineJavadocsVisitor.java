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

import com.blackbuild.annodocimal.ast.formatting.AnnoDocUtil;
import com.blackbuild.annodocimal.ast.parser.SourceExtractor;
import com.blackbuild.annodocimal.ast.parser.SourceExtractorFactory;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.control.SourceUnit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * AST visitor that inlines Javadoc comments as annotations.
 *
 * <p>For Groovy properties, the visitor preserves property documentation on the backing field and copies it to an
 * undocumented custom accessor. Generated accessor documentation is associated during source projection, where Groovy's
 * generated-member metadata remains available.</p>
 */
public class InlineJavadocsVisitor extends ClassCodeVisitorSupport {
    private final SourceUnit sourceUnit;

    private final SourceExtractor sourceExtractor;

    private final List<PropertyDocumentation> propertyDocumentation = new ArrayList<>();

    public InlineJavadocsVisitor(SourceUnit sourceUnit) {
        this.sourceUnit = sourceUnit;
        this.sourceExtractor = SourceExtractorFactory.getInstance().createSourceExtractor(sourceUnit);
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return sourceUnit;
    }

    @Override
    public void visitClass(ClassNode node) {
        addJavadocAsAnnotation(node);
        node.visitContents(this);
        applyPropertyDocumentation();
        for (Iterator<InnerClassNode> it = node.getInnerClasses(); it.hasNext(); ) {
            InnerClassNode innerClassNode = it.next();
            visitClass(innerClassNode);
        }
    }

    @Override
    protected void visitConstructorOrMethod(MethodNode node, boolean isConstructor) {
        addJavadocAsAnnotation(node);
    }

    @Override
    public void visitField(FieldNode node) {
        addJavadocAsAnnotation(node);
    }

    @Override
    public void visitProperty(PropertyNode node) {
        FieldNode field = node.getField();
        String documentation = sourceExtractor.getJavaDoc(node);
        if ((documentation == null || documentation.isBlank()) && field != null)
            documentation = sourceExtractor.getJavaDoc(field);
        if (documentation != null && !documentation.isBlank()) {
            propertyDocumentation.add(new PropertyDocumentation(node, documentation));
        }
    }

    private void applyPropertyDocumentation() {
        for (PropertyDocumentation property : propertyDocumentation) {
            addDocumentationIfAbsent(property.node().getField(), property.documentation());

            ClassNode owner = property.node().getField().getOwner();
            String propertyName = capitalize(property.node().getName());
            MethodNode getter = owner.getGetterMethod("get" + propertyName, false);
            if (getter == null && isBoolean(property.node()))
                getter = owner.getGetterMethod("is" + propertyName, false);
            addDocumentationIfAbsent(getter, property.documentation());

            MethodNode setter = owner.getSetterMethod("set" + propertyName, false);
            addDocumentationIfAbsent(setter, property.documentation());
        }
        propertyDocumentation.clear();
    }

    private static boolean isBoolean(PropertyNode property) {
        return ClassHelper.boolean_TYPE.equals(property.getType()) || ClassHelper.Boolean_TYPE.equals(property.getType());
    }

    private static String capitalize(String name) {
        if (name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static void addDocumentationIfAbsent(AnnotatedNode node, String documentation) {
        if (node == null || AnnoDocUtil.getDocumentationCarrierValue(node) != null) return;
        AnnoDocUtil.addDocumentation(node, documentation);
    }

    private void addJavadocAsAnnotation(AnnotatedNode node) {
        if (!node.getAnnotations(AnnoDocUtil.ANNODOC_ANNOTATION).isEmpty()
                || AnnoDocUtil.hasRuntimeGroovydoc(node)) return;

        String javadoc = sourceExtractor.getJavaDoc(node);
        if (javadoc != null && !javadoc.isBlank())
            AnnoDocUtil.addDocumentation(node, javadoc);
    }

    private record PropertyDocumentation(PropertyNode node, String documentation) {}

}
