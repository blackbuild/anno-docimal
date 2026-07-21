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
import groovy.json.JsonSlurper

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.util.regex.Matcher

/** Renders one immutable AnnoDocimal documentation and API snapshot from an exact Git commit. */
class VersionedDocumentationRenderer {

    static final String RENDERER_ID = 'annodocimal-buildsrc-vd-1'
    private static final Set<String> STAGES = ['archived', 'public-rc', 'release'] as Set
    private static final String VERSION_PATTERN = /\d+\.\d+\.\d+(?:-rc\.[1-9]\d*)?/
    private static final String CURRENT_BRANDING_MANIFEST = 'docs/branding/annodocimal-current.json'
    private static final Set<String> RESERVED_PATHS = ['source-manifest.json', 'version-status.md'] as Set

    static void render(Map<String, ?> inputs) {
        File objectDirectory = directory(inputs, 'objectDirectory')
        File outputDirectory = file(inputs, 'outputDirectory')
        String revision = string(inputs, 'revision')
        String rendererRevision = string(inputs, 'rendererRevision')
        String version = string(inputs, 'version')
        String stage = string(inputs, 'stage')
        String brandingManifestPath = optionalString(inputs, 'brandingManifestPath')
        String currentBrandingManifestPath = optionalString(inputs, 'currentBrandingManifestPath')
        String successorOf = optionalString(inputs, 'successorOf')
        Map<String, File> javadocs = javadocs(inputs.javadocInputDirectories ?: [:], stage != 'archived')

        requireFullSha(revision, 'revision')
        requireFullSha(rendererRevision, 'rendererRevision')
        if (!(version ==~ VERSION_PATTERN)) fail("Documentation version must be exact: $version")
        if (!STAGES.contains(stage)) fail("Documentation stage must be one of $STAGES: $stage")
        if (stage == 'public-rc' != version.contains('-rc.'))
            fail("$stage must agree with exact RC/final version $version")
        if (stage == 'archived' && version.contains('-rc.'))
            fail("Archived documentation must name an exact final version: $version")
        if (stage == 'archived' && successorOf) fail('Archived documentation cannot update an RC successor record')
        if (successorOf && (stage != 'release' || !(successorOf ==~ /\d+\.\d+\.\d+-rc\.[1-9]\d*/)))
            fail('An RC successor record requires a final release and an exact RC predecessor')
        if (git(objectDirectory, ['status', '--porcelain']).trim())
            fail('Documentation input worktree is dirty; render a checked-out immutable revision.')
        if (git(objectDirectory, ['rev-parse', '--verify', "${revision}^{commit}"]).trim() != revision)
            fail("Revision must resolve to the supplied full SHA: $revision")
        boolean hasApi = stage != 'archived' && !javadocs.empty

        Map<String, ?> branding = stage == 'archived' ? null : readBrandingManifest(objectDirectory, revision, brandingManifestPath)
        if (stage == 'release' && (brandingManifestPath != currentBrandingManifestPath || brandingManifestPath != CURRENT_BRANDING_MANIFEST))
            fail('A final documentation snapshot must use the current AnnoDocimal branding manifest')
        if (stage != 'archived' && !hasApi)
            fail('RC and final snapshots require Javadocs for every supported public Java API artifact')

        if (outputDirectory.exists() && outputDirectory.listFiles()?.length)
            fail("Output directory must be empty so an immutable snapshot cannot be overwritten: $outputDirectory")
        outputDirectory.mkdirs()
        String snapshotPath = stage == 'archived' ? "archive/$version" : version
        File exactDirectory = new File(outputDirectory, snapshotPath)
        Set<String> outputPaths = new TreeSet<>()
        copyGitTree(objectDirectory, revision, version, stage, snapshotPath, branding, hasApi, 'docs', exactDirectory, outputPaths, stage != 'archived')
        copyGitFile(objectDirectory, revision, version, stage, snapshotPath, branding, hasApi, 'README.md', exactDirectory, 'index.md', outputPaths)
        if (hasGitPath(objectDirectory, revision, 'CHANGES.md'))
            copyGitFile(objectDirectory, revision, version, stage, snapshotPath, branding, hasApi, 'CHANGES.md', exactDirectory, 'CHANGES.md', outputPaths)

        if (hasApi) {
            javadocs.each { module, directory ->
                verifyJavadocs(module, directory)
                copyDirectory(directory, new File(exactDirectory, "api/$module"), outputPaths, "api/$module")
            }
            outputPaths.add('api/index.md')
            write(exactDirectory, 'api/index.md', apiIndex(version, javadocs.keySet()).getBytes(StandardCharsets.UTF_8))
        }
        if (branding != null) copyBrandingAsset(objectDirectory, revision, branding, exactDirectory, outputPaths)
        outputPaths.add('version-status.md')
        write(exactDirectory, 'version-status.md', versionStatus(version, stage, snapshotPath).getBytes(StandardCharsets.UTF_8))
        write(outputDirectory, "status/${version}.json", canonicalJson(statusRecord(version, stage, null)).getBytes(StandardCharsets.UTF_8))
        if (successorOf) write(outputDirectory, "status/${successorOf}.json", canonicalJson(statusRecord(successorOf, 'public-rc', version)).getBytes(StandardCharsets.UTF_8))
        verifyLocalMarkdownLinks(exactDirectory)

        Map<String, String> hashes = hashes(exactDirectory).collectEntries { path, hash -> [("$snapshotPath/$path"): hash] }
        hashes.remove("$snapshotPath/source-manifest.json")
        Map<String, ?> manifest = [
                schemaVersion: 1,
                renderer     : [id: RENDERER_ID, revision: rendererRevision],
                source       : [revision: revision, documentationRoot: 'docs', readme: 'README.md', changes: 'CHANGES.md'],
                documentation: [version: version, stage: stage],
                branding     : branding == null ? null : [manifest: brandingManifestPath, identity: branding.identity, season: branding.season,
                                                         altText: branding.altText, approval: branding.approval, sourceAsset: branding.logo,
                                                         outputAsset: "assets/branding/${branding.logo.tokenize('/').last()}", sha256: branding.sha256],
                javadocs     : hasApi ? javadocs.collectEntries { module, directory -> [(module): sha256Directory(directory)] } : [:],
                generatedFiles: new TreeSet<>(hashes.keySet()),
                outputHashes : new TreeMap<>(hashes)
        ]
        write(exactDirectory, 'source-manifest.json', (JsonOutput.prettyPrint(JsonOutput.toJson(manifest)) + '\n').getBytes(StandardCharsets.UTF_8))
    }

