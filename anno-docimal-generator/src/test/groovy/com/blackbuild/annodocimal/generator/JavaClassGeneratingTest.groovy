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
package com.blackbuild.annodocimal.generator


import com.google.testing.compile.Compilation
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

import javax.tools.JavaFileObject
import javax.tools.StandardLocation

import static com.google.testing.compile.Compiler.javac

abstract class JavaClassGeneratingTest extends Specification {

    @Rule TestName testName = new TestName()
    Compiler compiler = javac().withOptions("-parameters")
    Compilation compilation
    JavaFileObject file

    JavaFileObject compile(@Language("java") String code) {
        String className = (code =~ ~/class\s+(\w+)/)[0][1]
        String packageName = (code =~ ~/package\s+(\w+)/)[0][1]
        compilation = compiler.compile(JavaFileObjects.forSourceString("$packageName.$className", code))
        file = compilation.generatedFile(StandardLocation.CLASS_OUTPUT, packageName, className + ".class").get()
        return file
    }
}
