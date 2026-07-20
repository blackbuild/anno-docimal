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
package com.blackbuild.annodocimal.plugin;

import com.blackbuild.annodocimal.generator.ProjectionPolicy;
import com.blackbuild.annodocimal.generator.SourceProjector;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.jspecify.annotations.NullMarked;

import javax.inject.Inject;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Projects selected top-level class files into one exclusively managed source directory.
 *
 * <p>Include and exclude patterns use slash-normalized paths relative to each input directory. Patterns include the
 * {@code .class} suffix; exclusions take precedence. The default selects every top-level class file.</p>
 */
@CacheableTask
@NullMarked
public abstract class SourceProjectionTask extends DefaultTask {

    @Inject
    public SourceProjectionTask(ObjectFactory objects) {
        getIncludes().convention(Collections.singleton("**/*.class"));
        getExcludes().convention(Collections.emptySet());
        getProjectionPolicy().convention(ProjectionPolicy.documentation());
    }

    /**
     * Class-output directories containing candidate top-level class files.
     *
     * @return documentation-sensitive class inputs
     */
    @Classpath
    public abstract ConfigurableFileCollection getClassesDirectories();

    /**
     * Ant-style include patterns over slash-normalized relative class-file paths.
     *
     * @return selected candidate patterns
     */
    @Input
    public abstract SetProperty<String> getIncludes();

    /**
     * Ant-style exclude patterns over slash-normalized relative class-file paths.
     *
     * @return excluded candidate patterns
     */
    @Input
    public abstract SetProperty<String> getExcludes();

    /**
     * Inclusion policy passed to the documentation-oriented source projector.
     *
     * @return projection policy
     */
    @Nested
    public abstract Property<ProjectionPolicy> getProjectionPolicy();

