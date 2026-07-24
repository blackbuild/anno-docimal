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

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.security.MessageDigest

/** Hermetic acceptance fixture for AnnoDocimal's immutable documentation renderer. */
abstract class VerifyVersionedDocumentationRendererTask extends DefaultTask {
    @TaskAction
    void verifyRendererContract() {
        File fixture = Files.createTempDirectory(temporaryDir.toPath(), 'versioned-documentation-').toFile()
        git(fixture, ['init']); git(fixture, ['config', 'user.email', 'fixtures@example.invalid']); git(fixture, ['config', 'user.name', 'Documentation fixtures'])
        new File(fixture, 'docs/user').mkdirs()
        new File(fixture, 'README.md').text = '# Repository pointer that is not public site input\n'
        new File(fixture, 'CHANGES.md').text = '# Changes\n'
        new File(fixture, 'docs/user/Home.md').text = '# AnnoDocimal\n\n[Usage details](usage.md#details)\n\n<script>alert("unsafe")</script>\n'
        new File(fixture, 'docs/user/usage.md').text = '# Usage\n\n## Details\n\n[Home](Home.md)\n\n[unsafe](javascript:alert(1))\n'
        new File(fixture, 'docs/user/supported-api.md').text = '# Supported API\n'
        new File(fixture, 'docs/user/_Sidebar.md').text = '- [Overview](Home.md)\n- [Usage](usage.md)\n'
        new File(fixture, 'docs/user/_Footer.md').text = 'AnnoDocimal fixture documentation.\n'
        new File(fixture, 'docs/versioned-documentation.md').text = '# Maintainer-only renderer notes\n'
        new File(fixture, 'img').mkdirs()
        byte[] logo = 'fixture-logo'.getBytes('UTF-8')
        new File(fixture, 'img/annodocimallogo.png').bytes = logo
        String logoDigest = MessageDigest.getInstance('SHA-256').digest(logo).encodeHex().toString()
        new File(fixture, 'docs/branding').mkdirs()
        new File(fixture, 'docs/branding/annodocimal-current.json').text = branding('AnnoDocimal', 'Current identity', logoDigest)
        new File(fixture, 'docs/branding/candidate.json').text = branding('AnnoDocimal candidate', 'Candidate identity', logoDigest)
        git(fixture, ['add', '.']); git(fixture, ['commit', '-m', 'fixture documentation'])
        String revision = git(fixture, ['rev-parse', 'HEAD']).trim()
        File javadoc = new File(temporaryDir, 'javadoc'); javadoc.mkdirs()
        ['index.html', 'allclasses-index.html', 'stylesheet.css'].each { new File(javadoc, it).text = '<title>API</title>' }
        new File(javadoc, 'legal').mkdirs()
        new File(javadoc, 'legal/jquery.md').text = 'Bundled Javadoc legal notice\n'
        File one = new File(temporaryDir, 'one'); File two = new File(temporaryDir, 'two')
        project.delete(one, two)
        render(fixture, one, revision, javadoc); render(fixture, two, revision, javadoc)
        assertTrue(new File(one, 'index.html').text.contains('1.0.0-rc.1/'), 'renderer-owned output selector links the exact tree')
        assertTrue(new File(one, '1.0.0-rc.1/index.html').text.contains('data-status="public-rc"'), 'renderer-owned HTML chrome')
        assertTrue(!new File(one, '1.0.0-rc.1/index.html').text.contains('Repository pointer that is not public site input'), 'root README is excluded from product snapshots')
        assertTrue(!new File(one, '1.0.0-rc.1/CHANGES').exists(), 'root change history is excluded from product snapshots')
        assertTrue(new File(one, '1.0.0-rc.1/index.html').text.contains('usage/#details'), 'Markdown deep links are rewritten')
        assertTrue(new File(one, '1.0.0-rc.1/index.html').text.contains('&lt;script&gt;'), 'authored HTML is escaped')
        assertTrue(!new File(one, '1.0.0-rc.1/usage/index.html').text.contains('href="javascript:'), 'unsafe URLs are sanitized')
        assertTrue(new File(one, '1.0.0-rc.1/usage/index.html').text.contains('aria-label="Documentation"'), 'authored sidebar is rendered in renderer-owned chrome')
        assertTrue(new File(one, '1.0.0-rc.1/usage/index.html').text.contains('AnnoDocimal fixture documentation.'), 'authored footer is rendered in renderer-owned chrome')
        assertTrue(!new File(one, '1.0.0-rc.1/docs/versioned-documentation').exists(), 'maintainer documentation is excluded from the public tree')
        assertTrue(!new File(one, '1.0.0-rc.1/_Sidebar').exists(), 'navigation fragments are not rendered as pages')
        assertTrue(new File(one, '1.0.0-rc.1/status/index.html').text.contains('successor status record'), 'fixed RC successor-status link')
        assertTrue(new File(one, '1.0.0-rc.1/api/anno-docimal-annotations/index.html').file, 'module Javadoc landing')
        File manifestFile = new File(one, '1.0.0-rc.1/source-manifest.json')
        Map manifest = new JsonSlurper().parse(manifestFile) as Map
        assertTrue(manifest.source.revision == revision, 'exact source evidence')
        assertTrue(manifest.source.documentationRoot == 'docs/user' && manifest.source.home == 'docs/user/Home.md', 'manifest records the canonical public source boundary')
        assertTrue(manifest.outputHashes['index.html'] == sha256(new File(one, '1.0.0-rc.1/index.html').bytes), 'manifest covers deployed HTML bytes')
        assertTrue(manifest.outputHashes['api/anno-docimal-annotations/index.html'] == sha256(new File(one, '1.0.0-rc.1/api/anno-docimal-annotations/index.html').bytes), 'manifest covers deployed Javadocs')
        assertTrue(manifest.outputHashes['api/anno-docimal-annotations/legal/jquery.md'] == sha256(new File(one, '1.0.0-rc.1/api/anno-docimal-annotations/legal/jquery.md').bytes), 'manifest covers Javadoc legal notices')
        assertTrue(!manifestFile.text.contains('status/1.0.0-rc.1.json'), 'immutable snapshot manifest excludes mutable status')
        List<File> deployedFiles = []
        new File(one, '1.0.0-rc.1').eachFileRecurse { File file -> if (file.file) deployedFiles << file }
        assertTrue(!deployedFiles.any { File file -> file.name.endsWith('.md') && !file.toPath().startsWith(new File(one, '1.0.0-rc.1/api').toPath()) }, 'deployed payload contains no authored Markdown')
        verifySite(one, '1.0.0-rc.1')
        File brokenFragment = new File(temporaryDir, 'broken-fragment')
        project.copy { from one; into brokenFragment }
        File brokenFragmentIndex = new File(brokenFragment, '1.0.0-rc.1/index.html')
        brokenFragmentIndex.text = brokenFragmentIndex.text.replace('#details', '#absent-fragment')
        expectSiteFailure { VerifyVersionedDocumentationRendererTask.verifySite(brokenFragment, '1.0.0-rc.1') }
        assertTrue(digest(one) == digest(two), 'repeat rendering is deterministic')
        new File(fixture, 'README.md').text = '# Historic AnnoDocimal\n'
        git(fixture, ['rm', '-r', 'docs', 'CHANGES.md'])
        git(fixture, ['add', 'README.md'])
        git(fixture, ['commit', '-m', 'historic README-only documentation'])
        String historicRevision = git(fixture, ['rev-parse', 'HEAD']).trim()
        File archive = new File(temporaryDir, 'archive')
        project.delete(archive)
        render(fixture, archive, historicRevision, javadoc,
                [version: '0.9.0', status: 'archived', javadocInputDirectories: [:],
                 brandingManifestPath: null, currentBrandingManifestPath: null])
        assertTrue(new File(archive, 'archive/0.9.0/index.html').text.contains('Archived (legacy)'), 'legacy archive chrome')
        assertTrue(!new File(archive, 'archive/0.9.0/docs').exists(), 'README-only archives do not require modern Markdown sources')
        assertTrue(!new File(archive, 'archive/0.9.0/CHANGES').exists(), 'README-only archives do not require modern change history')
        assertTrue(!new File(archive, 'archive/0.9.0/api').exists(), 'archives do not fabricate current Javadocs')
        verifySite(archive, 'archive/0.9.0')
        File finalRelease = new File(temporaryDir, 'final-release')
        project.delete(finalRelease)
        render(fixture, finalRelease, revision, javadoc,
                [version: '1.0.0', status: 'current', successorOf: '1.0.0-rc.1'])
        assertTrue(new File(finalRelease, 'status/1.0.0-rc.1.json').text.contains('"successor": "1.0.0"'), 'separate RC successor record')
        File candidate = new File(temporaryDir, 'candidate')
        project.delete(candidate)
        render(fixture, candidate, revision, javadoc, [brandingManifestPath: 'docs/branding/candidate.json'])
        assertTrue(new File(candidate, '1.0.0-rc.1/source-manifest.json').text.contains('Candidate identity'), 'candidate branding is allowed for public RCs')
        File pendingCandidate = new File(temporaryDir, 'pending-candidate')
        project.delete(pendingCandidate)
        render(fixture, pendingCandidate, revision, javadoc,
                [status: 'pending', releaseStage: 'candidate', brandingManifestPath: 'docs/branding/candidate.json'])
        assertTrue(new File(pendingCandidate, '1.0.0-rc.1/status/index.html').text.contains('deployed but unlisted'), 'pending evidence is explicitly unlisted')
        assertTrue(!new File(pendingCandidate, 'status/1.0.0-rc.1.json').exists(), 'pending evidence creates no public status record')
        assertTrue(new File(pendingCandidate, '1.0.0-rc.1/source-manifest.json').text.contains('Candidate identity'), 'candidate branding is allowed for pending candidates')
        File pendingFinal = new File(temporaryDir, 'pending-final')
        project.delete(pendingFinal)
        render(fixture, pendingFinal, revision, javadoc,
                [version: '1.0.0', status: 'pending', releaseStage: 'final'])
        assertTrue(new File(pendingFinal, '1.0.0/source-manifest.json').text.contains('"releaseStage": "final"'), 'pending final proof records its release stage')
        File rehearsal = new File(temporaryDir, 'rehearsal')
        project.delete(rehearsal)
        String rehearsalName = VersionedDocumentationRenderer.rehearsalName()
        render(fixture, rehearsal, revision, javadoc,
                [version: rehearsalName, status: 'rehearsal', rehearsal: true])
        assertTrue(new File(rehearsal, 'index.html').text.contains(rehearsalName), 'rehearsal selector identifies its contract and revision')
        assertTrue(new File(rehearsal, "$rehearsalName/index.html").text.contains('non-release rehearsal'), 'rehearsal uses explicit non-release chrome')
        Map rehearsalManifest = new JsonSlurper().parse(new File(rehearsal, "$rehearsalName/source-manifest.json")) as Map
        assertTrue(rehearsalManifest.documentation.localPath == rehearsalName, 'manifest records the local exact-tree path')
        assertTrue(rehearsalManifest.documentation.retainedPath == "rehearsal/${StaticDocumentationPageRenderer.CONTRACT_ID}/$revision",
                'manifest separates local review from retained platform evidence')
        assertTrue(!new File(rehearsal, 'status').exists(), 'rehearsal creates no release status record')
        verifySite(rehearsal, '')
        expectFailure { VerifyVersionedDocumentationRendererTask.render(fixture, new File(temporaryDir, 'pending-without-release-stage'), revision, javadoc,
                [status: 'pending']) }
        expectFailure { VerifyVersionedDocumentationRendererTask.render(fixture, new File(temporaryDir, 'release-stage-outside-pending'), revision, javadoc,
                [releaseStage: 'candidate']) }
        expectFailure { VerifyVersionedDocumentationRendererTask.render(fixture, new File(temporaryDir, 'candidate-final'), revision, javadoc,
                [version: '1.0.0', status: 'current', brandingManifestPath: 'docs/branding/candidate.json',
                 currentBrandingManifestPath: 'docs/branding/candidate.json']) }
        expectFailure { VerifyVersionedDocumentationRendererTask.render(fixture, new File(temporaryDir, 'missing-branding'), revision, javadoc,
                [brandingManifestPath: null]) }
        expectFailure { VerifyVersionedDocumentationRendererTask.render(fixture, new File(temporaryDir, 'candidate-final-version'), revision, javadoc,
                [version: '1.0.0', status: 'pending', releaseStage: 'candidate']) }
        expectFailure { VerifyVersionedDocumentationRendererTask.render(fixture, new File(temporaryDir, 'final-rc-version'), revision, javadoc,
                [status: 'pending', releaseStage: 'final']) }
        expectFailure {
            VersionedDocumentationRenderer.render(
                    objectDirectory: fixture, outputDirectory: new File(temporaryDir, 'bad'), revision: '0' * 40,
                    rendererRevision: revision, version: '1.0.0-rc.1', status: 'public-rc',
                    brandingManifestPath: 'docs/branding/annodocimal-current.json',
                    currentBrandingManifestPath: 'docs/branding/annodocimal-current.json',
                    javadocInputDirectories: ['api': javadoc])
        }
        new File(javadoc, 'index.html').text = '<a href="missing.html">broken API link</a>'
        expectFailure { VerifyVersionedDocumentationRendererTask.render(fixture, new File(temporaryDir, 'broken-javadocs'), revision, javadoc) }
        new File(fixture, 'dirty').text = 'dirty'
        expectFailure { VerifyVersionedDocumentationRendererTask.render(fixture, new File(temporaryDir, 'dirty-output'), revision, javadoc) }
    }