    private static void copyGitTree(File repository, String revision, String version, String stage, String snapshotPath, Map<String, ?> branding, boolean hasApi, String root, File output, Set<String> paths, boolean required) {
        List<String> files = git(repository, ['ls-tree', '-r', '--name-only', revision, '--', root]).readLines().findAll { it }
        if (files.empty && required) fail("Revision $revision does not contain canonical documentation under $root/")
        files.each { source ->
            String target = source.substring(root.length() + 1)
            copyGitFile(repository, revision, version, stage, snapshotPath, branding, hasApi, source, output, "docs/$target", paths)
        }
    }

    private static void copyGitFile(File repository, String revision, String version, String stage, String snapshotPath, Map<String, ?> branding, boolean hasApi, String source, File output, String target, Set<String> paths) {
        safePath(target)
        if (RESERVED_PATHS.contains(target)) fail("Source path collides with renderer-owned output: $target")
        if (!paths.add(target)) fail("Duplicate rendered path: $target")
        byte[] content = gitBytes(repository, ['show', "$revision:$source"])
        if (target.endsWith('.md')) content = (chrome(version, stage, revision, snapshotPath, branding, hasApi, source, target) + new String(content, StandardCharsets.UTF_8)).bytes
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

    private static String chrome(String version, String stage, String revision, String snapshotPath, Map<String, ?> branding, boolean hasApi, String path, String target) {
        String archived = stage == 'archived' ? '> **Archived (legacy).** This historical documentation is retained for compatibility.\n' : ''
        String prerelease = stage == 'public-rc' ? "> **Prerelease warning.** This release candidate is not stable; see its [successor status record](${siteRootReference(snapshotPath, target, "status/${version}.json")}).\n" : ''
        String navigation = hasApi ? "[API reference](${apiReference(target)}) · " : ''
        String identity = branding == null ? '' : "Identity: **${branding.identity} — ${branding.season}** · [logo](${brandingReference(target, branding.logo.tokenize('/').last())}) · "
        "<!-- Generated by $RENDERER_ID. Do not edit this rendered copy. -->\n" +
                "> **AnnoDocimal $version — $stage immutable documentation snapshot.** $navigation$identity" +
                "Source: [$revision](https://github.com/blackbuild/anno-docimal/tree/$revision/$path).\n" + archived + prerelease + '\n'
    }

    private static String apiIndex(String version, Set<String> modules) {
        "# AnnoDocimal $version API reference\n\n" + modules.collect { "- [$it]($it/)" }.join('\n') + '\n'
    }

    private static String apiReference(String target) {
        '../' * (target.tokenize('/').size() - 1) + 'api/'
    }

    private static String brandingReference(String target, String asset) {
        '../' * (target.tokenize('/').size() - 1) + "assets/branding/$asset"
    }

    private static String siteRootReference(String snapshotPath, String target, String relative) {
        '../' * (snapshotPath.tokenize('/').size() + target.tokenize('/').size() - 1) + relative
    }

    private static void verifyJavadocs(String module, File directory) {
        ['index.html', 'allclasses-index.html', 'stylesheet.css'].each { name ->
            if (!new File(directory, name).file) fail("Javadoc input for $module has no $name: $directory")
        }
        ['index.html', 'allclasses-index.html'].each { name ->
            File page = new File(directory, name)
            Matcher matcher = (page.getText(StandardCharsets.UTF_8.name()) =~ /(?:href|src)="([^"#?]+)[^\"]*"/)
            matcher.each { match ->
                String destination = match[1]
                if (!(destination ==~ /(?i)(https?|mailto|javascript):.*/) && !new File(page.parentFile, destination).file)
                    fail("Javadoc navigation is unresolved for $module: $name -> $destination")
            }
        }
    }

    private static String versionStatus(String version, String stage, String snapshotPath) {
        "# AnnoDocimal $version version status\n\n" +
                "Stage: **$stage**.\n\n" +
                (stage == 'archived' ? 'This archived legacy snapshot is immutable.\n' : stage == 'public-rc'
                        ? "This public release candidate is a prerelease and is not stable. See the [successor status record](${siteRootReference(snapshotPath, 'version-status.md', "status/${version}.json")}).\n"
                        : 'This is an immutable final release snapshot.\n')
    }

    private static Map<String, ?> statusRecord(String version, String stage, String successor) {
        [schemaVersion: 1, version: version, stage: stage, successor: successor]
    }

    private static String canonicalJson(Map<String, ?> value) {
        JsonOutput.prettyPrint(JsonOutput.toJson(new TreeMap<>(value))) + '\n'
    }

    private static Map<String, ?> readBrandingManifest(File repository, String revision, String path) {
        if (!path) fail('RC and final documentation require a versioned branding manifest')
        safePath(path)
        Object parsed
        try {
            parsed = new JsonSlurper().parseText(new String(gitBytes(repository, ['show', "$revision:$path"]), StandardCharsets.UTF_8))
        } catch (Exception exception) {
            fail("Branding manifest is malformed or absent at $path: ${exception.message}")
        }
        if (!(parsed instanceof Map)) fail("Branding manifest must be an object: $path")
        Map<String, ?> branding = parsed as Map<String, ?>
        ['identity', 'season', 'logo', 'altText', 'sha256', 'approval'].each { field ->
            if (!(branding[field] instanceof String) || branding[field].trim().empty) fail("Branding manifest $path requires $field")
        }
        safePath(branding.logo)
        if (!(branding.sha256 ==~ /[0-9a-f]{64}/)) fail("Branding manifest $path has an invalid sha256")
        branding
    }

    private static void copyBrandingAsset(File repository, String revision, Map<String, ?> branding, File output, Set<String> paths) {
        String target = "assets/branding/${branding.logo.tokenize('/').last()}"
        if (!paths.add(target)) fail("Branding logo collides with rendered output: $target")
        byte[] asset = gitBytes(repository, ['show', "$revision:${branding.logo}"])
        if (sha256(asset) != branding.sha256) fail("Branding manifest digest does not match ${branding.logo}")
        write(output, target, asset)
    }

    private static boolean hasGitPath(File repository, String revision, String path) {
        new ProcessBuilder(['git', 'cat-file', '-e', "$revision:$path"].collect { it.toString() } as List<String>)
                .directory(repository).start().waitFor() == 0
    }

    private static void verifyLocalMarkdownLinks(File root) {
        root.eachFileRecurse { page ->
            if (page.file && page.name.endsWith('.md')) {
                Matcher matcher = (page.getText(StandardCharsets.UTF_8.name()) =~ /\[[^]]+\]\(([^)]+)\)/)
                matcher.each { match ->
                    String destination = match[1].trim()
                    if (!(destination.startsWith('#') || destination ==~ /(?i)(https?|mailto):.*/)) {
                        String path = destination.split('#', 2)[0]
                        if (path && !path.contains(' ') && !path.startsWith('/')) {
                            File target = new File(page.parentFile, path)
                            if (!target.file && !(target.directory && (new File(target, 'index.md').file || new File(target, 'index.html').file)))
                                fail("Rendered Markdown link is unresolved: ${root.toPath().relativize(page.toPath())} -> $destination")
                        }
                    }
                }
            }
        }
    }

