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

import org.commonmark.Extension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Code
import org.commonmark.node.Heading
import org.commonmark.node.Node
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.AttributeProvider
import org.commonmark.renderer.html.AttributeProviderContext
import org.commonmark.renderer.html.AttributeProviderFactory
import org.commonmark.renderer.html.HtmlRenderer

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.text.Normalizer

/** Converts authored Markdown into complete dependency-free AnnoDocimal HTML pages. */
class StaticDocumentationPageRenderer {

    static final String CONTRACT_ID = 'commonmark-java-static-html-v1'
    static final String COMMONMARK_VERSION = '0.28.0'
    static final String SITE_CSS = '''
:root { color-scheme: light dark; --bg: #fff; --fg: #202124; --muted: #5f6368; --line: #d7dce1; --accent: #5b2ca0; --code: #f3f4f6; }
@media (prefers-color-scheme: dark) { :root { --bg: #17181a; --fg: #eceff1; --muted: #b0b6bc; --line: #42464b; --accent: #c7a6ff; --code: #25282c; } }
* { box-sizing: border-box; }
body { margin: 0; color: var(--fg); background: var(--bg); font: 16px/1.6 system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
a { color: var(--accent); }
.site-header { border-bottom: 1px solid var(--line); }
.site-header__inner, .layout, .site-footer { width: min(1120px, calc(100% - 2rem)); margin: 0 auto; }
.site-header__inner { display: flex; gap: 1rem; align-items: center; justify-content: space-between; padding: .8rem 0; }
.brand { display: inline-flex; gap: .65rem; align-items: center; color: var(--fg); font-weight: 700; text-decoration: none; }
.brand img { width: 2rem; height: 2rem; object-fit: contain; }
.version-badge { color: var(--muted); font-size: .9rem; }
.layout { display: grid; grid-template-columns: minmax(13rem, 18rem) minmax(0, 1fr); gap: 2rem; padding: 1.5rem 0 3rem; }
.sidebar { border-right: 1px solid var(--line); padding-right: 1rem; }
.sidebar ul { padding-left: 1.2rem; }
.content { min-width: 0; }
.status-banner { border: 1px solid var(--line); border-left: .35rem solid var(--accent); padding: .75rem 1rem; margin-bottom: 1.5rem; }
pre, code { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
code { background: var(--code); padding: .1em .25em; border-radius: .2rem; }
pre { overflow: auto; padding: 1rem; background: var(--code); border-radius: .3rem; }
pre code { padding: 0; }
table { border-collapse: collapse; display: block; overflow-x: auto; }
th, td { border: 1px solid var(--line); padding: .4rem .65rem; text-align: left; }
img { max-width: 100%; height: auto; }
.site-footer { border-top: 1px solid var(--line); padding: 1rem 0 2rem; color: var(--muted); }
@media (max-width: 760px) { .layout { grid-template-columns: 1fr; } .sidebar { border-right: 0; border-bottom: 1px solid var(--line); padding: 0 0 1rem; } }
'''.stripIndent().trim() + '\n'

    private static final List<Extension> EXTENSIONS = [TablesExtension.create()].asImmutable()

