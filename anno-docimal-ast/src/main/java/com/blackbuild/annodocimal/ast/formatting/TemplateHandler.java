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
package com.blackbuild.annodocimal.ast.formatting;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

/**
 * Handles the rendering of templates in a string.
 */
public class TemplateHandler {

    private TemplateHandler() {
        // Utility class
    }

    private static final Pattern PARAM_PATTERN = Pattern.compile("( ?)\\{\\{param:(\\w+)\\?([^}:]+)(?::([^}]+))?}}( ?)");
    private static final Pattern TEMPLATE_CONDITIONAL_PATTERN = Pattern.compile("( ?)\\{\\{(\\w+)\\?([^}:]+)(?::([^}]+))?}}( ?)");
    private static final Pattern TEMPLATE_VALUE_PATTERN = Pattern.compile("\\{\\{(\\w+)(?::([^}:]+))?}}");


    /**
     * Renders the given string by applying the given templates.
     * <p>
     *     Templates are placeholders in the text that can be replaced
     *     by the actual value when the text is rendered. Templates consist of two curly braces around the keywords, e.g.
     *     {@code {{text}}}.
     * </p>
     * <p>There are currently three types of replacements:</p>
     * <ul>
     *     <li>Basic templates: '{{[name]:[default]}}' or '{{[name]}}' These are replaced by a simple value. The key is the name of the template, the value is the replacement. These
     *     are taken from the given templateValues map. If the key is not found, the value will be replaced with the default value or the key name if no default is given.</li>
     *     <li>Param templates: '{{param:[name]?[if-case]}}' or '{{param:[name]?[if-case]:[else-case]}}'. These are replaced by the 'if-case' value if the given
     *     parameter is present, or by the else-case or empty string if no else-case is given otherwise. Params are taken from the params collection.</li>
     *     <li>Conditional templates: '{{[name]?[if-case]}}' or '{{[name]?[if-case]:[else-case]}}' function exactly like the param templates,
     *     except that instead of the params collection the keys of the template values are checked.</li>
     * </ul>
     *
     * @param rawString      The string to render the templates in.
     * @param templateValues The values to replace the templates with.
     * @param params         The parameters to check for.
     * @return The string with the templates replaced.
     */
    public static String renderTemplates(String rawString, Map<String, String> templateValues, Collection<String> params) {
        CharSequence result = rawString;
        result = replaceConditionalTemplates(result, TEMPLATE_CONDITIONAL_PATTERN, templateValues != null ? templateValues.keySet() : emptySet());
        result = replaceConditionalTemplates(result, PARAM_PATTERN, params != null ? params : emptySet());
        result = replaceTemplateValues(result, templateValues != null ? templateValues : emptyMap());
        return result.toString().trim();
    }

    private static CharSequence replaceTemplateValues(CharSequence input, Map<String, String> templateValues) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = TEMPLATE_VALUE_PATTERN.matcher(input);

        while (matcher.find()) {
            String templateName = matcher.group(1);
            String defaultValue = matcher.group(2);
            if (defaultValue == null) defaultValue = templateName;

            matcher.appendReplacement(result, templateValues.getOrDefault(templateName, defaultValue));
        }
        matcher.appendTail(result);

        return result;
    }

    private static CharSequence replaceConditionalTemplates(CharSequence input, Pattern pattern, Collection<String> availableKeys) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String leadingSpace = matcher.group(1);
            String paramName = matcher.group(2);
            String ifText = matcher.group(3);
            String elseText = matcher.group(4);
            String trailingSpace = matcher.group(5);
            if (availableKeys.contains(paramName)) {
                matcher.appendReplacement(result, leadingSpace + ifText + trailingSpace);
            } else if (elseText == null) {
                String replacement = leadingSpace.length() == 1 && trailingSpace.length() == 1 ? " " : "";
                matcher.appendReplacement(result, replacement);
            } else {
                matcher.appendReplacement(result, leadingSpace + elseText + trailingSpace);
            }
        }
        matcher.appendTail(result);

        return result;
    }

}
