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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Thread-safe facade that reconstructs documentation-oriented Java source from one caller-selected class file.
 *
 * <p>This service does not scan class directories, select top-level inputs, or remove stale outputs. Those collection
 * concerns belong to the caller or build task.</p>
 */
@NullMarked
public final class SourceProjector {

    private final ProjectionPolicy policy;

    /**
     * Creates a projector with one immutable inclusion policy.
     *
     * @param policy projection policy
     */
    public SourceProjector(ProjectionPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    /**
     * Projects one top-level class file to deterministic Java source text.
     *
     * @param classFile caller-selected top-level class file
     * @return Java source using LF line endings
     * @throws IOException if the class file cannot be read
     * @throws SourceProjectionException if a selected declaration cannot be represented as valid Java source
     */
    public String projectToText(Path classFile) throws IOException {
        return project(classFile).source;
    }

    /**
     * Projects one top-level class file beneath a managed output directory.
     *
     * <p>Only the package/type-relative source file is replaced. The method creates parent directories and does not
     * scan or clean any other output.</p>
     *
     * @param classFile caller-selected top-level class file
     * @param outputDirectory managed output root
     * @return the package/type-relative source path that was written
     * @throws IOException if input or output file-system access fails
     * @throws SourceProjectionException if a selected declaration cannot be represented as valid Java source
     */
    public Path projectToDirectory(Path classFile, Path outputDirectory) throws IOException {
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        ProjectionResult projection = project(classFile);
        Path normalizedOutput = outputDirectory.normalize();
        Path target = normalizedOutput.resolve(projection.internalName + ".java").normalize();
        if (!target.startsWith(normalizedOutput)) {
            throw new SourceProjectionException(classFile, projection.internalName.replace('/', '.'),
                    "Projected source path escapes its managed output directory");
        }
        Path parent = target.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            Files.writeString(temporary, projection.source, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
            return target;
        } finally {
            if (!moved) Files.deleteIfExists(temporary);
        }
    }

    private ProjectionResult project(Path classFile) throws IOException {
        Objects.requireNonNull(classFile, "classFile");
        ProjectionResult projection = SpecConverter.project(classFile, policy);
        String normalizedSource = projection.source.replace("\r\n", "\n").replace('\r', '\n');
        return new ProjectionResult(projection.internalName, normalizedSource);
    }

    static final class ProjectionResult {
        private final String internalName;
        private final String source;

        ProjectionResult(String internalName, String source) {
            this.internalName = internalName;
            this.source = source;
        }
    }
}
