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

import java.io.FileFilter
import java.nio.charset.StandardCharsets

/** Renders the mutable Pages root from the public, manifest-bound documentation ledger. */
class RootDocumentationDiscoveryRenderer {

    private static final Set<String> PUBLIC_STATUSES = ['current', 'public-rc'] as Set
    private static final String VERSION_PATTERN = /\d+\.\d+\.\d+(?:-rc\.[1-9]\d*)?/

    static void render(File pagesDirectory) {
        if (!pagesDirectory.directory) throw new IllegalArgumentException("Pages directory is required: $pagesDirectory")
        List<Map<String, String>> snapshots = publicSnapshots(pagesDirectory)
        List<String> routes = []
        if (new File(pagesDirectory, 'stable/index.html').file) routes << '<li><a href="/anno-docimal/stable/">Stable documentation (proof-gated)</a></li>'
        if (new File(pagesDirectory, 'preview/index.html').file) routes << '<li><a href="/anno-docimal/preview/">Preview documentation (proof-gated)</a></li>'
        if (new File(pagesDirectory, 'archive/index.html').file) routes << '<li><a href="/anno-docimal/archive/">Documentation archive</a></li>'
        String entries = snapshots ? '<h2>Published snapshots</h2><ul>' + snapshots.collect { snapshot ->
            "<li><a href=\"/anno-docimal/${snapshot.version}/\">${snapshot.version}</a> (${snapshot.status})</li>"
        }.join('\n') + '</ul>' : '<p>No public documentation snapshot has been published yet.</p>'
        new File(pagesDirectory, 'index.html').setText("""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>AnnoDocimal documentation</title></head><body><h1>AnnoDocimal documentation</h1>
<p>This discovery page links only to public, proof-gated documentation routes. Pending release evidence is deliberately unlisted.</p><ul>
${routes.join('\n')}
</ul>
${entries}
</body></html>
""", StandardCharsets.UTF_8.name())
    }

    private static List<Map<String, String>> publicSnapshots(File pagesDirectory) {
        File statusDirectory = new File(pagesDirectory, 'status')
        if (!statusDirectory.directory) return []
        statusDirectory.listFiles({ File file -> file.file && file.name.endsWith('.json') } as FileFilter)
                .collect { File record -> readRecord(record, pagesDirectory) }
                .findAll()
                .sort { Map<String, String> snapshot -> versionKey(snapshot.version) }
    }

    private static Map<String, String> readRecord(File record, File pagesDirectory) {
        try {
            Map value = new JsonSlurper().parse(record) as Map
            String version = value.version as String
            String status = value.status as String
            if (value.schemaVersion != 1 || !PUBLIC_STATUSES.contains(status) || !(version ==~ VERSION_PATTERN)) return null
            File manifest = new File(pagesDirectory, "$version/source-manifest.json")
            if (!manifest.file) return null
            Map manifestValue = new JsonSlurper().parse(manifest) as Map
            Map documentation = manifestValue.documentation as Map
            documentation.version == version && documentation.status == status ? [version: version, status: status] : null
        } catch (Exception ignored) {
            null
        }
    }

    private static List<Integer> versionKey(String version) {
        def match = (version =~ /(\d+)\.(\d+)\.(\d+)(?:-rc\.(\d+))?/)
        match.matches()
        [match[0][1], match[0][2], match[0][3], match[0][4] ? 0 : 1, match[0][4] ?: '0'].collect { it as Integer }
    }
}
