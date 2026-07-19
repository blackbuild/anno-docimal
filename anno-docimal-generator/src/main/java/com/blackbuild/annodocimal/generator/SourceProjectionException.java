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
package com.blackbuild.annodocimal.generator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Reports that a readable class contains a selected declaration that cannot be represented as valid Java source.
 */
@NullMarked
public final class SourceProjectionException extends RuntimeException {

    /** Projection input retained for diagnostics. */
    private final Path inputPath;
    /** Stable identifier for the selected declaration, when known. */
    private final @Nullable String declarationIdentifier;

    SourceProjectionException(Path inputPath, @Nullable String declarationIdentifier, String message) {
        this(inputPath, declarationIdentifier, message, null);
    }

    SourceProjectionException(Path inputPath, @Nullable String declarationIdentifier, String message,
                              @Nullable Throwable cause) {
        super(message, cause);
        this.inputPath = Objects.requireNonNull(inputPath, "inputPath");
        this.declarationIdentifier = declarationIdentifier;
    }

    /**
     * Returns the caller-supplied class-file path.
     *
     * @return the projection input
     */
    public Path getInputPath() {
        return inputPath;
    }

    /**
     * Returns a stable Java-style declaration identifier when one was available.
     *
     * @return the declaration identifier, if known
     */
    public Optional<String> getDeclarationIdentifier() {
        return Optional.ofNullable(declarationIdentifier);
    }
}
