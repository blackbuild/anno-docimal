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

import com.blackbuild.annodocimal.annotations.InlineJavadocs;
import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;

/**
 * Annotation processor that processes the {@link InlineJavadocs} annotation.
 * For classes annotated with this annotation, it generates a properties file with
 * the javadocs for each method, field and class.
 * <p>
 * This class is loosely base on <a href="https://github.com/dnault/therapi-runtime-javadoc/blob/main/therapi-runtime-javadoc-scribe/src/main/java/com/github/therapi/runtimejavadoc/scribe/JavadocAnnotationProcessor.java">JavadocAnnotationProcessor</a>,
 * with the following changes:
 * <ul>
 *     <li>No global transformation, only classes with {@link InlineJavadocs} are processed</li>
 *     <li>storage is in a properties file, not in a json structure. This reduces dependencies.</li>
 * </ul>.
 */
@SupportedAnnotationTypes("com.blackbuild.annodocimal.annotations.InlineJavadocs")
@AutoService(javax.annotation.processing.Processor.class)
public class AnnoDocimalAnnotationProcessor extends AbstractProcessor {

    private JavadocPropertiesBuilder javadocPropertiesBuilder;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        javadocPropertiesBuilder = new JavadocPropertiesBuilder(processingEnv);

        for (TypeElement annotation : annotations) {
            roundEnvironment.getElementsAnnotatedWith(annotation).stream()
                    .filter(TypeElement.class::isInstance)
                    .map(TypeElement.class::cast)
                    .forEach(this::generateJavadocForClass);
        }
        return false;
    }

    private void generateJavadocForClass(TypeElement type) {
        try {
            doGenerateJavadocForClass(type);
        } catch (Exception ex) {
            processingEnv.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "Javadoc retention failed; " + ex, type);
            throw new RuntimeException("Javadoc retention failed for " + type, ex);
        }

        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed instanceof TypeElement)
                generateJavadocForClass((TypeElement) enclosed);
        }
    }

    private void doGenerateJavadocForClass(TypeElement classElement) throws IOException {
        Properties classJavadoc = javadocPropertiesBuilder.getClassJavadoc(classElement);
        if (classJavadoc != null) {
            writeJavadocProperties(classElement, classJavadoc);
        }
    }

    private void writeJavadocProperties(TypeElement classElement, Properties classJsonDoc) throws IOException {
        FileObject resource = createJavadocResourceFile(classElement);
        try (OutputStream os = resource.openOutputStream()) {
            classJsonDoc.store(os, null);
        }
    }

    private FileObject createJavadocResourceFile(TypeElement classElement) throws IOException {
        PackageElement packageElement = getPackageElement(classElement);
        String packageName = packageElement.getQualifiedName().toString();
        String relativeName = getClassName(classElement) + InlineJavadocs.JAVADOC_PROPERTIES_SUFFIX;
        return processingEnv.getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, packageName, relativeName, classElement);
    }

    private static PackageElement getPackageElement(Element element) {
        if (element instanceof PackageElement) {
            return (PackageElement) element;
        }
        return getPackageElement(element.getEnclosingElement());
    }

    private static String getClassName(TypeElement typeElement) {
        // we can't take the simple name if we want to return names like EnclosingClass$NestedClass
        String typeName = typeElement.getQualifiedName().toString();
        String packageName = getPackageElement(typeElement).getQualifiedName().toString();

        if (!packageName.isEmpty()) {
            typeName = typeName.substring(packageName.length() + 1);
        }

        // for nested classes
        typeName = typeName.replace(".", "$");

        return typeName;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}

