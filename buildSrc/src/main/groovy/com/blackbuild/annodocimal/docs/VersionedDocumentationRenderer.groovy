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

/** Renders one immutable AnnoDocimal static documentation and API snapshot from an exact Git commit. */
class VersionedDocumentationRenderer {

    static final String RENDERER_ID = 'annodocimal-buildsrc-static-html-v2'
    private static final Set<String> STATUSES = ['pending', 'public-rc', 'current', 'archived'] as Set
    private static final Set<String> RELEASE_STAGES = ['candidate', 'final'] as Set
    private static final String VERSION_PATTERN = /\d+\.\d+\.\d+(?:-rc\.[1-9]\d*)?/
    private static final String CURRENT_BRANDING_MANIFEST = 'docs/branding/annodocimal-current.json'
    private static final String DOCUMENTATION_ROOT = 'docs/user'
    private static final String HOME_PATH = 'docs/user/Home.md'
    private static final String SIDEBAR_PATH = 'docs/user/_Sidebar.md'
    private static final String FOOTER_PATH = 'docs/user/_Footer.md'
    private static final Set<String> RESERVED_OUTPUTS = ['source-manifest.json', 'status', 'api', 'assets/site.css'] as Set

    static void render(Map<String, ?> inputs) {
        File objectDirectory = directory(inputs, 'objectDirectory')
        File outputDirectory = file(inputs, 'outputDirectory')
        String revision = string(inputs, 'revision')
        String rendererRevision = string(inputs, 'rendererRevision')
        String version = string(inputs, 'version')
        String status = string(inputs, 'status')
        boolean rehearsal = inputs.rehearsal == true
        String releaseStage = optionalString(inputs, 'releaseStage')
        String brandingManifestPath = optionalString(inputs, 'brandingManifestPath')
        String currentBrandingManifestPath = optionalString(inputs, 'currentBrandingManifestPath')
        String successorOf = optionalString(inputs, 'successorOf')
        Map<String, File> javadocs = javadocs(inputs.javadocInputDirectories ?: [:], status != 'archived')

        requireFullSha(revision, 'revision')
        requireFullSha(rendererRevision, 'rendererRevision')
        if (rehearsal) {
            if (status != 'rehearsal' || version != rehearsalName())
                fail('The repository-local rehearsal must use its dedicated non-release identity')
            if (releaseStage || successorOf)
                fail('A rehearsal cannot declare a release stage or successor')
        } else {
            validateReleaseIdentity(version, status, releaseStage, successorOf)
        }
        if (git(objectDirectory, ['status', '--porcelain']).trim())
            fail('Documentation input worktree is dirty; render a checked-out immutable revision.')
        if (git(objectDirectory, ['rev-parse', '--verify', "${revision}^{commit}"]).trim() != revision)
            fail("Revision must resolve to the supplied full SHA: $revision")
        boolean hasApi = status != 'archived' && !javadocs.empty

        Map<String, ?> branding = status == 'archived' ? null :
                readBrandingManifest(objectDirectory, revision, brandingManifestPath)
        boolean finalProduct = !rehearsal && (status == 'current' || status == 'pending' && releaseStage == 'final')
        if (finalProduct && (brandingManifestPath != currentBrandingManifestPath || brandingManifestPath != CURRENT_BRANDING_MANIFEST))
            fail('A final documentation snapshot must use the current AnnoDocimal branding manifest')
        if (status != 'archived' && !hasApi)
            fail('Product documentation snapshots require Javadocs for every supported public Java API artifact')

        if (outputDirectory.exists() && outputDirectory.listFiles()?.length)
            fail("Output directory must be empty so an immutable snapshot cannot be overwritten: $outputDirectory")
        outputDirectory.mkdirs()
        String snapshotPath = status == 'archived' ? "archive/$version" : version
        File exactDirectory = new File(outputDirectory, snapshotPath)

        Map<String, byte[]> authoredMarkdown = new TreeMap<>()
        Map<String, byte[]> authoredAssets = new TreeMap<>()
        List<String> documentationPaths = git(objectDirectory, ['ls-tree', '-r', '--name-only', revision, '--', DOCUMENTATION_ROOT])
                .readLines().findAll { it }
        boolean legacyReadme = documentationPaths.empty && status == 'archived'
        if (documentationPaths.empty && !legacyReadme)
            fail("Revision $revision does not contain canonical user documentation under $DOCUMENTATION_ROOT/")
        documentationPaths.each { String sourcePath ->
            byte[] bytes = gitBytes(objectDirectory, ['show', "$revision:$sourcePath"])
            if (sourcePath.endsWith('.md')) authoredMarkdown[sourcePath] = bytes
            else authoredAssets[sourcePath] = bytes
        }
        if (legacyReadme) authoredMarkdown['README.md'] = gitBytes(objectDirectory, ['show', "$revision:README.md"])
        else if (!authoredMarkdown.containsKey(HOME_PATH)) fail("Canonical user documentation requires $HOME_PATH")

        byte[] sidebarMarkdown = authoredMarkdown.remove(SIDEBAR_PATH)
        byte[] footerMarkdown = authoredMarkdown.remove(FOOTER_PATH)

        Map<String, String> pageOutputs = new TreeMap<>()
        authoredMarkdown.each { String sourcePath, byte[] ignored ->
            String outputPath = StaticDocumentationPageRenderer.pageOutputPath(sourcePath)
            if (outputPath != 'index.html' && RESERVED_OUTPUTS.any { outputPath == it || outputPath.startsWith("$it/") })
                fail("Authored page collides with renderer-owned output: $sourcePath -> $outputPath")
            if (pageOutputs.containsValue(outputPath)) fail("Duplicate rendered output path: $outputPath")
            pageOutputs[sourcePath] = outputPath
        }

        Set<String> outputPaths = new TreeSet<>(pageOutputs.values())
        authoredAssets.each { String sourcePath, byte[] content ->
            String outputPath = StaticDocumentationPageRenderer.publicAssetOutputPath(sourcePath)
            if (RESERVED_OUTPUTS.any { outputPath == it || outputPath.startsWith("$it/") })
                fail("Authored asset collides with renderer-owned output: $sourcePath -> $outputPath")
            if (!outputPaths.add(outputPath)) fail("Duplicate rendered output path: $outputPath")
            pageOutputs[sourcePath] = outputPath
            write(exactDirectory, outputPath, content)
        }

        String logoTarget
        String logoAltText = 'AnnoDocimal'
        if (branding != null) {
            logoTarget = "assets/branding/${branding.logo.tokenize('/').last()}"
            if (!outputPaths.add(logoTarget)) fail("Branding logo collides with rendered output: $logoTarget")
            byte[] logo = gitBytes(objectDirectory, ['show', "$revision:${branding.logo}"])
            if (sha256(logo) != branding.sha256) fail("Branding manifest digest does not match ${branding.logo}")
            write(exactDirectory, logoTarget, logo)
            logoAltText = branding.altText
        }
        outputPaths.add('assets/site.css')
        write(exactDirectory, 'assets/site.css', StaticDocumentationPageRenderer.SITE_CSS.getBytes(StandardCharsets.UTF_8))

        Map<String, String> presentation = presentation(version, status, releaseStage, rehearsal)
        authoredMarkdown.each { String sourcePath, byte[] markdown ->
            String outputPath = pageOutputs[sourcePath]
            String html = StaticDocumentationPageRenderer.render(
                    markdown: new String(markdown, StandardCharsets.UTF_8), sourcePath: sourcePath,
                    outputPath: outputPath, pageOutputs: pageOutputs, version: presentation.version,
                    status: presentation.status, statusLabel: presentation.label, notice: presentation.notice,
                    hasApi: hasApi, logoPath: logoTarget, logoAltText: logoAltText,
                    repositoryRevision: revision, repositorySourcePath: sourcePath,
                    sidebarMarkdown: text(sidebarMarkdown), sidebarSourcePath: SIDEBAR_PATH,
                    footerMarkdown: text(footerMarkdown), footerSourcePath: FOOTER_PATH)
            write(exactDirectory, outputPath, html.getBytes(StandardCharsets.UTF_8))
        }

        if (hasApi) {
            javadocs.each { String module, File source ->
                verifyJavadocs(module, source)
                copyDirectory(source, new File(exactDirectory, "api/$module"), outputPaths, "api/$module")
            }
            writeGeneratedPage(exactDirectory, 'api/index.html', 'api/index.md', apiIndex(version, javadocs.keySet()),
                    pageOutputs, presentation, logoTarget, logoAltText, hasApi, sidebarMarkdown, footerMarkdown)
        }
        writeGeneratedPage(exactDirectory, 'status/index.html', 'status/index.md',
                versionStatus(version, status, releaseStage, rehearsal), pageOutputs, presentation, logoTarget, logoAltText,
                hasApi, sidebarMarkdown, footerMarkdown)

        if (!rehearsal && status != 'pending')
            write(outputDirectory, "status/${version}.json", canonicalJson(statusRecord(version, status, null)).getBytes(StandardCharsets.UTF_8))
        if (successorOf)
            write(outputDirectory, "status/${successorOf}.json", canonicalJson(statusRecord(successorOf, 'public-rc', version)).getBytes(StandardCharsets.UTF_8))

        Map<String, String> outputHashes = hashes(exactDirectory)
        outputHashes.remove('source-manifest.json')
        Map<String, ?> manifest = [
                schemaVersion: 2,
                renderer: [id: RENDERER_ID, revision: rendererRevision,
                           contract: StaticDocumentationPageRenderer.CONTRACT_ID,
                           commonmarkVersion: StaticDocumentationPageRenderer.COMMONMARK_VERSION,
                           extensions: ['gfm-tables'], rawHtml: 'escaped', unsafeUrls: 'sanitized'],
                source: legacyReadme ? [revision: revision, legacyHome: 'README.md'] :
                        [revision: revision, documentationRoot: DOCUMENTATION_ROOT, home: HOME_PATH],
                documentation: rehearsal ? [mode: 'rehearsal', localPath: snapshotPath,
                                               retainedPath: "rehearsal/${StaticDocumentationPageRenderer.CONTRACT_ID}/$revision"] :
                        [version: version, status: status] + (releaseStage ? [releaseStage: releaseStage] : [:]),
                branding: branding == null ? null : [manifest: brandingManifestPath, identity: branding.identity,
                                                     presentation: branding.presentation, altText: branding.altText,
                                                     approval: branding.approval, sourceAsset: branding.logo,
                                                     outputAsset: logoTarget, sha256: branding.sha256],
                javadocs: hasApi ? javadocs.collectEntries { String module, File source -> [(module): sha256Directory(source)] } : [:],
                generatedFiles: new TreeSet<>(outputHashes.keySet()),
                outputHashes: new TreeMap<>(outputHashes)
        ]
        write(exactDirectory, 'source-manifest.json', canonicalJson(manifest).getBytes(StandardCharsets.UTF_8))
        String selectorDescription = rehearsal ?
                "Non-release rehearsal for renderer contract ${StaticDocumentationPageRenderer.CONTRACT_ID} at exact revision $revision." :
                "This artifact contains the exact immutable $snapshotPath documentation tree."
        write(outputDirectory, 'index.html', StaticDocumentationPageRenderer.renderSelector(snapshotPath, selectorDescription)
                .getBytes(StandardCharsets.UTF_8))
        if (hashes(exactDirectory).keySet().any { it.endsWith('.md') && !it.startsWith('api/') })
            fail('The deployed release payload must not contain authored Markdown')
    }

