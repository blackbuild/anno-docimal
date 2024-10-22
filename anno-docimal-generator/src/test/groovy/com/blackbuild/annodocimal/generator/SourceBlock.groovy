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
/**
 * Simple helper class to split source code in to blocks (not tokens).
 * Blocks are method field or class definitions including their java docs and inner classes.
 * This is by no ways complete and only works for a very limited set of cases.
 */
class SourceBlock {
    // Groovy 3+ adds additional annotations and their imports
    public static final ArrayList<String> GROOVY3_IGNORED_IMPORTS = ["java.lang.Object",
                                                                     "groovy.lang.MetaClass",
                                                                     "groovy.transform.Generated",
                                                                     "groovy.transform.Internal",
                                                                     "java.beans.Transient"]
    List<String> javaDocLines
    String text
    String packageName
    List<String> imports
    List<SourceBlock> innerBlocks
    List<String> annotations
    SourceBlock parent
    int indent

    static SourceBlock fromtext(String source) {
        def lines = source.readLines()
        SourceBlock rootBlock = new SourceBlock()
        SourceBlock currentBlock = rootBlock

        lines.each { line ->
            currentBlock = currentBlock.addLine(line)
        }

        rootBlock.cleanEmptyAndIgnoredBlocks()
        return rootBlock
    }

    String strip(CharSequence... strips) {
        def result = text
        strips.each { result = result - it }
        return result.trim()
    }

    SourceBlock addLine(String line) {
        if (line.isBlank()) return this
        def trimmedLine = line.trim()

        if (line.startsWith("package ")) {
            packageName = line[8..-2]
            return this
        }

        if (line.startsWith("import ")) {
            if (imports == null) imports = []
            def importPackage = line[7..-2]
            if (!GROOVY3_IGNORED_IMPORTS.contains(importPackage))
                imports.add(importPackage)
            return this
        }

        if (trimmedLine.startsWith("/**") || trimmedLine.startsWith("* ") || trimmedLine.startsWith("*/")) {
            addJavaDoc(line)
            return this
        }
        if (trimmedLine.startsWith("@")) {
            addAnnotation(trimmedLine)
            return this
        }
        if (trimmedLine.endsWith("{")) {
            setText line.substring(0, line.length() - 1).stripTrailing()
            return newInnerBlock()
        }
        if (trimmedLine.endsWith("{}")) {
            setText line.substring(0, line.length() - 2).stripTrailing()
            return parent
        }
        if (trimmedLine == "}") {
            if (parent.parent)
                return parent.parent.newInnerBlock()
            return parent
        }
        if (trimmedLine.endsWith(";")) {
            setText  line.substring(0, line.length() - 1).stripTrailing()
            return parent
        }
        setText line
        return this
    }

    private SourceBlock newInnerBlock() {
        def innerBlock = new SourceBlock(parent: this)
        if ((innerBlocks) == null) innerBlocks = []
        innerBlocks.add(innerBlock)
        innerBlock
    }

    void setText(String text) {
        assert !this.text : "Text already set: ${this.text}, new text: $text"
        indent = text.takeWhile { it == ' ' }.size()
        this.text = text.trim()
    }

    void addJavaDoc(String line) {
        def trim = line.trim()
        if (trim.startsWith("/**")) {
            if (javaDocLines) throw new IllegalStateException("duplicate javadoc block")
            if (trim.endsWith("*/"))
                javaDocLines = [trim.substring(3, line.length() - 2)]
            else if (trim.size() > 3)
                javaDocLines = [trim.substring(3)]
            else javaDocLines = []
        } else if (trim.endsWith("*/")) {
            if (trim.size() > 2)
                javaDocLines.add(trim.substring(0, line.length() - 2))
        } else {
            if (trim.startsWith("* "))
                javaDocLines.add(trim.substring(2))
            else
                javaDocLines.add(trim)
        }
    }

    String getJavaDoc() {
        return javaDocLines?.join("\n")
    }

    List<SourceBlock> allBlocks() {
        List<SourceBlock> result = []
        result.add(this)
        if (innerBlocks) {
            innerBlocks.each { innerBlock ->
                result.addAll(innerBlock.allBlocks())
            }
        }
        return result
    }

    void cleanEmptyAndIgnoredBlocks() {
        if (innerBlocks) {
            innerBlocks.each { it.cleanEmptyAndIgnoredBlocks() }
            innerBlocks.removeAll {
                // Groovy 3+ adds additional methods to classes, we ignore the as well as empty blocks
                !it.text || it.text.contains(" setMetaClass(") || it.text.contains(" getMetaClass()")
            }
        }
    }

    String getType() {
        if (!text) return "empty"
        if (text.contains("class ")) return parent ? "inner class" : "class"
        if (text.contains("interface ")) return "interface"
        if (text.contains("enum ")) return "enum"
        if (text.contains("(")) {
            def firstPart = text.trim().tokenize("/").first().tokenize()
            return firstPart.size() == 3 ? "method" : "constructor"
        }
        return "unknown"
    }

    SourceBlock getBlock(String name) {
        return innerBlocks.find { it.text == name }
    }

    SourceBlock getOneOf(String pattern, String... replacements) {
        return replacements.findResult {
            getBlock(pattern.replace("*", it))
        } as SourceBlock
    }

    @Override
    String toString() {
        if (innerBlocks)
            return "$type: $text $innerBlocks"
        else
            return "$type: $text"
    }

    void addAnnotation(String annotation) {
        if (annotations == null) annotations = []
        annotations.add(annotation)
    }
}
