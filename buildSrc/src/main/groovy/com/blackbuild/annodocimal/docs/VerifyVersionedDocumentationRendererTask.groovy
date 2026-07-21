/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2026 Stephan Pauxberger
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
package com.blackbuild.annodocimal.docs

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.security.MessageDigest

/** Hermetic acceptance fixture for AnnoDocimal's immutable documentation renderer. */
abstract class VerifyVersionedDocumentationRendererTask extends DefaultTask {
    @TaskAction
    void verifyRendererContract() {
        File fixture = Files.createTempDirectory(temporaryDir.toPath(), 'versioned-documentation-').toFile()
        git(fixture, ['init']); git(fixture, ['config', 'user.email', 'fixtures@example.invalid']); git(fixture, ['config', 'user.name', 'Documentation fixtures'])
        new File(fixture, 'docs').mkdirs()
        new File(fixture, 'README.md').text = '# AnnoDocimal\n'
        new File(fixture, 'CHANGES.md').text = '# Changes\n'
        new File(fixture, 'docs/usage.md').text = '# Usage\n'
        git(fixture, ['add', '.']); git(fixture, ['commit', '-m', 'fixture documentation'])
        String revision = git(fixture, ['rev-parse', 'HEAD']).trim()
        File javadoc = new File(temporaryDir, 'javadoc'); javadoc.mkdirs(); new File(javadoc, 'index.html').text = '<title>API</title>'
        File one = new File(temporaryDir, 'one'); File two = new File(temporaryDir, 'two')
        project.delete(one, two)
        render(fixture, one, revision, javadoc); render(fixture, two, revision, javadoc)
        assertTrue(new File(one, '1.0.0-rc.1/index.md').text.contains('immutable documentation snapshot'), 'renderer-owned Markdown chrome')
        assertTrue(new File(one, '1.0.0-rc.1/api/anno-docimal-annotations/index.html').file, 'module Javadoc landing')
        assertTrue(new File(one, '1.0.0-rc.1/source-manifest.json').text.contains(revision), 'exact source evidence')
        assertTrue(digest(one) == digest(two), 'repeat rendering is deterministic')
        expectFailure { VersionedDocumentationRenderer.render(objectDirectory: fixture, outputDirectory: new File(temporaryDir, 'bad'), revision: '0' * 40, rendererRevision: revision, version: '1.0.0-rc.1', stage: 'public-rc', javadocInputDirectories: ['api': javadoc]) }
        new File(fixture, 'dirty').text = 'dirty'
        expectFailure { VerifyVersionedDocumentationRendererTask.render(fixture, new File(temporaryDir, 'dirty-output'), revision, javadoc) }
    }

    private static void render(File fixture, File output, String revision, File javadoc) {
        VersionedDocumentationRenderer.render(objectDirectory: fixture, outputDirectory: output, revision: revision,
                rendererRevision: revision, version: '1.0.0-rc.1', stage: 'public-rc',
                javadocInputDirectories: ['anno-docimal-annotations': javadoc])
    }
    private static String digest(File root) {
        MessageDigest value = MessageDigest.getInstance('SHA-256')
        root.eachFileRecurse { file ->
            if (file.file) {
                value.update(root.toPath().relativize(file.toPath()).toString().bytes)
                value.update(file.bytes)
            }
        }
        value.digest().encodeHex().toString()
    }
    private static String git(File directory, List<String> arguments) { def process = new ProcessBuilder((['git'] + arguments) as List<String>).directory(directory).redirectErrorStream(true).start(); String output = process.inputStream.text; if (process.waitFor() != 0) throw new IllegalStateException(output); output }
    private static void assertTrue(boolean condition, String message) { if (!condition) throw new AssertionError(message) }
    private static void expectFailure(Closure action) { try { action.call(); throw new AssertionError('Expected renderer failure') } catch (IllegalArgumentException ignored) { } }
}