    private static void validateReleaseIdentity(String version, String status, String releaseStage, String successorOf) {
        if (!(version ==~ VERSION_PATTERN)) fail("Documentation version must be exact: $version")
        if (!STATUSES.contains(status)) fail("Documentation status must be one of $STATUSES: $status")
        if (status == 'pending') {
            if (!RELEASE_STAGES.contains(releaseStage))
                fail("Pending documentation requires documentationReleaseStage to be one of $RELEASE_STAGES")
            if (releaseStage == 'candidate' && !version.contains('-rc.') || releaseStage == 'final' && version.contains('-rc.'))
                fail("Pending $releaseStage must agree with exact RC/final version $version")
        } else if (releaseStage) {
            fail('documentationReleaseStage is valid only with pending documentation')
        }
        if (status == 'public-rc' && !version.contains('-rc.')) fail("Public RC documentation must name an exact RC version: $version")
        if (status in ['current', 'archived'] && version.contains('-rc.')) fail("$status documentation must name an exact final version: $version")
        if (successorOf && (status != 'current' || !(successorOf ==~ /\d+\.\d+\.\d+-rc\.[1-9]\d*/)))
            fail('An RC successor record requires current final documentation and an exact RC predecessor')
    }

    private static void writeGeneratedPage(File exactDirectory, String outputPath, String sourcePath, String markdown,
                                           Map<String, String> pageOutputs, Map<String, String> presentation,
                                           String logoTarget, String logoAltText, boolean hasApi,
                                           byte[] sidebarMarkdown, byte[] footerMarkdown) {
        String html = StaticDocumentationPageRenderer.render(
                markdown: markdown, sourcePath: sourcePath, outputPath: outputPath, pageOutputs: pageOutputs,
                version: presentation.version, status: presentation.status, statusLabel: presentation.label,
                notice: presentation.notice, hasApi: hasApi, logoPath: logoTarget, logoAltText: logoAltText,
                sidebarMarkdown: text(sidebarMarkdown), sidebarSourcePath: SIDEBAR_PATH,
                footerMarkdown: text(footerMarkdown), footerSourcePath: FOOTER_PATH)
        write(exactDirectory, outputPath, html.getBytes(StandardCharsets.UTF_8))
    }

