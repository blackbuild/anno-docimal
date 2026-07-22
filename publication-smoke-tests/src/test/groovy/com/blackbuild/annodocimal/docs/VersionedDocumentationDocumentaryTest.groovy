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

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import spock.lang.Issue
import spock.lang.See
import spock.lang.Specification
import spock.lang.Tag

import java.nio.file.Files
import java.security.MessageDigest

@Issue('71')
@Tag('documentary')
@See('https://github.com/blackbuild/anno-docimal/blob/master/docs/versioned-documentation.md#local-presentation-rehearsal')
class VersionedDocumentationDocumentaryTest extends Specification {

    def 'demonstrates an immutable exact-site rehearsal'() {
        given: 'a clean checkout at one exact commit and generated module Javadocs'
        File checkout = Files.createTempDirectory('documentation-checkout-').toFile()
        git(checkout, ['init'])
        git(checkout, ['config', 'user.email', 'fixtures@example.invalid'])
        git(checkout, ['config', 'user.name', 'Documentation fixtures'])
        new File(checkout, 'docs').mkdirs()
        new File(checkout, 'README.md').text = '# AnnoDocimal\n\n[Usage](docs/usage.md#details)\n'
        new File(checkout, 'CHANGES.md').text = '# Changes\n'
        new File(checkout, 'docs/usage.md').text = '# Usage\n\n## Details\n\n[Home](../README.md)\n'
        new File(checkout, 'img').mkdirs()
        byte[] logo = 'documentary-logo'.bytes
        new File(checkout, 'img/annodocimallogo.png').bytes = logo
        new File(checkout, 'docs/branding').mkdirs()
        new File(checkout, 'docs/branding/annodocimal-current.json').text = """{
  \"identity\": \"AnnoDocimal\",
  \"season\": \"Current identity\",
  \"logo\": \"img/annodocimallogo.png\",
  \"altText\": \"AnnoDocimal logo\",
  \"sha256\": \"${MessageDigest.getInstance('SHA-256').digest(logo).encodeHex()}\",
  \"approval\": \"documentary fixture\"
}
"""
        new File(checkout, '.gitignore').text = '.gradle/\nbuild/\n'
        new File(checkout, 'settings.gradle').text = "rootProject.name = 'documentation-rehearsal'\n"
        git(checkout, ['add', '.'])
        git(checkout, ['commit', '-m', 'fixture documentation'])
        String revision = git(checkout, ['rev-parse', 'HEAD']).trim()
        File javadocs = Files.createTempDirectory('documentation-javadocs-').toFile()
        ['index.html', 'allclasses-index.html', 'stylesheet.css'].each { new File(javadocs, it).text = '<title>Annotations API</title>' }
        File output = Files.createTempDirectory('documentation-output-').toFile()
        File buildLogic = new File(RenderVersionedDocumentationTask.protectionDomain.codeSource.location.toURI())
        File commonmark = new File(Parser.protectionDomain.codeSource.location.toURI())
        File tables = new File(TablesExtension.protectionDomain.codeSource.location.toURI())
        new File(checkout, 'build.gradle').text = rehearsalBuild(buildLogic, commonmark, tables, javadocs)
        git(checkout, ['add', 'build.gradle'])
        git(checkout, ['commit', '-m', 'configure documentation rehearsal'])
        revision = git(checkout, ['rev-parse', 'HEAD']).trim()

        when: 'the exact release candidate is rendered through the documented Gradle task without publishing'
        def result = GradleRunner.create()
                .withProjectDir(checkout)
                .withArguments(
                        'renderVersionedDocumentation',
                        "-PdocumentationRevision=$revision",
                        "-PdocumentationRendererRevision=$revision",
                        '-PdocumentationVersion=1.0.0-rc.1',
                        '-PdocumentationStatus=public-rc',
                        '-PdocumentationBrandingManifest=docs/branding/annodocimal-current.json',
                        '-PdocumentationCurrentBrandingManifest=docs/branding/annodocimal-current.json',
                        "-PdocumentationOutputDirectory=${output.absolutePath}")
                .build()

        then: 'the output makes its immutable source, status, and public API visible'
        result.task(':renderVersionedDocumentation').outcome == TaskOutcome.SUCCESS
        new File(output, '1.0.0-rc.1/index.html').text.contains('1.0.0-rc.1 · release candidate')
        new File(output, '1.0.0-rc.1/index.html').text.contains('docs/usage/#details')
        new File(output, '1.0.0-rc.1/api/anno-docimal-annotations/index.html').file
        new File(output, '1.0.0-rc.1/source-manifest.json').text.contains(revision)
        new File(output, '1.0.0-rc.1/status/index.html').text.contains('successor status record')

        when: 'a README-only historical revision is rendered without branding or current Javadocs'
        javadocs.deleteDir()
        new File(checkout, 'README.md').text = '# Historic AnnoDocimal\n'
        git(checkout, ['rm', '-r', 'docs', 'CHANGES.md'])
        git(checkout, ['add', 'README.md'])
        git(checkout, ['commit', '-m', 'historic README-only documentation'])
        String archiveRevision = git(checkout, ['rev-parse', 'HEAD']).trim()
        File archiveOutput = Files.createTempDirectory('documentation-archive-output-').toFile()
        def archiveResult = GradleRunner.create()
                .withProjectDir(checkout)
                .withArguments(
                        'renderVersionedDocumentation',
                        "-PdocumentationRevision=$archiveRevision",
                        "-PdocumentationRendererRevision=$archiveRevision",
                        '-PdocumentationVersion=0.9.0',
                        '-PdocumentationStatus=archived',
                        "-PdocumentationOutputDirectory=${archiveOutput.absolutePath}")
                .build()

        then: 'the Gradle seam preserves the explicit legacy archive contract'
        archiveResult.task(':renderVersionedDocumentation').outcome == TaskOutcome.SUCCESS
        new File(archiveOutput, 'archive/0.9.0/index.html').text.contains('Archived (legacy)')
        !new File(archiveOutput, 'archive/0.9.0/api').exists()
        !new File(archiveOutput, 'archive/0.9.0/assets/branding').exists()
    }

