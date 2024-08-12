package com.blackbuild.annodocimal.ast.parser;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class AbstractSourceExtractor implements SourceExtractor {
    protected String reformat(String rawComment) {
        if (rawComment == null) return null;

        if (rawComment.startsWith("/**"))
            rawComment = rawComment.substring(2);
        if (rawComment.endsWith("*/"))
            rawComment = rawComment.substring(0, rawComment.length() - 2);
        rawComment = rawComment.replaceAll("(?m)^\\s*\\*", "");

        if (rawComment.isBlank()) return null;

        String[] lines = rawComment.split("\n");
        int minIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            if (line.trim().isEmpty())
                continue;

            int indent = 0;
            while (indent < line.length() && Character.isWhitespace(line.charAt(indent)))
                indent++;

            minIndent = Math.min(minIndent, indent);
        }

        int finalMinIndent = minIndent;
        String joined = Arrays.stream(lines)
                .map(line -> line.length() >= finalMinIndent ? line.substring(finalMinIndent) : "")
                .collect(Collectors.joining("\n"));

        int start = 0;
        while (start < joined.length() && joined.charAt(start) == '\n')
            start++;

        int end = joined.length();
        while (end > 0 && Character.isWhitespace(joined.charAt(end - 1)))
            end--;

        return joined.substring(start, end);
    }
}