    private static Map<String, String> presentation(String version, String status, String releaseStage, boolean rehearsal) {
        if (rehearsal) return [version: 'local rehearsal', status: 'rehearsal', label: 'non-release rehearsal',
                               notice: 'Non-release rehearsal. This output is disposable and advances no release status or alias.']
        switch (status) {
            case 'archived': return [version: version, status: status, label: 'Archived (legacy)',
                                     notice: 'Archived (legacy). This historical documentation is retained for compatibility.']
            case 'public-rc': return [version: version, status: status, label: 'release candidate',
                                      notice: 'Prerelease warning. This release candidate is not stable.']
            case 'pending': return [version: version, status: status, label: "pending $releaseStage proof",
                                    notice: "Pending $releaseStage release evidence. This immutable proof is deployed but unlisted and advances no alias."]
            default: return [version: version, status: status, label: 'exact published release',
                             notice: 'This is an immutable exact published final release snapshot.']
        }
    }

    static String rehearsalName() { 'local-rehearsal' }

    private static String text(byte[] bytes) { bytes == null ? null : new String(bytes, StandardCharsets.UTF_8) }

    private static String apiIndex(String version, Set<String> modules) {
        "# AnnoDocimal $version API reference\n\n" + modules.collect { "- [$it]($it/)" }.join('\n') + '\n'
    }

