/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Stephan Pauxberger
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An immutable, normalized documentation model for transformation authors.
 */
public final class Documentation {

    private static final Documentation EMPTY = new Documentation(null, List.of(), Map.of(), null, Map.of(), List.of(), Map.of());
    private static final Pattern TAG = Pattern.compile("@(\\S+)(?:\\s+(.*))?");
    private static final Pattern NAMED_TAG = Pattern.compile("(\\S+)(?:\\s+(.*))?");
    private static final Pattern TEMPLATE_KEY = Pattern.compile("[A-Za-z][A-Za-z0-9_.-]*");

    private final String summary;
    private final List<Block> blocks;
    private final Map<String, String> parameters;
    private final String returnDescription;
    private final Map<String, String> exceptions;
    private final List<Tag> tags;
    private final Map<String, String> templateValues;

    private Documentation(String summary, List<Block> blocks, Map<String, String> parameters, String returnDescription,
                          Map<String, String> exceptions, List<Tag> tags, Map<String, String> templateValues) {
        this.summary = blankToNull(summary);
        this.blocks = List.copyOf(blocks);
        this.parameters = immutableMap(parameters);
        this.returnDescription = returnDescription;
        this.exceptions = immutableMap(exceptions);
        this.tags = List.copyOf(tags);
        this.templateValues = immutableMap(templateValues);
    }

    public static Documentation empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parses normalized documentation content. Parsing consumes no template metadata implicitly.
     *
     * @param text normalized documentation content
     * @return its normalized semantic model
     */
    public static Documentation parse(String text) {
        if (text == null || text.isBlank()) return empty();

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').strip();
        Builder builder = builder();
        List<String> lines = List.of(normalized.split("\n", -1));
        int index = parseBody(lines, builder);
        parseTags(lines, index, builder);
        return builder.build();
    }

    private static int parseBody(List<String> lines, Builder builder) {
        StringBuilder summary = new StringBuilder();
        int index = 0;
        while (index < lines.size()) {
            String line = lines.get(index);
            if (line.startsWith("@") || line.equals("<p>") || line.startsWith("<p>") || line.equals("<pre>") || line.startsWith("<pre>")) break;
            if (!line.isBlank()) {
                if (summary.length() > 0) summary.append('\n');
                summary.append(line.strip());
            }
            index++;
        }
        if (summary.length() > 0) builder.summary(summary.toString());

        while (index < lines.size() && !lines.get(index).startsWith("@")) {
            if (lines.get(index).isBlank()) {
                index++;
            } else if (lines.get(index).equals("<p>") || lines.get(index).startsWith("<p>")) {
                index = parseDelimitedBlock(lines, index, "<p>", "</p>", false, builder);
            } else if (lines.get(index).equals("<pre>") || lines.get(index).startsWith("<pre>")) {
                index = parseDelimitedBlock(lines, index, "<pre>", "</pre>", true, builder);
            } else {
                StringBuilder paragraph = new StringBuilder();
                while (index < lines.size() && !lines.get(index).isBlank() && !lines.get(index).startsWith("@")) {
                    if (paragraph.length() > 0) paragraph.append('\n');
                    paragraph.append(lines.get(index).strip());
                    index++;
                }
                builder.paragraph(paragraph.toString());
            }
        }
        return index;
    }

    private static int parseDelimitedBlock(List<String> lines, int index, String opening, String closing, boolean code, Builder builder) {
        String firstLine = lines.get(index);
        String firstContent = firstLine.substring(opening.length());
        int closingInFirstLine = firstContent.indexOf(closing);
        if (closingInFirstLine >= 0) {
            String value = firstContent.substring(0, closingInFirstLine).strip();
            if (code) builder.codeBlock(value);
            else builder.paragraph(value);
            return index + 1;
        }
        StringBuilder content = new StringBuilder(firstContent);
        index++;
        while (index < lines.size()) {
            String line = lines.get(index);
            int closingIndex = line.indexOf(closing);
            if (closingIndex >= 0) {
                if (content.length() > 0) content.append('\n');
                content.append(line, 0, closingIndex);
                index++;
                break;
            }
            if (content.length() > 0) content.append('\n');
            content.append(line);
            index++;
        }
        String value = code ? content.toString().strip() : content.toString().strip();
        if (code) builder.codeBlock(value);
        else builder.paragraph(value);
        return index;
    }

