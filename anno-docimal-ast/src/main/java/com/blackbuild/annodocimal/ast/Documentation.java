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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An immutable, normalized documentation model for transformation authors.
 * Instances preserve authored block and generic-tag order and expose immutable collections. Use
 * {@link #toBuilder()} to rewrite a value and {@link #render()} to produce normalized carrier content.
 */
public final class Documentation {

    private static final Documentation EMPTY = new Documentation(null, List.of(), Map.of(), null, Map.of(), List.of(), Map.of());
    private static final String DOCUMENTATION = "documentation";
    private static final String UNKNOWN_KEY = "<unknown>";
    private static final String PARAGRAPH_OPEN = "<p>";
    private static final String PARAGRAPH_CLOSE = "</p>";
    private static final String CODE_BLOCK_OPEN = "<pre>";
    private static final String CODE_BLOCK_CLOSE = "</pre>";
    private static final String PARAMETER_FRAGMENT = "param:";
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
        this.summary = summary;
        this.blocks = List.copyOf(blocks);
        this.parameters = immutableMap(parameters);
        this.returnDescription = returnDescription;
        this.exceptions = immutableMap(exceptions);
        this.tags = List.copyOf(tags);
        this.templateValues = immutableMap(templateValues);
    }

    /**
     * Returns the shared empty documentation value.
     *
     * @return empty documentation
     */
    public static Documentation empty() {
        return EMPTY;
    }

    /**
     * Creates an empty mutable builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parses normalized documentation content. Parsing consumes no template metadata implicitly.
     *
     * @param text normalized documentation content; blank text produces {@link #empty()}
     * @return its normalized semantic model
     * @throws NullPointerException when {@code text} is {@code null}
     */
    public static Documentation parse(String text) {
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) return empty();

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').strip();
        Builder builder = builder();
        List<String> lines = List.of(normalized.split("\n", -1));
        int index = parseBody(lines, builder);
        parseTags(lines, index, builder);
        return builder.build();
    }

    private static int parseBody(List<String> lines, Builder builder) {
        int index = parseSummary(lines, builder);
        while (index < lines.size() && !lines.get(index).startsWith("@")) {
            index = parseBodyItem(lines, index, builder);
        }
        return index;
    }

    private static int parseSummary(List<String> lines, Builder builder) {
        StringBuilder summary = new StringBuilder();
        int index = 0;
        while (index < lines.size()) {
            String line = lines.get(index);
            if (line.startsWith("@") || startsDelimitedBlock(line, PARAGRAPH_OPEN) || startsDelimitedBlock(line, CODE_BLOCK_OPEN)) break;
            if (!line.isBlank()) {
                if (!summary.isEmpty()) summary.append('\n');
                summary.append(line.strip());
            }
            index++;
        }
        if (!summary.isEmpty()) builder.summary(summary.toString());
        return index;
    }

    private static int parseBodyItem(List<String> lines, int index, Builder builder) {
        String line = lines.get(index);
        if (line.isBlank()) return index + 1;
        if (startsDelimitedBlock(line, PARAGRAPH_OPEN)) {
            return parseDelimitedBlock(lines, index, PARAGRAPH_OPEN, PARAGRAPH_CLOSE, false, builder);
        }
        if (startsDelimitedBlock(line, CODE_BLOCK_OPEN)) {
            return parseDelimitedBlock(lines, index, CODE_BLOCK_OPEN, CODE_BLOCK_CLOSE, true, builder);
        }
        return parseParagraph(lines, index, builder);
    }

    private static boolean startsDelimitedBlock(String line, String opening) {
        return line.equals(opening) || line.startsWith(opening);
    }

    private static int parseParagraph(List<String> lines, int index, Builder builder) {
        StringBuilder paragraph = new StringBuilder();
        while (index < lines.size() && !lines.get(index).isBlank() && !lines.get(index).startsWith("@")) {
            if (!paragraph.isEmpty()) paragraph.append('\n');
            paragraph.append(lines.get(index).strip());
            index++;
        }
        builder.paragraph(paragraph.toString());
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
                if (!content.isEmpty()) content.append('\n');
                content.append(line, 0, closingIndex);
                index++;
                break;
            }
            if (!content.isEmpty()) content.append('\n');
            content.append(line);
            index++;
        }
        String value = content.toString().strip();
        if (code) builder.codeBlock(value);
        else builder.paragraph(value);
        return index;
    }

    private static void parseTags(List<String> lines, int index, Builder builder) {
        String currentName = null;
        StringBuilder currentValue = null;
        for (; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.startsWith("@") && line.length() > 1 && !Character.isWhitespace(line.charAt(1))) {
                addTag(builder, currentName, currentValue);
                TagParts tag = splitTag(line.substring(1));
                currentName = tag.name();
                currentValue = new StringBuilder(tag.value());
            } else if (currentValue != null && !line.isBlank()) {
                currentValue.append(' ').append(line.strip());
            }
        }
        addTag(builder, currentName, currentValue);
    }

    private static void addTag(Builder builder, String name, StringBuilder value) {
        if (name == null) return;
        String description = value == null ? "" : value.toString();
        TagParts namedTag = splitTag(description);
        if (name.equals("param") && !namedTag.name().isEmpty()) builder.param(namedTag.name(), namedTag.value());
        else if (name.equals("return")) builder.returns(description);
        else if ((name.equals("throws") || name.equals("exception")) && !namedTag.name().isEmpty()) builder.throwsException(namedTag.name(), namedTag.value());
        else builder.tag(name, description);
    }

    private static TagParts splitTag(String text) {
        String value = text.strip();
        int separator = firstWhitespace(value);
        return separator < 0 ? new TagParts(value, "") : new TagParts(value.substring(0, separator), value.substring(separator).strip());
    }

    private static int firstWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) return index;
        }
        return -1;
    }

    /**
     * Returns the optional documentation summary.
     *
     * @return the summary, or an empty optional when absent
     */
    public Optional<String> getSummary() {
        return Optional.ofNullable(summary);
    }

    /**
     * Returns authored prose and code blocks in order.
     *
     * @return an immutable block list
     */
    public List<Block> getBlocks() {
        return blocks;
    }

    /**
     * Returns parameter descriptions in authored insertion order.
     *
     * @return an immutable parameter map
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Returns the optional return description.
     *
     * @return the return description, or an empty optional when absent
     */
    public Optional<String> getReturnDescription() {
        return Optional.ofNullable(returnDescription);
    }

    /**
     * Returns exception descriptions in authored insertion order.
     *
     * @return an immutable exception map
     */
    public Map<String, String> getExceptions() {
        return exceptions;
    }

    /**
     * Returns repeatable generic tags in authored order.
     *
     * @return an immutable tag list
     */
    public List<Tag> getTags() {
        return tags;
    }

    /**
     * Returns named values used only by explicit template rendering.
     *
     * @return an immutable template-value map
     */
    public Map<String, String> getTemplateValues() {
        return templateValues;
    }

    /**
     * Tests whether the value has no semantic content category.
     *
     * @return {@code true} when all content categories are absent
     */
    public boolean isEmpty() {
        return summary == null && blocks.isEmpty() && parameters.isEmpty() && returnDescription == null && exceptions.isEmpty() && tags.isEmpty();
    }

    /**
     * Creates a mutable builder initialized from this value.
     *
     * @return an independent builder
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Renders normalized documentation without filtering or reordering parameter tags.
     *
     * @return normalized documentation content
     * @throws TemplateException when explicit template input is malformed or incomplete
     */
    public String render() {
        return renderForParameters(null, DOCUMENTATION);
    }

    /**
     * Renders normalized documentation for a final declaration signature. Parameter descriptions are filtered and
     * ordered according to {@code finalParameters}; conditional parameter fragments use the same names.
     *
     * @param finalParameters final parameter names in signature order
     * @return normalized documentation content
     * @throws NullPointerException when the collection or one of its elements is {@code null}
     * @throws TemplateException when explicit template input is malformed or incomplete
     */
    public String render(Collection<String> finalParameters) {
        return renderForParameters(requireParameterNames(finalParameters), DOCUMENTATION);
    }

    String renderForParameters(Collection<String> finalParameters, String target) {
        StringBuilder rendered = renderSections();
        appendTags(rendered, finalParameters);
        Collection<String> parametersForTemplates = finalParameters == null ? parameters.keySet() : finalParameters;
        return substitute(rendered.toString(), templateValues, parametersForTemplates, target);
    }

    private StringBuilder renderSections() {
        StringBuilder rendered = new StringBuilder();
        appendSection(rendered, summary);
        for (Block block : blocks) appendSection(rendered, block.render());
        return rendered;
    }

    private void appendTags(StringBuilder rendered, Collection<String> finalParameters) {
        boolean hasTags = !parameters.isEmpty() || returnDescription != null || !exceptions.isEmpty() || !tags.isEmpty();
        if (hasTags && !rendered.isEmpty()) rendered.append('\n');
        appendParameters(rendered, finalParameters);
        if (returnDescription != null) appendTag(rendered, "return", returnDescription);
        for (Map.Entry<String, String> exception : exceptions.entrySet()) appendTag(rendered, "throws " + exception.getKey(), exception.getValue());
        for (Tag tag : tags) appendTag(rendered, tag.getName(), tag.getValue());
    }

    private void appendParameters(StringBuilder rendered, Collection<String> finalParameters) {
        if (finalParameters == null) {
            parameters.forEach((name, description) -> appendTag(rendered, "param " + name, description));
            return;
        }
        for (String parameter : finalParameters) {
            String description = parameters.get(parameter);
            if (description != null) appendTag(rendered, "param " + parameter, description);
        }
    }

    private static void appendSection(StringBuilder rendered, String value) {
        if (value == null || value.isBlank()) return;
        if (!rendered.isEmpty()) rendered.append("\n\n");
        rendered.append(value);
    }

    private static void appendTag(StringBuilder rendered, String name, String value) {
        if (!rendered.isEmpty()) rendered.append('\n');
        rendered.append('@').append(name);
        if (!value.isEmpty()) rendered.append(' ').append(value);
    }

    private static String substitute(String input, Map<String, String> values, Collection<String> parameters, String target) {
        StringBuilder result = new StringBuilder();
        int position = 0;
        while (position < input.length()) {
            int opening = nextOpeningDelimiter(input, position, target);
            if (opening < 0) {
                result.append(input, position, input.length());
                break;
            }
            result.append(input, position, opening);
            int closing = closingDelimiter(input, opening + 2);
            if (closing < 0) throw templateFailure(target, malformedKey(input.substring(opening + 2)), "missing closing delimiter");
            appendSubstitution(result, input.substring(opening + 2, closing), values, parameters, target);
            position = closing + 2;
        }
        return result.toString();
    }

    private static int nextOpeningDelimiter(String input, int position, String target) {
        int opening = input.indexOf("{{", position);
        int unexpectedClosing = input.indexOf("}}", position);
        if (unexpectedClosing >= 0 && (opening < 0 || unexpectedClosing < opening)) {
            throw templateFailure(target, UNKNOWN_KEY, "unexpected closing delimiter");
        }
        return opening;
    }

    private static void appendSubstitution(StringBuilder result, String expression, Map<String, String> values, Collection<String> parameters, String target) {
        if (expression.startsWith(PARAMETER_FRAGMENT)) {
            appendParameterFragment(result, expression, parameters, target);
        } else {
            appendRequiredValue(result, expression, values, target);
        }
    }

    private static void appendParameterFragment(StringBuilder result, String expression, Collection<String> parameters, String target) {
        int question = expression.indexOf('?');
        String key = parameterFragmentKey(expression, question);
        if (question < 0 || key.isBlank() || question == expression.length() - 1 || !TEMPLATE_KEY.matcher(key).matches()) {
            throw templateFailure(target, key, "malformed parameter fragment");
        }
        if (parameters.contains(key)) result.append(expression.substring(question + 1));
    }

    private static String parameterFragmentKey(String expression, int question) {
        int prefixLength = PARAMETER_FRAGMENT.length();
        return question < 0 ? malformedKey(expression.substring(prefixLength)) : expression.substring(prefixLength, question);
    }

    private static void appendRequiredValue(StringBuilder result, String expression, Map<String, String> values, String target) {
        if (!TEMPLATE_KEY.matcher(expression).matches()) throw templateFailure(target, malformedKey(expression), "malformed placeholder");
        String value = values.get(expression);
        if (value == null) throw templateFailure(target, expression, "missing required value");
        result.append(value);
    }

    private static TemplateException templateFailure(String target, String key, String problem) {
        return new TemplateException("Template " + problem + " for key '" + key + "' while rendering " + target);
    }

    private static int closingDelimiter(String input, int start) {
        int depth = 0;
        int index = start;
        while (index < input.length() - 1) {
            if (input.startsWith("{{", index)) {
                depth++;
                index += 2;
            } else if (input.startsWith("}}", index)) {
                if (depth == 0) return index;
                depth--;
                index += 2;
            } else {
                index++;
            }
        }
        return -1;
    }

    private static String malformedKey(String candidate) {
        String value = candidate.strip();
        int delimiter = value.indexOf('?');
        if (delimiter >= 0) return value.substring(0, delimiter);
        return value.isEmpty() ? UNKNOWN_KEY : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private record TagParts(String name, String value) {
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

    @Override
    public String toString() {
        return "Documentation{" +
                "summary=" + summary +
                ", blocks=" + blocks +
                ", parameters=" + parameters +
                ", returnDescription=" + returnDescription +
                ", exceptions=" + exceptions +
                ", tags=" + tags +
                ", templateValues=" + templateValues +
                '}';
    }

    /** A first-class authored prose or code block. */
    public static final class Block {
        private final String text;
        private final boolean code;

        private Block(String text, boolean code) {
            this.text = requireContent(text, code ? "code block" : "paragraph");
            this.code = code;
        }

        /**
         * Returns normalized block text.
         *
         * @return block text without rendering delimiters
         */
        public String getText() {
            return text;
        }

        /**
         * Tests whether this is a code block rather than prose.
         *
         * @return {@code true} for code
         */
        public boolean isCode() {
            return code;
        }

        private String render() {
            return code ? CODE_BLOCK_OPEN + text + CODE_BLOCK_CLOSE : PARAGRAPH_OPEN + text + PARAGRAPH_CLOSE;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Block that && code == that.code && text.equals(that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, code);
        }

        @Override
        public String toString() {
            return "Block{" + "text='" + text + '\'' + ", code=" + code + '}';
        }
    }

    /** A generic, ordered named documentation tag. */
    public static final class Tag {
        private final String name;
        private final String value;

        private Tag(String name, String value) {
            this.name = requireName(name, "tag");
            this.value = Objects.requireNonNull(value, "value");
        }

        /**
         * Returns the tag name without the leading {@code @}.
         *
         * @return tag name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the tag value, which may be blank.
         *
         * @return tag value
         */
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

        @Override
        public String toString() {
            return "Tag{" + "name='" + name + '\'' + ", value='" + value + '\'' + '}';
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

        /**
         * Creates an author-owned textual reference without a label.
         *
         * @param target textual Javadoc target
         * @return the reference
         * @throws NullPointerException when {@code target} is {@code null}
         * @throws IllegalArgumentException when {@code target} is blank
         */
        public static Link text(String target) {
            return new Link(target, null);
        }

        /**
         * Creates an author-owned textual reference with a label.
         *
         * @param target textual Javadoc target
         * @param label display label; blank text is treated as absent
         * @return the reference
         * @throws NullPointerException when either argument is {@code null}
         * @throws IllegalArgumentException when {@code target} is blank
         */
        public static Link text(String target, String label) {
            return new Link(target, Objects.requireNonNull(label, "label"));
        }

        /**
         * Returns the author-owned textual target.
         *
         * @return textual target
         */
        public String getTarget() {
            return target;
        }

        /**
         * Returns the optional display label.
         *
         * @return label, or an empty optional when absent
         */
        public Optional<String> getLabel() {
            return Optional.ofNullable(label);
        }

        /**
         * Renders this reference as an inline Javadoc link.
         *
         * @return canonical inline-link text
         */
        public String inline() {
            return "{@link " + target + (label == null ? "" : " " + label) + "}";
        }

        private String seeValue() {
            return target + (label == null ? "" : " " + label);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Link that && target.equals(that.target) && Objects.equals(label, that.label);
        }

        @Override
        public int hashCode() {
            return Objects.hash(target, label);
        }

        @Override
        public String toString() {
            return "Link{" + "target='" + target + '\'' + ", label='" + label + '\'' + '}';
        }
    }

    /** Failure to render a malformed or incomplete template. */
    public static final class TemplateException extends IllegalArgumentException {
        private TemplateException(String message) {
            super(message);
        }
    }

    /**
     * A mutable, non-thread-safe builder whose {@link #build()} results are independent immutable snapshots.
     */
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

        /**
         * Sets the summary. Blank text is a valid explicit value.
         *
         * @param value summary text
         * @return this builder
         */
        public Builder summary(String value) {
            summary = Objects.requireNonNull(value, "value").strip();
            return this;
        }

        /**
         * Removes the summary.
         *
         * @return this builder
         */
        public Builder clearSummary() {
            summary = null;
            return this;
        }

        /**
         * Appends a prose block.
         *
         * @param value non-blank prose
         * @return this builder
         */
        public Builder paragraph(String value) {
            blocks.add(new Block(value, false));
            return this;
        }

        /**
         * Appends a code block.
         *
         * @param value non-blank code
         * @return this builder
         */
        public Builder codeBlock(String value) {
            blocks.add(new Block(value, true));
            return this;
        }

        /**
         * Replaces all authored blocks, preserving the supplied order.
         *
         * @param values replacement blocks; an empty collection clears the category
         * @return this builder
         */
        public Builder replaceBlocks(Collection<Block> values) {
            blocks.clear();
            Objects.requireNonNull(values, "values").forEach(value -> blocks.add(Objects.requireNonNull(value, "block")));
            return this;
        }

        /**
         * Adds or replaces a parameter description without changing an existing parameter's position.
         *
         * @param name parameter name
         * @param description description, which may be blank
         * @return this builder
         */
        public Builder param(String name, String description) {
            parameters.put(requireName(name, "parameter"), Objects.requireNonNull(description, "description"));
            return this;
        }

        /**
         * Replaces all parameter descriptions in map iteration order.
         *
         * @param values replacement descriptions; an empty map clears the category
         * @return this builder
         */
        public Builder replaceParameters(Map<String, String> values) {
            parameters.clear();
            Objects.requireNonNull(values, "values").forEach(this::param);
            return this;
        }

        /**
         * Sets the return description. Blank text is a valid explicit value.
         *
         * @param description return description
         * @return this builder
         */
        public Builder returns(String description) {
            returnDescription = Objects.requireNonNull(description, "description");
            return this;
        }

        /**
         * Removes the return description.
         *
         * @return this builder
         */
        public Builder clearReturn() {
            returnDescription = null;
            return this;
        }

        /**
         * Adds or replaces an exception description without changing an existing exception's position.
         *
         * @param exception exception type name
         * @param description description, which may be blank
         * @return this builder
         */
        public Builder throwsException(String exception, String description) {
            exceptions.put(requireName(exception, "exception"), Objects.requireNonNull(description, "description"));
            return this;
        }

        /**
         * Replaces all exception descriptions in map iteration order.
         *
         * @param values replacement descriptions; an empty map clears the category
         * @return this builder
         */
        public Builder replaceExceptions(Map<String, String> values) {
            exceptions.clear();
            Objects.requireNonNull(values, "values").forEach(this::throwsException);
            return this;
        }

        /**
         * Appends a repeatable generic tag.
         *
         * @param name tag name without {@code @}
         * @param value tag value, which may be blank
         * @return this builder
         */
        public Builder tag(String name, String value) {
            tags.add(new Tag(name, value));
            return this;
        }

        /**
         * Appends a {@code deprecated} tag.
         *
         * @param value deprecation description, which may be blank
         * @return this builder
         */
        public Builder deprecated(String value) {
            return tag("deprecated", value);
        }

        /**
         * Appends a {@code see} tag from a first-class reference.
         *
         * @param link reference to append
         * @return this builder
         */
        public Builder see(Link link) {
            return tag("see", Objects.requireNonNull(link, "link").seeValue());
        }

        /**
         * Appends a {@code see} tag from an author-owned textual target.
         *
         * @param target textual target
         * @return this builder
         */
        public Builder seeText(String target) {
            return see(Link.text(target));
        }

        /**
         * Replaces all generic tags, preserving the supplied order.
         *
         * @param values replacement tags; an empty collection clears the category
         * @return this builder
         */
        public Builder replaceTags(Collection<Tag> values) {
            tags.clear();
            Objects.requireNonNull(values, "values").forEach(value -> tags.add(Objects.requireNonNull(value, "tag")));
            return this;
        }

        /**
         * Adds or replaces one named template value.
         *
         * @param key placeholder key
         * @param value literal single-pass replacement
         * @return this builder
         * @throws TemplateException when the key is malformed or the value is {@code null}
         */
        public Builder template(String key, String value) {
            return templateValue(key, value);
        }

        /**
         * Adds or replaces named template values. Raw or dynamic calls are validated at runtime so invalid values retain
         * target and key context.
         *
         * @param values placeholder values
         * @return this builder
         * @throws NullPointerException when {@code values} is {@code null}
         * @throws TemplateException when a key is malformed or a value is null or not a string
         */
        public Builder templateValues(Map<String, String> values) {
            Objects.requireNonNull(values, "values");
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                templateValue(entry.getKey(), entry.getValue());
            }
            return this;
        }

        private Builder templateValue(Object key, Object value) {
            String name = requireTemplateKeyValue(key);
            if (!(value instanceof String)) throw templateFailure(DOCUMENTATION, name, value == null ? "null value" : "non-string value");
            templateValues.put(name, (String) value);
            return this;
        }

        /**
         * Appends fallback documentation. Existing singular and named content wins; blocks and repeatable tags append.
         *
         * @param documentation fallback documentation
         * @return this builder
         */
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

        /**
         * Replaces the complete builder state with an immutable value.
         *
         * @param documentation replacement documentation
         * @return this builder
         */
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

        /**
         * Removes parameter descriptions that are not present in a final declaration signature.
         *
         * @param finalParameters retained parameter names
         * @return this builder
         */
        public Builder filterParameters(Collection<String> finalParameters) {
            Set<String> names = new LinkedHashSet<>(requireParameterNames(finalParameters));
            parameters.keySet().removeIf(name -> !names.contains(name));
            return this;
        }

        /**
         * Creates an immutable snapshot independent of subsequent builder changes.
         *
         * @return immutable documentation
         */
        public Documentation build() {
            return summary == null && blocks.isEmpty() && parameters.isEmpty() && returnDescription == null && exceptions.isEmpty() && tags.isEmpty()
                    ? empty() : new Documentation(summary, blocks, parameters, returnDescription, exceptions, tags, templateValues);
        }
    }

    private static String requireContent(String value, String kind) {
        Objects.requireNonNull(value, kind);
        if (value.isBlank()) throw new IllegalArgumentException(kind + " must not be blank");
        return value.strip();
    }

    private static String requireName(String value, String kind) {
        Objects.requireNonNull(value, kind + " name");
        if (value.isBlank()) throw new IllegalArgumentException(kind + " name must not be blank");
        return value.strip();
    }

    private static Collection<String> requireParameterNames(Collection<String> values) {
        Objects.requireNonNull(values, "values");
        values.forEach(value -> Objects.requireNonNull(value, "parameter name"));
        return values;
    }

    private static String requireTemplateKey(String key) {
        if (key == null || !TEMPLATE_KEY.matcher(key).matches()) throw templateFailure(DOCUMENTATION, key == null ? UNKNOWN_KEY : key, "malformed key");
        return key;
    }

    private static String requireTemplateKeyValue(Object key) {
        if (!(key instanceof String)) {
            throw templateFailure(DOCUMENTATION, key == null ? UNKNOWN_KEY : String.valueOf(key), "malformed key");
        }
        return requireTemplateKey((String) key);
    }
}