    private static void verifySite(File root, String entryPath) {
        DocumentationSiteServer server = new DocumentationSiteServer(root, entryPath)
        try { server.verify() } finally { server.close() }
    }
    private static void expectSiteFailure(Closure action) {
        try { action.call(); throw new AssertionError('Expected site verification failure') }
        catch (GradleException ignored) { }
    }

    private static void render(File fixture, File output, String revision, File javadoc, Map<String, ?> overrides = [:]) {
        Map<String, ?> inputs = [objectDirectory: fixture, outputDirectory: output, revision: revision,
                                 rendererRevision: revision, version: '1.0.0-rc.1', status: 'public-rc',
                                 brandingManifestPath: 'docs/branding/annodocimal-current.json',
                                 currentBrandingManifestPath: 'docs/branding/annodocimal-current.json',
                                 javadocInputDirectories: ['anno-docimal-annotations': javadoc]]
        inputs.putAll(overrides)
        VersionedDocumentationRenderer.render(inputs)
    }
    private static String branding(String identity, String presentation, String digest) {
        """{
  \"identity\": \"$identity\",
  \"presentation\": \"$presentation\",
  \"logo\": \"img/annodocimallogo.png\",
  \"altText\": \"AnnoDocimal logo\",
  \"sha256\": \"$digest\",
  \"approval\": \"fixture\"
}
"""
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
    private static String sha256(byte[] bytes) { MessageDigest.getInstance('SHA-256').digest(bytes).encodeHex().toString() }
    private static String git(File directory, List<String> arguments) { def process = new ProcessBuilder((['git'] + arguments) as List<String>).directory(directory).redirectErrorStream(true).start(); String output = process.inputStream.text; if (process.waitFor() != 0) throw new IllegalStateException(output); output }
    private static void assertTrue(boolean condition, String message) { if (!condition) throw new AssertionError(message) }
    private static void expectFailure(Closure action) { try { action.call(); throw new AssertionError('Expected renderer failure') } catch (IllegalArgumentException ignored) { } }
}
