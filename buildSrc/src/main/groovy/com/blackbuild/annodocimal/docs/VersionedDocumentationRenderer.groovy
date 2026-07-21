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

import groovy.json.JsonOutput

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.util.regex.Matcher

/** Renders one immutable AnnoDocimal documentation and API snapshot from an exact Git commit. */
class VersionedDocumentationRenderer {

    static final String RENDERER_ID = 'annodocimal-buildsrc-vd-1'
    private static final Set<String> STAGES = ['public-rc', 'release'] as Set
    private static final String VERSION_PATTERN = /\d+\.\d+\.\d+(?:-rc\.[1-9]\d*)?/
    private static final Set<String> RESERVED_PATHS = ['source-manifest.json', 'version-status.md'] as Set

    static void render(Map<String, ?> inputs) {
        File objectDirectory = directory(inputs, 'objectDirectory')
        File outputDirectory = file(inputs, 'outputDirectory')
        String revision = string(inputs, 'revision')
        String rendererRevision = string(inputs, 'rendererRevision')
        String version = string(inputs, 'version')
        String stage = string(inputs, 'stage')
        Map<String, File> javadocs = javadocs(inputs.javadocInputDirectories ?: [:])

        requireFullSha(revision, 'revision')
        requireFullSha(rendererRevision, 'rendererRevision')
        if (!(version ==~ VERSION_PATTERN)) fail("Documentation version must be exact: $version")
        if (!STAGES.contains(stage)) fail("Documentation stage must be one of $STAGES: $stage")
        if (stage == 'public-rc' != version.contains('-rc.'))
            fail("$stage must agree with exact RC/final version $version")
        if (git(objectDirectory, ['status', '--porcelain']).trim())
            fail('Documentation input worktree is dirty; render a checked-out immutable revision.')
        if (git(objectDirectory, ['rev-parse', '--verify', "${revision}^{commit}"]).trim() != revision)
            fail("Revision must resolve to the supplied full SHA: $revision")

        if (outputDirectory.exists() && outputDirectory.listFiles()?.length)
            fail("Output directory must be empty so an immutable snapshot cannot be overwritten: $outputDirectory")
        outputDirectory.mkdirs()
        File exactDirectory = new File(outputDirectory, version)
        Set<String> outputPaths = new TreeSet<>()
        copyGitTree(objectDirectory, revision, version, 'docs', exactDirectory, outputPaths)
        copyGitFile(objectDirectory, revision, version, 'README.md', exactDirectory, 'index.md', outputPaths)
        copyGitFile(objectDirectory, revision, version, 'CHANGES.md', exactDirectory, 'changes.md', outputPaths)

        javadocs.each { module, directory ->
            if (!new File(directory, 'index.html').file)
                fail("Javadoc input for $module has no index.html: $directory")
            copyDirectory(directory, new File(exactDirectory, "api/$module"), outputPaths, "api/$module")
        }
        outputPaths.add('api/index.md')
        write(exactDirectory, 'api/index.md', apiIndex(version, javadocs.keySet()).getBytes(StandardCharsets.UTF_8))
        outputPaths.add('version-status.md')
        write(exactDirectory, 'version-status.md', versionStatus(version, stage).getBytes(StandardCharsets.UTF_8))
        verifyLocalMarkdownLinks(exactDirectory)

        Map<String, String> hashes = hashes(outputDirectory)
        hashes.remove("$version/source-manifest.json")
        Map<String, ?> manifest = [
                schemaVersion: 1,
                renderer     : [id: RENDERER_ID, revision: rendererRevision],
                source       : [revision: revision, documentationRoot: 'docs', readme: 'README.md', changes: 'CHANGES.md'],
                documentation: [version: version, stage: stage],
                javadocs     : javadocs.collectEntries { module, directory -> [(module): sha256Directory(directory)] },
                generatedFiles: new TreeSet<>(hashes.keySet()),
                outputHashes : new TreeMap<>(hashes)
        ]
        write(exactDirectory, 'source-manifest.json', (JsonOutput.prettyPrint(JsonOutput.toJson(manifest)) + '\n').getBytes(StandardCharsets.UTF_8))
    }

    private static void copyGitTree(File repository, String revision, String version, String root, File output, Set<String> paths) {
        List<String> files = git(repository, ['ls-tree', '-r', '--name-only', revision, '--', root]).readLines().findAll { it }
        if (files.empty) fail("Revision $revision does not contain canonical documentation under $root/")
        files.each { source ->
            String target = source.substring(root.length() + 1)
            copyGitFile(repository, revision, version, source, output, "docs/$target", paths)
        }
    }

    private static void copyGitFile(File repository, String revision, String version, String source, File output, String target, Set<String> paths) {
        safePath(target)
        if (RESERVED_PATHS.contains(target)) fail("Source path collides with renderer-owned output: $target")
        if (!paths.add(target)) fail("Duplicate rendered path: $target")
        byte[] content = gitBytes(repository, ['show', "$revision:$source"])
        if (target.endsWith('.md')) content = (chrome(version, revision, source) + new String(content, StandardCharsets.UTF_8)).bytes
        write(output, target, content)
    }

