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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Converts real reflection elements to their JavaPoet Spec counterparts.
 */
public class SpecConverter {

    private final File packageDir;

    public SpecConverter(File packageDir) {
        this.packageDir = packageDir;
    }

    public JavaPoetClassVisitor readClass(String className, int innerClassModifiers) throws IOException {
        File classFile = new File(packageDir, className.replace('.', '/') + ".class");

        try (InputStream source = new FileInputStream(classFile)) {
            JavaPoetClassVisitor visitor = new JavaPoetClassVisitor(this, innerClassModifiers);
            ClassReader classReader = new ClassReader(source);
            classReader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
            return visitor;
        }
    }

    public static JavaFile toJavaFile(File classFile) throws IOException {
        JavaPoetClassVisitor outerType = new SpecConverter(classFile.getParentFile()).readClass(classFile.getName().replace(".class", ""), -1);
        return JavaFile.builder(outerType.getPackageName(), outerType.getType()).build();
    }


}