    private static void parseTags(List<String> lines, int index, Builder builder) {
        String currentName = null;
        StringBuilder currentValue = null;
        for (; index < lines.size(); index++) {
            Matcher matcher = TAG.matcher(lines.get(index));
            if (matcher.matches()) {
                addTag(builder, currentName, currentValue);
                currentName = matcher.group(1);
                currentValue = new StringBuilder(matcher.group(2) == null ? "" : matcher.group(2));
            } else if (currentValue != null && !lines.get(index).isBlank()) {
                currentValue.append(' ').append(lines.get(index).strip());
            }
        }
        addTag(builder, currentName, currentValue);
    }

    private static void addTag(Builder builder, String name, StringBuilder value) {
        if (name == null) return;
        String description = value == null ? "" : value.toString();
        Matcher namedTag = NAMED_TAG.matcher(description);
        if (name.equals("param") && namedTag.matches()) builder.param(namedTag.group(1), nullToEmpty(namedTag.group(2)));
        else if (name.equals("return")) builder.returns(description);
        else if ((name.equals("throws") || name.equals("exception")) && namedTag.matches()) builder.throwsException(namedTag.group(1), nullToEmpty(namedTag.group(2)));
        else builder.tag(name, description);
    }

    public String getSummary() {
        return summary;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getReturnDescription() {
        return returnDescription;
    }

    public Map<String, String> getExceptions() {
        return exceptions;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public Map<String, String> getTemplateValues() {
        return templateValues;
    }

    public boolean isEmpty() {
        return summary == null && blocks.isEmpty() && parameters.isEmpty() && returnDescription == null && exceptions.isEmpty() && tags.isEmpty();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public String render() {
        return renderForParameters(null, "documentation");
    }

    public String render(Collection<String> finalParameters) {
        return renderForParameters(finalParameters, "documentation");
    }

    String renderForParameters(Collection<String> finalParameters, String target) {
        StringBuilder rendered = new StringBuilder();
        appendSection(rendered, summary);
        for (Block block : blocks) appendSection(rendered, block.render());

        boolean hasTags = !parameters.isEmpty() || returnDescription != null || !exceptions.isEmpty() || !tags.isEmpty();
        if (hasTags && rendered.length() > 0) rendered.append('\n');
        if (finalParameters == null) {
            for (Map.Entry<String, String> parameter : parameters.entrySet()) appendTag(rendered, "param " + parameter.getKey(), parameter.getValue());
        } else {
            for (String parameter : finalParameters) {
                String description = parameters.get(parameter);
                if (description != null) appendTag(rendered, "param " + parameter, description);
            }
        }
        if (returnDescription != null) appendTag(rendered, "return", returnDescription);
        for (Map.Entry<String, String> exception : exceptions.entrySet()) appendTag(rendered, "throws " + exception.getKey(), exception.getValue());
        for (Tag tag : tags) appendTag(rendered, tag.getName(), tag.getValue());
        return substitute(rendered.toString(), templateValues, finalParameters == null ? parameters.keySet() : finalParameters, target);
    }

    private static void appendSection(StringBuilder rendered, String value) {
        if (value == null || value.isBlank()) return;
        if (rendered.length() > 0) rendered.append("\n\n");
        rendered.append(value);
    }

    private static void appendTag(StringBuilder rendered, String name, String value) {
        if (rendered.length() > 0) rendered.append('\n');
        rendered.append('@').append(name);
        if (!value.isEmpty()) rendered.append(' ').append(value);
    }

    private static String substitute(String input, Map<String, String> values, Collection<String> parameters, String target) {
        StringBuilder result = new StringBuilder();
        int position = 0;
        while (position < input.length()) {
            int opening = input.indexOf("{{", position);
            int unexpectedClosing = input.indexOf("}}", position);
            if (unexpectedClosing >= 0 && (opening < 0 || unexpectedClosing < opening)) throw templateFailure(target, "<unknown>", "unexpected closing delimiter");
            if (opening < 0) {
                result.append(input, position, input.length());
                break;
            }
            result.append(input, position, opening);
            int closing = closingDelimiter(input, opening + 2);
            if (closing < 0) throw templateFailure(target, malformedKey(input.substring(opening + 2)), "missing closing delimiter");
            String expression = input.substring(opening + 2, closing);
            if (expression.startsWith("param:")) {
                int question = expression.indexOf('?');
                String key = question < 0 ? malformedKey(expression.substring("param:".length())) : expression.substring("param:".length(), question);
                if (question < 0 || key.isBlank() || question == expression.length() - 1 || !TEMPLATE_KEY.matcher(key).matches()) {
                    throw templateFailure(target, key, "malformed parameter fragment");
                }
                if (parameters.contains(key)) result.append(expression.substring(question + 1));
            } else {
                if (!TEMPLATE_KEY.matcher(expression).matches()) throw templateFailure(target, malformedKey(expression), "malformed placeholder");
                String value = values.get(expression);
                if (value == null) throw templateFailure(target, expression, "missing required value");
                result.append(value);
            }
            position = closing + 2;
        }
        return result.toString();
    }

    private static TemplateException templateFailure(String target, String key, String problem) {
        return new TemplateException("Template " + problem + " for key '" + key + "' while rendering " + target);
    }

    private static int closingDelimiter(String input, int start) {
        int depth = 0;
        for (int index = start; index < input.length() - 1; index++) {
            if (input.startsWith("{{", index)) {
                depth++;
                index++;
            } else if (input.startsWith("}}", index)) {
                if (depth == 0) return index;
                depth--;
                index++;
            }
        }
        return -1;
    }

    private static String malformedKey(String candidate) {
        String value = candidate.strip();
        int delimiter = value.indexOf('?');
        return delimiter >= 0 ? value.substring(0, delimiter) : value.isEmpty() ? "<unknown>" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Map<String, String> immutableMap(Map<String, String> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Documentation that)) return false;
        return Objects.equals(summary, that.summary) && blocks.equals(that.blocks) && parameters.equals(that.parameters)
                && Objects.equals(returnDescription, that.returnDescription) && exceptions.equals(that.exceptions)
                && tags.equals(that.tags) && templateValues.equals(that.templateValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(summary, blocks, parameters, returnDescription, exceptions, tags, templateValues);
    }

    /** A first-class authored prose or code block. */
    public static final class Block {
        private final String text;
        private final boolean code;

        private Block(String text, boolean code) {
            this.text = requireContent(text, code ? "code block" : "paragraph");
            this.code = code;
        }

        public String getText() {
            return text;
        }

        public boolean isCode() {
            return code;
        }

        private String render() {
            return code ? "<pre>" + text + "</pre>" : "<p>" + text + "</p>";
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Block that && code == that.code && text.equals(that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, code);
        }
    }

    /** A generic, ordered named documentation tag. */
    public static final class Tag {
        private final String name;
        private final String value;

        private Tag(String name, String value) {
            this.name = requireName(name, "tag");
            this.value = nullToEmpty(value);
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Tag that && name.equals(that.name) && value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }
    }

    /** A first-class documentation reference that can render inline or as a {@code @see} tag. */
    public static final class Link {
        private final String target;
        private final String label;

        private Link(String target, String label) {
            this.target = requireContent(target, "link target");
            this.label = blankToNull(label);
        }

        public static Link text(String target) {
            return new Link(target, null);
        }

        public static Link text(String target, String label) {
            return new Link(target, label);
        }

        public String getTarget() {
            return target;
        }

        public String getLabel() {
            return label;
        }

        public String inline() {
            return "{@link " + target + (label == null ? "" : " " + label) + "}";
        }

        private String seeValue() {
            return target + (label == null ? "" : " " + label);
        }
    }

    /** Failure to render a malformed or incomplete template. */
    public static final class TemplateException extends IllegalArgumentException {
        private TemplateException(String message) {
            super(message);
        }
    }

    /** A mutable builder whose {@link #build()} results are independent immutable snapshots. */
    public static final class Builder {
        private String summary;
        private final List<Block> blocks = new ArrayList<>();
        private final Map<String, String> parameters = new LinkedHashMap<>();
        private String returnDescription;
        private final Map<String, String> exceptions = new LinkedHashMap<>();
        private final List<Tag> tags = new ArrayList<>();
        private final Map<String, String> templateValues = new LinkedHashMap<>();

        private Builder() {
        }

        private Builder(Documentation documentation) {
            summary = documentation.summary;
            blocks.addAll(documentation.blocks);
            parameters.putAll(documentation.parameters);
            returnDescription = documentation.returnDescription;
            exceptions.putAll(documentation.exceptions);
            tags.addAll(documentation.tags);
            templateValues.putAll(documentation.templateValues);
        }

        public Builder summary(String value) {
            summary = blankToNull(value);
            return this;
        }

        public Builder paragraph(String value) {
            blocks.add(new Block(value, false));
            return this;
        }

        public Builder appendParagraph(String value) {
            return paragraph(value);
        }

        public Builder codeBlock(String value) {
            blocks.add(new Block(value, true));
            return this;
        }

        public Builder replaceBlocks(Collection<Block> values) {
            blocks.clear();
            blocks.addAll(values);
            return this;
        }

        public Builder param(String name, String description) {
            parameters.put(requireName(name, "parameter"), nullToEmpty(description));
            return this;
        }

        public Builder replaceParameters(Map<String, String> values) {
            parameters.clear();
            values.forEach(this::param);
            return this;
        }

        public Builder returns(String description) {
            returnDescription = nullToEmpty(description);
            return this;
        }

        public Builder replaceReturn(String description) {
            return returns(description);
        }

        public Builder throwsException(String exception, String description) {
            exceptions.put(requireName(exception, "exception"), nullToEmpty(description));
            return this;
        }

        public Builder replaceExceptions(Map<String, String> values) {
            exceptions.clear();
            values.forEach(this::throwsException);
            return this;
        }

        public Builder tag(String name, String value) {
            tags.add(new Tag(name, value));
            return this;
        }

        public Builder deprecated(String value) {
            return tag("deprecated", value);
        }

        public Builder see(Link link) {
            return tag("see", Objects.requireNonNull(link, "link").seeValue());
        }

        public Builder seeText(String target) {
            return see(Link.text(target));
        }

        public Builder replaceTags(Collection<Tag> values) {
            tags.clear();
            tags.addAll(values);
            return this;
        }

        public Builder template(String key, String value) {
            return templateValue(key, value);
        }

        public Builder templateValues(Map<String, ?> values) {
            values.forEach(this::templateValue);
            return this;
        }

        private Builder templateValue(String key, Object value) {
            String name = requireTemplateKey(key);
            if (!(value instanceof String)) throw templateFailure("documentation", name, value == null ? "null value" : "non-string value");
            templateValues.put(name, (String) value);
            return this;
        }

        public Builder append(Documentation documentation) {
            Objects.requireNonNull(documentation, "documentation");
            if (summary == null) summary = documentation.summary;
            blocks.addAll(documentation.blocks);
            documentation.parameters.forEach(parameters::putIfAbsent);
            if (returnDescription == null) returnDescription = documentation.returnDescription;
            documentation.exceptions.forEach(exceptions::putIfAbsent);
            tags.addAll(documentation.tags);
            documentation.templateValues.forEach(templateValues::putIfAbsent);
            return this;
        }

        public Builder replace(Documentation documentation) {
            Objects.requireNonNull(documentation, "documentation");
            summary = documentation.summary;
            replaceBlocks(documentation.blocks);
            replaceParameters(documentation.parameters);
            returnDescription = documentation.returnDescription;
            replaceExceptions(documentation.exceptions);
            replaceTags(documentation.tags);
            templateValues.clear();
            templateValues.putAll(documentation.templateValues);
            return this;
        }

        public Builder filterParameters(Collection<String> finalParameters) {
            Set<String> names = new LinkedHashSet<>(finalParameters);
            parameters.keySet().removeIf(name -> !names.contains(name));
            return this;
        }

        public Documentation build() {
            return summary == null && blocks.isEmpty() && parameters.isEmpty() && returnDescription == null && exceptions.isEmpty() && tags.isEmpty()
                    ? empty() : new Documentation(summary, blocks, parameters, returnDescription, exceptions, tags, templateValues);
        }
    }

    private static String requireContent(String value, String kind) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(kind + " must not be blank");
        return value.strip();
    }

    private static String requireName(String value, String kind) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(kind + " name must not be blank");
        return value.strip();
    }

    private static String requireTemplateKey(String key) {
        if (key == null || !TEMPLATE_KEY.matcher(key).matches()) throw templateFailure("documentation", key == null ? "<unknown>" : key, "malformed key");
        return key;
    }
}
