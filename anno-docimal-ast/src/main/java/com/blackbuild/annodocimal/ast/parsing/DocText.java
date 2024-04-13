package com.blackbuild.annodocimal.ast.parsing;

import org.jetbrains.annotations.NotNull;

import java.text.BreakIterator;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Container for parsed javadoc comments. Useful to prevent additional parsing in later phases.
 */
public class DocText {

    protected final String rawText;
    protected final String title;
    protected final String body;
    protected final Map<String, List<String>> tags;

    protected DocText(String rawText) {
        this.rawText = rawText;
        this.title = calculateFirstSentence();
        this.body = calculateBody();
        this.tags = calculateTags();
    }

    protected @NotNull String calculateFirstSentence() {
        String text = rawText;
        // form SimpleGroovyDoc
        text = text.replaceFirst("(?ms)<p>.*", "").trim();
        // assume completely blank line signifies end of sentence
        text = text.replaceFirst("(?ms)\\n\\s*\\n.*", "").trim();
        // assume @tag signifies end of sentence
        text = text.replaceFirst("(?ms)\\n\\s*@([a-z]+).*", "").trim();
        // Comment Summary using first sentence (Locale sensitive)
        BreakIterator boundary = BreakIterator.getSentenceInstance(Locale.getDefault());
        boundary.setText(text);
        int start = boundary.first();
        int end = boundary.next();
        if (start > -1 && end > -1) {
            // need to abbreviate this comment for the summary
            text = text.substring(start, end);
        }
        return text.trim();
    }

    protected String calculateBody() {
        String text = rawText;
        // drop first sentence
        int index = text.indexOf(title);
        if (index != -1)
            text = text.substring(index + title.length()).trim();

        if (text.trim().startsWith("@"))
            return "";

        // drop tags
        text = text.replaceFirst("(?ms)\\n\\s*@([a-z]+).*", "");
        return text.trim();
    }

    protected Map<String, List<String>> calculateTags() {
        if (rawText == null || !rawText.contains("@")) return Collections.emptyMap();

        Map<String, List<String>> result = new java.util.LinkedHashMap<>();

        StringBuilder tagText = null;
        String tagName = null;
        for (String line : rawText.split("\\n")) {
            line = line.trim();
            if (line.startsWith("@")) {
                if (tagText != null) {
                    result.computeIfAbsent(tagName, k -> new ArrayList<>()).add(tagText.toString().trim().replaceAll("\\s+", " "));
                }

                tagName = line.substring(1).split("\\s+")[0];
                tagText = new StringBuilder(line.length() > tagName.length() + 2 ? line.substring(tagName.length() + 2) : "").append(" ");
                continue;
            }
            if (tagText != null) {
                tagText.append(line).append(" ");
            }
        }
        if (tagName != null)
            result.computeIfAbsent(tagName, k -> new ArrayList<>()).add(tagText.toString().trim().replaceAll("\\s+", " "));

        result.replaceAll((k, v) -> Collections.unmodifiableList(v));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns the parsed tags. The key is the name of the tag, the value is a list of all values for this tag,
     * trimmed and concatenated with a single space.
     * @return the parsed tags
     */
    public Map<String, List<String>> getTags() {
        return tags;
    }

    /**
     * Returns the raw text of the javadoc comment.
     * @return the raw text of the javadoc comment
     */
    public String getRawText() {
        return rawText;
    }

    /**
     * Returns the first sentence of the javadoc comment. According to Javadoc conventions, the title should end with a period.
     * If the text does not contain a period, the first sentence is assumed to end at the first double line break,
     * the first occurrence of "&lt;p&gt;", or the first occurrence of "{@literal @tag}".
     * @return the first sentence of the javadoc comment
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the body of the javadoc comment. The body is the text after the first sentence and before the first tag.
     * @return the body of the javadoc comment
     * @see #getTitle()
     */
    public String getBody() {
        return body;
    }

    /**
     * Returns the values of the given tag. The values are trimmed and concatenated with a single space.
     * @param name the name of the tag
     * @return the values of the tag. Can be empty but never null.
     */
    public List<String> getTags(String name) {
        return tags.getOrDefault(name, Collections.emptyList());
    }

    /**
     * Returns the single value of the given tag. If the value is present multiple times, the first value is returned.
     * @param name the name of the tag
     * @return the first value of the tag.
     */
    public Optional<String> getTag(String name) {
        return tags.getOrDefault(name, Collections.emptyList()).stream().findFirst();
    }

    /**
     * Returns the value of the given name tag (like the text for a specified param).
     * <p>
     *     '{@literal @}param name first name of the person' with 'getNamedTag("param", "name")' will return "first name of the person",
     *     'getNamedTag("param", "surname")' will return an empty optional.
     * </p>
     * <p>
     *     The result can be an empty string if the classifier is present but the value is empty. (e.g. '{@literal @}param name')
     * </p>
     * @param tagName the name of the tag
     * @param name the classifier of the tag
     * @return the first value of the tag.
     */
    public Optional<String> getNamedTag(String tagName, String name) {
        Pattern startsWithName = Pattern.compile("^" + name + "(?:\\s+(.*))?$");
        return tags.getOrDefault(tagName, Collections.emptyList()).stream()
                .map(startsWithName::matcher)
                .filter(Matcher::matches)
                .map(m -> m.group(1))
                .map(s -> s == null ? "" : s.trim())
                .findFirst();
    }
}
