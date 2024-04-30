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

import java.util.List;

/**
 * Builder to easily create Javadoc strings. Is designed to create a single instance of the builder
 * per javadoc comment.
 */
public interface DocBuilder {

    /**
     * Creates a copy of the builder. The copy is independent of the original builder and can be modified
     * without affecting the original.
     *
     * @return the copy of the builder
     */
    DocBuilder getCopy();

    /**
     * Checks if the builder is empty. A builder is considered empty if no content has been added to it.
     *
     * @return {@code true} if the builder is empty, {@code false} otherwise
     */
    boolean isEmpty();

    /**
     * Copies the javadoc from a raw text string into this builder. Existing content in the builder is not overwritten.
     * Note that additional paragraphs created with {@link #extraP(String)} are not effected by this method.
     *
     * @param rawText the raw text string
     * @return this builder
     */
    DocBuilder fromRawText(String rawText);

    /**
     * Copies the javadoc from a {@link DocText} object into this builder. Existing content in the builder is not
     * overwritten. Note that additional paragraphs created with {@link #extraP(String)} are not effected by this method.
     *
     * @param docText the {@link DocText} object
     * @return this builder
     */
    DocBuilder fromDocText(DocText docText);

    /**
     * Sets the title of the Javadoc. As per Javadoc convention, the title should end with a period.
     *
     * @param title the title of the Javadoc
     * @return this builder
     */
    DocBuilder title(String title);

    /**
     * Adds a paragraph to the Javadoc. Paragraphs are added in the given order. In standard javadoc output,
     * paragraphs are wrapped in {@code <p>} tags.
     *
     * @param paragraph the paragraph to add
     * @return this builder
     */
    DocBuilder p(String paragraph);

    /**
     * Adds a paragraph to the Javadoc. Paragraphs are added in the given order. In standard javadoc output,
     * paragraphs are wrapped in {@code <p>} tags. In contrast to {@link #p(String)}, additionalParagraphs
     * are not effected by {@link #fromDocText(DocText)}, thus the result would a combination of the copy source's
     * paragraphs and the additional paragraphs. This is useful for taking the content of a method's javadoc and
     * adding additional information for a specific use case.
     *
     * @param paragraph the paragraph to add
     * @return this builder
     */
    DocBuilder extraP(String paragraph);

    /**
     * Adds a code block to the Javadoc. Code blocks are special paragraphs that include more sophisticated
     * encoding, especially for the symbols {@literal @}, &lt; and &gt;.
     *
     * @param code the code block to add. Should be added as is, without any additional formatting.
     * @return this builder
     */
    DocBuilder code(String code);

    /**
     * Adds a parameter to the Javadoc. Parameters are added in the given order. In standard javadoc output,
     * parameters are added as single line blocks starting with {@code @param}.
     *
     * @param name        the name of the parameter
     * @param description the description of the parameter
     * @return this builder
     */
    DocBuilder param(String name, String description);

    /**
     * Sets the return type of the Javadoc. In standard javadoc output, the return type is added as a single line
     * block starting with {@code @return}.
     *
     * @param returnType the return type of the method
     * @return this builder
     */
    DocBuilder returnType(String returnType);

    /**
     * Adds an exception to the Javadoc. Exceptions are added in the given order. In standard javadoc output,
     * exceptions are added as single line blocks starting with {@code @throws}.
     *
     * @param exception   the name of the exception
     * @param description the description of the exception
     * @return this builder
     */
    DocBuilder throwsException(String exception, String description);

    /**
     * Adds a tag to the Javadoc. Tags are added in the given order. In standard javadoc output,
     * tags are added as single line blocks starting with the tag name.
     *
     * @param tag         the name of the tag
     * @param description the description of the tag
     * @return this builder
     */
    DocBuilder tag(String tag, String description);

    /**
     * Adds a 'see' tag to the Javadoc.
     *
     * @param links the description of the tag
     * @return this builder
     */
    DocBuilder seeAlso(String... links);

    /**
     * Sets the since-version in the Javadoc. In standard javadoc output, the version is added as a single line
     * block starting with {@code @since}.
     *
     * @param version the version of the method
     * @return this builder
     */
    DocBuilder since(String version);

    /**
     * Marks the method as deprecated. In standard javadoc output, the deprecation is added as a single line
     * block starting with {@code @deprecated}.
     *
     * @param reason the reason for deprecation
     * @return this builder
     */
    DocBuilder deprecated(String reason);

    /**
     * Adds an author tag to the Javadoc. In standard javadoc output, the author is added as a single line
     * block starting with {@code @author}.
     *
     * @param author the author of the method
     * @return this builder
     */
    DocBuilder author(String author);

    /**
     * Creates the Javadoc string from the builder. The actual formatting is dependent on the implementation.
     *
     * @return the Javadoc string
     */
    String toJavadoc();

    /**
     * Creates the Javadoc string from the builder. The actual formatting is dependent on the implementation.
     * Parameter tags for parameters not in the validParameters list are not included.
     *
     * @param validParameters the list of valid parameters
     * @return the Javadoc string
     */
    String toJavadoc(List<String> validParameters);
}
