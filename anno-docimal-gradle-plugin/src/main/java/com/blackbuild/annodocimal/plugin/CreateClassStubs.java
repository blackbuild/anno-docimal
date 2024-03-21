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
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;


public abstract class CreateClassStubs extends DefaultTask {

    private ClassLoader classLoader;

    @Input
    public abstract Property<SourceSet> getSourceSet();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void execute() {
        FileCollection classesDirs = getSourceSet().get().getOutput().getClassesDirs();
        FileCollection runtimeClasspath = getSourceSet().get().getCompileClasspath().plus(classesDirs);

        createClassLoader(runtimeClasspath);

        classesDirs.getAsFileTree()
                .matching(f -> f.exclude("**/*$*").include("**/*.class"))
                .visit(this::handleClassFile);
    }

    private void handleClassFile(FileVisitDetails fileVisitDetails) {
        try {
            Class<?> clazz = classLoader.loadClass(toClassName(fileVisitDetails));
            AnnoDocGenerator.generate(clazz, getOutputDirectory().get().getAsFile());
        } catch (ClassNotFoundException e) {
            throw new GradleException("Error loading class " + toClassName(fileVisitDetails), e);
        } catch (IOException e) {
            throw new GradleException("Could not write stub for " + toClassName(fileVisitDetails), e);
        }
    }

    @NotNull
    private static String toClassName(FileVisitDetails fileVisitDetails) {
        return fileVisitDetails.getRelativePath().toString().replace(".class", "").replace("/", ".");
    }

    private void createClassLoader(FileCollection runtimeClasspath) {
        URL[] classpath = runtimeClasspath.getFiles().stream()
                .map(File::toURI)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (MalformedURLException e) {
                        throw new GradleException("Error creating classloader from classpath", e);
                    }
                })
                .toArray(URL[]::new);
        classLoader = new URLClassLoader(classpath);
    }

}
