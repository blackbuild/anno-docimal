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

import com.blackbuild.annodocimal.generator.AnnoDocGenerator;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@CacheableTask
public abstract class CreateClassStubs extends DefaultTask {

    private final ConfigurableFileCollection classesDirs = getProject().getObjects().fileCollection();

    @CompileClasspath
    public FileCollection getClassesDir() {
        return classesDirs;
    }

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    public CreateClassStubs classes(Object... classes) {
        this.classesDirs.from(classes);
        return this;
    }

    @TaskAction
    public void execute() {
        getClassesDir().getAsFileTree()
                .matching(f -> f.exclude("**/*$*").include("**/*.class"))
                .visit(this::handleClassFile);
    }

    private void handleClassFile(FileVisitDetails fileVisitDetails) {
        if (fileVisitDetails.isDirectory()) return;
        try {
            AnnoDocGenerator.generate(fileVisitDetails.getFile(), getOutputDirectory().get().getAsFile());
        } catch (IOException e) {
            throw new GradleException("Could not write stub for " + toClassName(fileVisitDetails), e);
        }
    }

    @NotNull
    private static String toClassName(FileVisitDetails fileVisitDetails) {
        return fileVisitDetails.getRelativePath().toString().replace(".class", "").replace("/", ".");
    }

}