    static String render(Map<String, ?> inputs) {
        String markdown = inputs.markdown?.toString() ?: ''
        String sourcePath = inputs.sourcePath.toString()
        String outputPath = inputs.outputPath.toString()
        Map<String, String> pageOutputs = new TreeMap<>()
        pageOutputs.putAll(inputs.pageOutputs as Map<String, String>)

        Node document = parser().parse(markdown)
        Map<Node, String> headingIds = assignHeadingIds(document)
        rewriteLinks(document, sourcePath, outputPath, pageOutputs)
        String content = htmlRenderer(headingIds).render(document)
        String title = inputs.title?.toString() ?: firstHeading(document) ?: 'AnnoDocimal documentation'
        String version = inputs.version.toString()
        String status = inputs.status.toString()
        String statusLabel = inputs.statusLabel.toString()
        String notice = inputs.notice.toString()
        String homeLink = relativeUrl(outputPath, 'index.html')
        String apiLink = relativeUrl(outputPath, 'api/index.html')
        String statusLink = relativeUrl(outputPath, 'status/index.html')
        String changesLink = relativeUrl(outputPath, 'CHANGES/index.html')
        String cssLink = relativeUrl(outputPath, 'assets/site.css')
        boolean hasApi = inputs.hasApi != false
        boolean hasChanges = pageOutputs.containsKey('CHANGES.md')
        String navigation = '<ul><li><a href="' + escapeAttribute(homeLink) + '">Documentation</a></li>' +
                (hasApi ? '<li><a href="' + escapeAttribute(apiLink) + '">API reference</a></li>' : '') +
                (hasChanges ? '<li><a href="' + escapeAttribute(changesLink) + '">Change history</a></li>' : '') + '</ul>'
        String logoPath = inputs.logoPath?.toString()
        String logo = logoPath ? "<img src=\"${escapeAttribute(relativeUrl(outputPath, logoPath))}\" alt=\"${escapeAttribute(inputs.logoAltText?.toString() ?: 'AnnoDocimal')}\">" : ''
        String repositoryRevision = inputs.repositoryRevision?.toString()
        String repositorySourcePath = inputs.repositorySourcePath?.toString()
        String source = repositoryRevision && repositorySourcePath ?
                "<a href=\"https://github.com/blackbuild/anno-docimal/blob/${escapeAttribute(repositoryRevision)}/${escapeAttribute(repositorySourcePath)}\">Exact source</a>." : ''

        """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escapeHtml(title)} — AnnoDocimal ${escapeHtml(version)}</title>
  <link rel="stylesheet" href="${escapeAttribute(cssLink)}">
</head>
<body>
  <header class="site-header"><div class="site-header__inner">
    <a class="brand" href="${escapeAttribute(homeLink)}">${logo}<span>AnnoDocimal</span></a>
    <span class="version-badge">${escapeHtml(version)} · ${escapeHtml(statusLabel)}</span>
  </div></header>
  <div class="layout">
    <nav class="sidebar" aria-label="Documentation">${navigation}</nav>
    <main class="content">
      <aside class="status-banner" data-status="${escapeAttribute(status)}">${escapeHtml(notice)} <a href="${escapeAttribute(statusLink)}">Version status</a>.</aside>
      ${content}
    </main>
  </div>
  <footer class="site-footer"><p>AnnoDocimal documentation rendered from exact repository sources. ${source}</p></footer>
</body>
</html>
"""
    }

    static String pageOutputPath(String sourcePath) {
        if (sourcePath == 'README.md') return 'index.html'
        String withoutExtension = sourcePath.substring(0, sourcePath.length() - '.md'.length())
        "$withoutExtension/index.html"
    }

    static String relativeUrl(String fromOutputPath, String targetOutputPath) {
        Path fromDirectory = Paths.get(fromOutputPath).parent ?: Paths.get('')
        String result = fromDirectory.relativize(Paths.get(targetOutputPath)).toString().replace('\\', '/')
        if (targetOutputPath.endsWith('/index.html')) result = result.substring(0, result.length() - 'index.html'.length())
        else if (targetOutputPath == 'index.html') result = result == 'index.html' ? './' : result.substring(0, result.length() - 'index.html'.length())
        result ?: './'
    }

    static String slug(String value) {
        String normalized = Normalizer.normalize(value ?: '', Normalizer.Form.NFC).toLowerCase(Locale.ROOT)
        normalized.replaceAll(/[^\p{L}\p{N}\p{M}_\- ]/, '').replaceAll(/\s+/, '-').replaceAll(/-+/, '-').replaceAll(/^-|-$/, '')
    }

    private static Parser parser() { Parser.builder().extensions(EXTENSIONS).build() }

    private static HtmlRenderer htmlRenderer(Map<Node, String> headingIds) {
        HtmlRenderer.builder().extensions(EXTENSIONS).escapeHtml(true).sanitizeUrls(true)
                .attributeProviderFactory(new HeadingIdAttributeProviderFactory(headingIds)).build()
    }