    private static String versionStatus(String version, String status, String releaseStage, boolean rehearsal) {
        if (rehearsal) return '# Non-release documentation rehearsal\n\nThis output is disposable, unlisted, and advances no release status or alias.\n'
        "# AnnoDocimal $version version status\n\nStatus: **$status**.\n\n" +
                (releaseStage ? "Release stage: **$releaseStage**.\n\n" : '') +
                (status == 'archived' ? 'This archived legacy snapshot is immutable.\n' : status == 'public-rc' ?
                        "This public release candidate is a prerelease and is not stable. See the [successor status record](/anno-docimal/status/${version}.json).\n" :
                        status == 'pending' ? 'This immutable proof is deployed but unlisted. It is not a public release and advances no alias.\n' :
                                'This is an immutable exact published final release snapshot. Its presence does not advance stable or line aliases.\n')
    }

    private static Map<String, ?> statusRecord(String version, String status, String successor) {
        [schemaVersion: 1, version: version, status: status, successor: successor]
    }

    private static String canonicalJson(Map<String, ?> value) { JsonOutput.prettyPrint(JsonOutput.toJson(new TreeMap<>(value))) + '\n' }

    private static Map<String, ?> readBrandingManifest(File repository, String revision, String path) {
        if (!path) fail('Product documentation requires a versioned branding manifest')
        safePath(path)
        Object parsed
        try {
            parsed = new JsonSlurper().parseText(new String(gitBytes(repository, ['show', "$revision:$path"]), StandardCharsets.UTF_8))
        } catch (Exception exception) {
            fail("Branding manifest is malformed or absent at $path: ${exception.message}")
        }
        if (!(parsed instanceof Map)) fail("Branding manifest must be an object: $path")
        Map<String, ?> branding = parsed as Map<String, ?>
        ['identity', 'presentation', 'logo', 'altText', 'sha256', 'approval'].each { String field ->
            if (!(branding[field] instanceof String) || branding[field].trim().empty) fail("Branding manifest $path requires $field")
        }
        safePath(branding.logo)
        if (!(branding.sha256 ==~ /[0-9a-f]{64}/)) fail("Branding manifest $path has an invalid sha256")
        branding
    }

