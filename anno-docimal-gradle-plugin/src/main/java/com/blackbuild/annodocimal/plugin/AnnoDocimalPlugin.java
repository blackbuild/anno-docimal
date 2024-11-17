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

import org.gradle.api.*;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.tasks.compile.GroovyCompile;
import org.gradle.api.tasks.compile.GroovyCompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;

import java.util.HashMap;
import java.util.Map;

public class AnnoDocimalPlugin implements Plugin<Project> {

    private Project project;

    @Override
    public void apply(Project project) {
        this.project = project;
        project.getPluginManager().apply(AnnoDocimalBasePlugin.class);

        project.getTasks().withType(GroovyCompile.class).configureEach(task ->
                task.doFirst(new SetGroovyCompilerOptions())
        );

        project.getTasks().withType(JavaCompile.class).configureEach(task ->
                task.getOptions().getCompilerArgs().add("-parameters")
        );
    }

    private static boolean isGroovy24Dependency(ResolvedDependency dep) {
        return dep.getModuleGroup().equals("org.codehaus.groovy") && dep.getModuleVersion().startsWith("2.4.");
    }

    @NonNullApi
    private class SetGroovyCompilerOptions implements Action<Task> {

        @Override
        public void execute(Task task) {
            project.getConfigurations().getByName("compileClasspath", conf -> {
                if (conf.getResolvedConfiguration().getFirstLevelModuleDependencies().stream().noneMatch(AnnoDocimalPlugin::isGroovy24Dependency)) {
                    GroovyCompileOptions groovyOptions = ((GroovyCompile) task).getGroovyOptions();
                    Map<String, Boolean> optimizationOptions = groovyOptions.getOptimizationOptions();
                    if (optimizationOptions == null) {
                        optimizationOptions = new HashMap<>();
                        groovyOptions.setOptimizationOptions(optimizationOptions);
                    }
                    optimizationOptions.put("groovydoc", true);
                    groovyOptions.setParameters(true);
                }
            });
        }
    }
}