    /**
     * The directory exclusively managed by this task.
     *
     * @return managed source output directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    protected final void projectSources() {
        Path outputDirectory = getOutputDirectory().get().getAsFile().toPath().toAbsolutePath().normalize();
        List<Candidate> candidates = selectedCandidates(outputDirectory);
        Path stagingDirectory = createStagingDirectory(outputDirectory);
        boolean replaced = false;
        try {
            SourceProjector projector = new SourceProjector(getProjectionPolicy().get());
            for (Candidate candidate : candidates) {
                projector.projectToDirectory(candidate.classFile, stagingDirectory);
            }
            replaceOutputDirectory(stagingDirectory, outputDirectory);
            replaced = true;
        } catch (IOException exception) {
            throw new GradleException("Could not project selected source classes", exception);
        } finally {
            if (!replaced) deleteRecursively(stagingDirectory);
        }
    }

    private List<Candidate> selectedCandidates(Path outputDirectory) {
        List<Path> inputDirectories = getClassesDirectories().getFiles().stream()
                .map(file -> file.toPath().toAbsolutePath().normalize())
                .sorted()
                .toList();
        ensureInputsDoNotOverlapOutput(inputDirectories, outputDirectory);

        List<Pattern> includes = patterns(getIncludes().get());
        List<Pattern> excludes = patterns(getExcludes().get());
        Map<String, Candidate> candidatesByBinaryName = new LinkedHashMap<>();
        for (Path inputDirectory : inputDirectories) {
            if (!Files.exists(inputDirectory)) continue;
            if (!Files.isDirectory(inputDirectory)) {
                throw new GradleException("SourceProjectionTask classes directory does not exist: " + inputDirectory);
            }
            try (Stream<Path> files = Files.walk(inputDirectory)) {
                files.filter(Files::isRegularFile)
                        .map(classFile -> candidate(inputDirectory, classFile, includes, excludes))
                        .filter(candidate -> candidate != null)
                        .sorted(Comparator.comparing(candidate -> candidate.classFile.toString()))
                        .forEach(candidate -> addCandidate(candidatesByBinaryName, candidate));
            } catch (IOException exception) {
                throw new GradleException("Could not inspect classes directory " + inputDirectory, exception);
            }
        }
        return candidatesByBinaryName.values().stream()
                .sorted(Comparator.comparing(candidate -> candidate.binaryName))
                .toList();
    }

    private static Candidate candidate(Path inputDirectory, Path classFile, List<Pattern> includes, List<Pattern> excludes) {
        String relativePath = inputDirectory.relativize(classFile).toString().replace('\\', '/');
        if (!relativePath.endsWith(".class") || !matches(relativePath, includes, excludes)) return null;
        ClassMetadata metadata = readMetadata(classFile);
        return metadata.topLevel ? new Candidate(classFile, metadata.binaryName) : null;
    }

    private static void addCandidate(Map<String, Candidate> candidatesByBinaryName, Candidate candidate) {
        Candidate duplicate = candidatesByBinaryName.putIfAbsent(candidate.binaryName, candidate);
        if (duplicate != null) {
            throw new GradleException("Selected class " + candidate.binaryName + " appears in both "
                    + duplicate.classFile + " and " + candidate.classFile);
        }
    }

    private static boolean matches(String relativePath, List<Pattern> includes, List<Pattern> excludes) {
        return includes.stream().anyMatch(pattern -> pattern.matcher(relativePath).matches())
                && excludes.stream().noneMatch(pattern -> pattern.matcher(relativePath).matches());
    }

    private static List<Pattern> patterns(Set<String> patterns) {
        List<Pattern> result = new ArrayList<>();
        patterns.stream().sorted().forEach(pattern -> result.add(Pattern.compile(antPattern(pattern))));
        return result;
    }

    private static String antPattern(String pattern) {
        StringBuilder expression = new StringBuilder("^");
        String normalized = pattern.replace('\\', '/');
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            if (character == '*') {
                boolean recursive = index + 1 < normalized.length() && normalized.charAt(index + 1) == '*';
                if (recursive) {
                    index++;
                    if (index + 1 < normalized.length() && normalized.charAt(index + 1) == '/') {
                        index++;
                        expression.append("(?:.*/)?");
                    } else {
                        expression.append(".*");
                    }
                } else {
                    expression.append("[^/]*");
                }
            } else if (character == '?') {
                expression.append("[^/]");
            } else {
                if ("\\.[]{}()+-^$|".indexOf(character) >= 0) expression.append('\\');
                expression.append(character);
            }
        }
        return expression.append('$').toString();
    }

    private static void ensureInputsDoNotOverlapOutput(List<Path> inputDirectories, Path outputDirectory) {
        for (Path inputDirectory : inputDirectories) {
            if (inputDirectory.startsWith(outputDirectory) || outputDirectory.startsWith(inputDirectory)) {
                throw new GradleException("SourceProjectionTask input and output directories must not overlap: "
                        + inputDirectory + " and " + outputDirectory);
            }
        }
    }

    private static Path createStagingDirectory(Path outputDirectory) {
        try {
            Path parent = outputDirectory.getParent();
            if (parent == null) throw new GradleException("SourceProjectionTask output directory has no parent: " + outputDirectory);
            Files.createDirectories(parent);
            return Files.createTempDirectory(parent, outputDirectory.getFileName() + ".staging-");
        } catch (IOException exception) {
            throw new GradleException("Could not create staging directory for " + outputDirectory, exception);
        }
    }

    private static void replaceOutputDirectory(Path stagingDirectory, Path outputDirectory) throws IOException {
        if (!Files.exists(outputDirectory)) {
            move(stagingDirectory, outputDirectory);
            return;
        }
        Path backupDirectory = outputDirectory.resolveSibling(outputDirectory.getFileName() + ".backup-" + UUID.randomUUID());
        move(outputDirectory, backupDirectory);
        try {
            move(stagingDirectory, outputDirectory);
        } catch (IOException exception) {
            move(backupDirectory, outputDirectory);
            throw exception;
        }
        deleteRecursively(backupDirectory);
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target);
        }
    }

    private static void deleteRecursively(Path directory) {
        if (!Files.exists(directory)) return;
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException exception) {
                    throw new GradleException("Could not delete managed projection output " + path, exception);
                }
            });
        } catch (IOException exception) {
            throw new GradleException("Could not delete managed projection output " + directory, exception);
        }
    }

    private static ClassMetadata readMetadata(Path classFile) {
        try (InputStream input = Files.newInputStream(classFile); DataInputStream data = new DataInputStream(input)) {
            if (data.readInt() != 0xcafebabe) throw new GradleException("Not a class file: " + classFile);
            data.readUnsignedShort();
            data.readUnsignedShort();
            int constantPoolSize = data.readUnsignedShort();
            String[] utf8 = new String[constantPoolSize];
            int[] classNames = new int[constantPoolSize];
            for (int index = 1; index < constantPoolSize; index++) {
                int tag = data.readUnsignedByte();
                switch (tag) {
                    case 1 -> utf8[index] = data.readUTF();
                    case 3, 4 -> skip(data, 4);
                    case 5, 6 -> {
                        skip(data, 8);
                        index++;
                    }
                    case 7 -> classNames[index] = data.readUnsignedShort();
                    case 8, 16, 19, 20 -> skip(data, 2);
                    case 9, 10, 11, 12, 17, 18 -> skip(data, 4);
                    case 15 -> skip(data, 3);
                    default -> throw new GradleException("Unsupported class-file constant-pool tag " + tag + " in " + classFile);
                }
            }
            data.readUnsignedShort();
            int thisClass = data.readUnsignedShort();
            data.readUnsignedShort();
            String binaryName = utf8[classNames[thisClass]].replace('/', '.');
            skipInterfaces(data);
            skipMembers(data);
            skipMembers(data);
            return new ClassMetadata(binaryName, !isMemberOrLocalClass(data, thisClass, utf8));
        } catch (IOException exception) {
            throw new GradleException("Could not read class metadata from " + classFile, exception);
        }
    }

    private static void skipInterfaces(DataInputStream data) throws IOException {
        int count = data.readUnsignedShort();
        skip(data, count * 2L);
    }

    private static void skipMembers(DataInputStream data) throws IOException {
        int count = data.readUnsignedShort();
        for (int index = 0; index < count; index++) {
            skip(data, 6);
            skipAttributes(data);
        }
    }

    private static boolean isMemberOrLocalClass(DataInputStream data, int thisClass, String[] utf8) throws IOException {
        int attributeCount = data.readUnsignedShort();
        boolean enclosed = false;
        for (int index = 0; index < attributeCount; index++) {
            int nameIndex = data.readUnsignedShort();
            long length = Integer.toUnsignedLong(data.readInt());
            if (!"InnerClasses".equals(utf8[nameIndex])) {
                if ("EnclosingMethod".equals(utf8[nameIndex])) enclosed = true;
                skip(data, length);
                continue;
            }
            int classCount = data.readUnsignedShort();
            for (int classIndex = 0; classIndex < classCount; classIndex++) {
                int innerClass = data.readUnsignedShort();
                skip(data, 2);
                skip(data, 2);
                skip(data, 2);
                if (innerClass == thisClass) enclosed = true;
            }
        }
        return enclosed;
    }

    private static void skipAttributes(DataInputStream data) throws IOException {
        int count = data.readUnsignedShort();
        for (int index = 0; index < count; index++) {
            skip(data, 2);
            skip(data, Integer.toUnsignedLong(data.readInt()));
        }
    }

    private static void skip(DataInputStream data, long bytes) throws IOException {
        data.skipNBytes(bytes);
    }

    private record Candidate(Path classFile, String binaryName) {
    }

    private record ClassMetadata(String binaryName, boolean topLevel) {
    }
}