    private static String rehearsalBuild(File buildLogic, File commonmark, File tables, File javadocs) {
        String buildLogicPath = gradleString(buildLogic)
        String commonmarkPath = gradleString(commonmark)
        String tablesPath = gradleString(tables)
        String javadocPath = gradleString(javadocs)
        """buildscript {
    dependencies {
        classpath files('$buildLogicPath', '$commonmarkPath', '$tablesPath')
    }
}

import com.blackbuild.annodocimal.docs.RenderVersionedDocumentationTask

tasks.register('renderVersionedDocumentation', RenderVersionedDocumentationTask) {
    def documentationStatus = providers.gradleProperty('documentationStatus')
    def documentationJavadocs = documentationStatus.map { value ->
        value == 'archived' ? [:] : ['anno-docimal-annotations': '$javadocPath']
    }
    revision.set(providers.gradleProperty('documentationRevision'))
    rendererRevision.set(providers.gradleProperty('documentationRendererRevision'))
    documentationVersion.set(providers.gradleProperty('documentationVersion'))
    status.set(documentationStatus)
    releaseStage.set(providers.gradleProperty('documentationReleaseStage'))
    brandingManifestPath.set(providers.gradleProperty('documentationBrandingManifest'))
    currentBrandingManifestPath.set(providers.gradleProperty('documentationCurrentBrandingManifest'))
    successorOf.set(providers.gradleProperty('documentationSuccessorOf'))
    objectDirectory.set(layout.projectDirectory)
    outputDirectory.set(layout.dir(providers.gradleProperty('documentationOutputDirectory').map { file(it) }))
    javadocInputDirectories.set(documentationJavadocs)
    javadocInputs.from(documentationJavadocs.map { it.values() })
}
"""
    }

    private static String gradleString(File file) {
        file.absolutePath.replace('\\', '\\\\').replace("'", "\\'")
    }

    private static String git(File directory, List<String> arguments) {
        Process process = new ProcessBuilder((['git'] + arguments) as List<String>)
                .directory(directory).redirectErrorStream(true).start()
        String output = process.inputStream.text
        assert process.waitFor() == 0: output
        output
    }
}
