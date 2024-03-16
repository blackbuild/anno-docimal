package com.blackbuild.annodocimal.generator
/**
 * Simple helper class to split source code in to blocks (not tokens).
 * Blocks are method field or class definitions including their java docs and inner classes.
 * This is by no ways complete and only works for a very limited set of cases.
 */
class SourceBlock {
    List<String> javaDocLines
    String text
    String packageName
    List<String> imports
    List<SourceBlock> innerBlocks
    SourceBlock parent
    int indent

    static SourceBlock fromtext(String source) {
        def lines = source.readLines()
        SourceBlock rootBlock = new SourceBlock()
        SourceBlock currentBlock = rootBlock

        lines.each { line ->
            currentBlock = currentBlock.addLine(line)
        }

        rootBlock.cleanEmptyBlocks()
        return rootBlock
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
            imports.add(line[7..-2])
            return this
        }

        if (trimmedLine.startsWith("/**") || trimmedLine.startsWith("* ") || trimmedLine.startsWith("*/")) {
            addJavaDoc(line)
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

    void cleanEmptyBlocks() {
        if (innerBlocks) {
            innerBlocks.each { it.cleanEmptyBlocks() }
            innerBlocks = innerBlocks.findAll { it.text }
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

    @Override
    String toString() {
        if (innerBlocks)
            return "$type: $text $innerBlocks"
        else
            return "$type: $text"
    }
}