    private static Map<Node, String> assignHeadingIds(Node document) {
        Map<Node, String> ids = new IdentityHashMap<>()
        Map<String, Integer> occurrences = [:].withDefault { 0 }
        document.accept(new AbstractVisitor() {
            @Override
            void visit(Heading heading) {
                String base = slug(plainText(heading)) ?: 'section'
                int count = occurrences[base]++
                ids[heading] = count ? "$base-$count" : base
                visitChildren(heading)
            }
        })
        ids
    }

    private static void rewriteLinks(Node document, String sourcePath, String outputPath, Map<String, String> pageOutputs) {
        document.accept(new AbstractVisitor() {
            @Override
            void visit(org.commonmark.node.Link link) {
                link.destination = rewriteDestination(link.destination, sourcePath, outputPath, pageOutputs)
                visitChildren(link)
            }

            @Override
            void visit(org.commonmark.node.Image image) {
                image.destination = rewriteDestination(image.destination, sourcePath, outputPath, pageOutputs)
                visitChildren(image)
            }
        })
    }

    private static String rewriteDestination(String destination, String sourcePath, String outputPath, Map<String, String> pageOutputs) {
        if (!destination) return destination
        if (destination.startsWith('#')) return normalizeFragment(destination)
        if (destination.startsWith('/')) return destination
        if (destination.startsWith('//') || destination ==~ /(?i)[a-z][a-z0-9+.-]*:.*/) return destination
        List<Integer> suffixOffsets = [destination.indexOf('?'), destination.indexOf('#')].findAll { it >= 0 }
        int suffixAt = suffixOffsets.empty ? -1 : suffixOffsets.min()
        String path = suffixAt >= 0 ? destination.substring(0, suffixAt) : destination
        String suffix = suffixAt >= 0 ? destination.substring(suffixAt) : ''
        Path sourceParent = Paths.get(sourcePath).parent ?: Paths.get('')
        Path resolved = sourceParent.resolve(path).normalize()
        String sourceTarget = resolved.toString().replace('\\', '/')
        String outputTarget = pageOutputs[sourceTarget]
        if (!outputTarget && !sourceTarget.toLowerCase(Locale.ROOT).endsWith('.md')) outputTarget = pageOutputs[sourceTarget + '.md']
        if (resolved.startsWith('..')) return destination
        String rewritten = relativeUrl(outputPath, outputTarget ?: sourceTarget)
        if (path.endsWith('/') && !rewritten.endsWith('/')) rewritten += '/'
        rewritten + normalizeFragment(suffix)
    }

    private static String normalizeFragment(String suffix) {
        int hash = suffix.indexOf('#')
        if (hash < 0 || hash == suffix.length() - 1) return suffix
        String decoded = URLDecoder.decode(suffix.substring(hash + 1).replace('+', '%2B'), StandardCharsets.UTF_8)
        suffix.substring(0, hash + 1) + slug(decoded)
    }

    private static String firstHeading(Node document) {
        Node current = document.firstChild
        while (current) {
            if (current instanceof Heading) return plainText(current)
            current = current.next
        }
        null
    }

    private static String plainText(Node node) {
        StringBuilder value = new StringBuilder()
        Node child = node.firstChild
        while (child) {
            if (child instanceof Text || child instanceof Code) value.append(child.literal)
            else value.append(plainText(child))
            child = child.next
        }
        value.toString()
    }

    private static String escapeHtml(String value) { escapeAttribute(value).replace("'", '&#39;') }
    private static String escapeAttribute(String value) { (value ?: '').replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('"', '&quot;') }

    private static class HeadingIdAttributeProviderFactory implements AttributeProviderFactory {
        private final Map<Node, String> ids
        HeadingIdAttributeProviderFactory(Map<Node, String> ids) { this.ids = ids }
        @Override
        AttributeProvider create(AttributeProviderContext context) {
            { Node node, String tagName, Map<String, String> attributes ->
                if (ids.containsKey(node)) attributes.id = ids[node]
            } as AttributeProvider
        }
    }
}
