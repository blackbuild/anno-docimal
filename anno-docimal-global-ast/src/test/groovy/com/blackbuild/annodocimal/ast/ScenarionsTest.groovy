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
package com.blackbuild.annodocimal.ast

import groovy.io.FileType
import org.codehaus.groovy.control.CompilationUnit

class ScenarionsTest extends AbstractGlobalAstTest {

    def compileDirectory(File folder) {
        CompilationUnit cu = new CompilationUnit(compilerConfiguration, null, loader)
        folder.eachFileRecurse (FileType.FILES) { file ->
            if(file.name.endsWith('.groovy')) {
                cu.addSource(file)
            }
        }
        cu.compile()
    }

    def "test compilation from folder"() {
        given:
        File sourceFolder = new File("src/test/scenarios/multifile")

        when:
        compileDirectory(sourceFolder)

        then:
        noExceptionThrown()
    }
}