    private static void copyDirectory(File source, File target, Set<String> paths, String prefix) {
        source.eachFileRecurse { child ->
            if (!child.file) return
            String relative = source.toPath().relativize(child.toPath()).toString().replace(File.separatorChar, '/' as char)
            String path = "$prefix/$relative"
            safePath(path)
            if (!paths.add(path)) fail("Javadoc path collides with rendered documentation: $path")
            write(target, relative, child.bytes)
        }
    }

    private static String chrome(String version, String revision, String path) {
        "<!-- Generated by $RENDERER_ID. Do not edit this rendered copy. -->\n" +
                "> **AnnoDocimal immutable documentation snapshot.** [API reference](/$version/api/) · " +
                "Source: [$revision](https://github.com/blackbuild/anno-docimal/tree/$revision/$path).\n\n"
    }

    private static String apiIndex(String version, Set<String> modules) {
        "# AnnoDocimal $version API reference\n\n" + modules.collect { "- [$it]($it/)" }.join('\n') + '\n'
    }

    private static String versionStatus(String version, String stage) {
        "# AnnoDocimal $version version status\n\n" +
                "Stage: **$stage**.\n\n" +
                (stage == 'public-rc' ? 'This is a prerelease and is not stable.\n' : 'This is an immutable final release snapshot.\n')
    }

    private static void verifyLocalMarkdownLinks(File root) {
        root.eachFileRecurse { page ->
            if (!page.file || !page.name.endsWith('.md')) return
            Matcher matcher = (page.getText(StandardCharsets.UTF_8.name()) =~ /\[[^]]+\]\(([^)]+)\)/)
            matcher.each { match ->
                String destination = match[1].trim()
                if (destination.startsWith('#') || destination ==~ /(?i)(https?|mailto):.*/) return
                String path = destination.split('#', 2)[0]
                if (!path || path.contains(' ') || path.startsWith('/')) return
                File target = new File(page.parentFile, path)
                if (!target.file && !(target.directory && (new File(target, 'index.md').file || new File(target, 'index.html').file)))
                    fail("Rendered Markdown link is unresolved: ${root.toPath().relativize(page.toPath())} -> $destination")
            }
        }
    }

    private static Map<String, File> javadocs(Object value) {
        if (!(value instanceof Map) || value.empty) fail('Javadoc inputs must name every supported public Java API artifact')
        Map<String, File> result = new TreeMap<>()
        value.each { name, path ->
            if (!(name instanceof String) || !name || path == null) fail('Javadoc inputs require non-empty module names and directories')
            File directory = path instanceof File ? path : new File(path.toString())
            if (!directory.directory) fail("Javadoc input for $name is not a directory: $directory")
            result[name] = directory
        }
        result
    }

    private static Map<String, String> hashes(File root) {
        Map<String, String> values = new TreeMap<>()
        root.eachFileRecurse { child ->
            if (child.file) values[root.toPath().relativize(child.toPath()).toString().replace(File.separatorChar, '/' as char)] = sha256(child.bytes)
        }
        values
    }

    private static String sha256Directory(File directory) {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        directory.eachFileRecurse { child ->
            if (child.file) {
                digest.update(directory.toPath().relativize(child.toPath()).toString().getBytes(StandardCharsets.UTF_8))
                digest.update(child.bytes)
            }
        }
        digest.digest().encodeHex().toString()
    }

    private static String sha256(byte[] value) { MessageDigest.getInstance('SHA-256').digest(value).encodeHex().toString() }
    private static void write(File root, String relative, byte[] content) { File target = new File(root, relative); target.parentFile.mkdirs(); Files.write(target.toPath(), content) }
    private static File file(Map values, String key) { values[key] == null ? fail("$key is required") : values[key] instanceof File ? values[key] : new File(values[key].toString()) }
    private static File directory(Map values, String key) { File value = file(values, key); if (!value.directory) fail("$key must be a directory: $value"); value }
    private static String string(Map values, String key) { Object value = values[key]; if (!(value instanceof String) || value.trim().empty) fail("$key is required"); value.trim() }
    private static void safePath(String path) { if (path.startsWith('/') || path.contains('\\') || path.tokenize('/').contains('..')) fail("Unsafe output path: $path") }
    private static void requireFullSha(String value, String key) { if (!(value ==~ /[0-9a-f]{40}/)) fail("$key must be a full lowercase Git SHA: $value") }
    private static String git(File directory, List<String> arguments) { new String(gitBytes(directory, arguments), StandardCharsets.UTF_8) }
    private static byte[] gitBytes(File directory, List<String> arguments) {
        List<String> command = (['git'] + arguments).collect { it.toString() }
        Process process = new ProcessBuilder(command).directory(directory).redirectErrorStream(true).start()
        byte[] output = process.inputStream.bytes
        if (process.waitFor() != 0) fail("Git input acquisition failed (${command.join(' ')}): ${new String(output, StandardCharsets.UTF_8).trim()}")
        output
    }
    private static void fail(String message) { throw new IllegalArgumentException(message) }
}
