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

import com.blackbuild.annodocimal.annotations.Javadocs;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.control.SourceUnit;

import java.io.IOException;
import java.util.Iterator;

public class InlineJavadocVisitor extends ClassCodeVisitorSupport {
    private final SourceUnit sourceUnit;

    private final SourceExtractor sourceExtractor;

    private static final ClassNode JAVADOC_ANNOTATION = ClassHelper.make(Javadocs.class);

    public InlineJavadocVisitor(SourceUnit sourceUnit) {
        this.sourceUnit = sourceUnit;
        try {
            this.sourceExtractor = new GroovyDocToolSourceExtractor(sourceUnit);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected SourceUnit getSourceUnit() {
        return sourceUnit;
    }

    @Override
    public void visitClass(ClassNode node) {
        addJavadocAsAnnotation(node);
        node.visitContents(this);
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
        // do nothing
    }

    private void addJavadocAsAnnotation(AnnotatedNode node) {
        if (!node.getAnnotations(JAVADOC_ANNOTATION).isEmpty()) return;

        String javadoc = sourceExtractor.getJavaDoc(node);
        if (javadoc != null) {
            AnnotationNode annotation = new AnnotationNode(JAVADOC_ANNOTATION);
            annotation.addMember("value", new ConstantExpression(javadoc));
            node.addAnnotation(annotation);
        }
    }
}
