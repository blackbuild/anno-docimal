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

import spock.lang.Issue
import spock.lang.See
import spock.lang.Specification
import spock.lang.Tag

import java.nio.file.Files

@Issue('71')
@Tag('documentary')
@See('https://github.com/blackbuild/anno-docimal/blob/master/docs/versioned-documentation.md#local-rehearsal')
class VersionedDocumentationDocumentaryTest extends Specification {

    def 'demonstrates an immutable exact-site rehearsal'() {
        given: 'a clean checkout at one exact commit and generated module Javadocs'
        File checkout = Files.createTempDirectory('documentation-checkout-').toFile()
        git(checkout, ['init'])
        git(checkout, ['config', 'user.email', 'fixtures@example.invalid'])
        git(checkout, ['config', 'user.name', 'Documentation fixtures'])
        new File(checkout, 'docs').mkdirs()
        new File(checkout, 'README.md').text = '# AnnoDocimal\n'
        new File(checkout, 'CHANGES.md').text = '# Changes\n'
        new File(checkout, 'docs/usage.md').text = '# Usage\n'
        git(checkout, ['add', '.'])
        git(checkout, ['commit', '-m', 'fixture documentation'])
        String revision = git(checkout, ['rev-parse', 'HEAD']).trim()
        File javadocs = Files.createTempDirectory('documentation-javadocs-').toFile()
        ['index.html', 'allclasses-index.html', 'stylesheet.css'].each { new File(javadocs, it).text = '<title>Annotations API</title>' }
        File output = Files.createTempDirectory('documentation-output-').toFile()

        when: 'the exact release candidate is rendered without publishing'
        VersionedDocumentationRenderer.render(
                objectDirectory: checkout,
                outputDirectory: output,
                revision: revision,
                rendererRevision: revision,
                version: '1.0.0-rc.1',
                stage: 'public-rc',
                javadocInputDirectories: ['anno-docimal-annotations': javadocs])

        then: 'the output makes its immutable source and public API visible'
        new File(output, '1.0.0-rc.1/index.md').text.contains('immutable documentation snapshot')
        new File(output, '1.0.0-rc.1/api/anno-docimal-annotations/index.html').file
        new File(output, '1.0.0-rc.1/source-manifest.json').text.contains(revision)
    }

    private static String git(File directory, List<String> arguments) {
        Process process = new ProcessBuilder((['git'] + arguments) as List<String>)
                .directory(directory).redirectErrorStream(true).start()
        String output = process.inputStream.text
        assert process.waitFor() == 0: output
        output
    }
}