    private static void verifyJavadocs(String module, File directory) {
        ['index.html', 'allclasses-index.html', 'stylesheet.css'].each { String name ->
            if (!new File(directory, name).file) fail("Javadoc input for $module has no $name: $directory")
        }
        ['index.html', 'allclasses-index.html'].each { String name ->
            File page = new File(directory, name)
            Matcher matcher = (page.getText(StandardCharsets.UTF_8.name()) =~ /(?:href|src)="([^"#?]+)[^"]*"/)
            matcher.each { match ->
                String destination = match[1]
                if (!(destination ==~ /(?i)(https?|mailto|javascript):.*/) && !new File(page.parentFile, destination).file)
                    fail("Javadoc navigation is unresolved for $module: $name -> $destination")
            }
        }
    }

    private static void copyDirectory(File source, File target, Set<String> paths, String prefix) {
        source.eachFileRecurse { File child ->
            if (!child.file) return
            String relative = source.toPath().relativize(child.toPath()).toString().replace(File.separatorChar, '/' as char)
            String path = "$prefix/$relative"
            safePath(path)
            if (!paths.add(path)) fail("Javadoc path collides with rendered documentation: $path")
            write(target, relative, child.bytes)
        }
    }

    private static boolean hasGitPath(File repository, String revision, String path) {
        new ProcessBuilder(['git', 'cat-file', '-e', "$revision:$path"].collect { it.toString() } as List<String>)
                .directory(repository).start().waitFor() == 0
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
        root.eachFileRecurse { File child ->
            if (child.file) values[root.toPath().relativize(child.toPath()).toString().replace(File.separatorChar, '/' as char)] = sha256(child.bytes)
        }
        values
    }

    private static String sha256Directory(File directory) {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        List<File> files = []
        directory.eachFileRecurse { File child -> if (child.file) files << child }
        files.sort { File child -> directory.toPath().relativize(child.toPath()).toString() }.each { File child ->
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