    private static Map<String, File> javadocs(Object value, boolean required) {
        if (!(value instanceof Map)) fail('Javadoc inputs must be a module-to-directory map')
        if (value.empty && required) fail('Javadoc inputs must name every supported public Java API artifact')
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
        List<File> files = []
        directory.eachFileRecurse { child -> if (child.file) files << child }
        files.sort { child -> directory.toPath().relativize(child.toPath()).toString() }.each { child ->
            digest.update(directory.toPath().relativize(child.toPath()).toString().getBytes(StandardCharsets.UTF_8))
            digest.update(child.bytes)
        }
        digest.digest().encodeHex().toString()
    }

    private static String sha256(byte[] value) { MessageDigest.getInstance('SHA-256').digest(value).encodeHex().toString() }
    private static void write(File root, String relative, byte[] content) { File target = new File(root, relative); target.parentFile.mkdirs(); Files.write(target.toPath(), content) }
    private static File file(Map values, String key) { values[key] == null ? fail("$key is required") : values[key] instanceof File ? values[key] : new File(values[key].toString()) }
    private static File directory(Map values, String key) { File value = file(values, key); if (!value.directory) fail("$key must be a directory: $value"); value }
    private static String string(Map values, String key) { Object value = values[key]; if (!(value instanceof String) || value.trim().empty) fail("$key is required"); value.trim() }
    private static String optionalString(Map values, String key) { Object value = values[key]; value == null ? null : string(values, key) }
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
