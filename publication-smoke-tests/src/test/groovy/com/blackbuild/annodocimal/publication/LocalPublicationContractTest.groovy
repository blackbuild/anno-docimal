/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2026 Stephan Pauxberger (Gradle Plugin) and others
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.blackbuild.annodocimal.publication

import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile

@Issue('#44')
class LocalPublicationContractTest extends Specification {

    private static final List<String> ARTIFACTS = [
            'anno-docimal-annotations',
            'anno-docimal-apt',
            'anno-docimal-ast',
            'anno-docimal-global-ast',
            'anno-docimal-generator',
            'anno-docimal-gradle-plugin'
    ]

    private static final List<String> PLUGIN_IDS = [
            'com.blackbuild.annodocimal.base-plugin',
            'com.blackbuild.annodocimal.groovy-plugin'
    ]

    @Shared Path repository = Path.of(System.getProperty('annodocimal.publication.repository'))
    @Shared String version = System.getProperty('annodocimal.publication.version')

    def 'clean local repository contains the complete publication product'() {
        expect:
        ARTIFACTS.every { artifact ->
            def directory = moduleDirectory('com.blackbuild.annodocimal', artifact)
            Files.isRegularFile(directory.resolve("${artifact}-${version}.jar")) &&
                    Files.isRegularFile(directory.resolve("${artifact}-${version}.pom")) &&
                    Files.isRegularFile(directory.resolve("${artifact}-${version}.module")) &&
                    Files.isRegularFile(directory.resolve("${artifact}-${version}-sources.jar")) &&
                    Files.isRegularFile(directory.resolve("${artifact}-${version}-javadoc.jar"))
        }

        and:
        PLUGIN_IDS.every { pluginId ->
            def artifact = "${pluginId}.gradle.plugin"
            def directory = moduleDirectory(pluginId, artifact)
            Files.isRegularFile(directory.resolve("${artifact}-${version}.pom"))
        }
    }

    def 'POMs expose only the intended consumer dependencies and descriptions'() {
        expect:
        pomContract('anno-docimal-annotations') == [
                description : 'Annotations for AnnoDocimal',
                dependencies: ['org.jspecify:jspecify:compile']
        ]
        pomContract('anno-docimal-apt') == [
                description : 'Annotation Processor for AnnoDocimal',
                dependencies: ['com.blackbuild.annodocimal:anno-docimal-annotations:compile']
        ]
        pomContract('anno-docimal-ast') == [
                description : 'Groovy AST documentation capture and helper APIs for AnnoDocimal',
                dependencies: [
                        'com.blackbuild.annodocimal:anno-docimal-annotations:compile',
                        'org.jspecify:jspecify:compile'
                ]
        ]
        pomContract('anno-docimal-global-ast') == [
                description : 'Global Groovy AST documentation capture for AnnoDocimal',
                dependencies: ['com.blackbuild.annodocimal:anno-docimal-ast:compile']
        ]
        pomContract('anno-docimal-generator') == [
                description : 'Source-projection generator for AnnoDocimal',
                dependencies: ['org.jspecify:jspecify:compile']
        ]
        pomContract('anno-docimal-gradle-plugin') == [
                description : 'AnnoDocimal source-projection Gradle plugins',
                dependencies: []
        ]

        and:
        pluginMarkerDependency('com.blackbuild.annodocimal.base-plugin') ==
                "com.blackbuild.annodocimal:anno-docimal-gradle-plugin:${version}"
        pluginMarkerDependency('com.blackbuild.annodocimal.groovy-plugin') ==
                "com.blackbuild.annodocimal:anno-docimal-gradle-plugin:${version}"
    }

    def 'Gradle module metadata describes resolvable variants and files'() {
        when:
        def contracts = ARTIFACTS.collectEntries { artifact ->
            def metadata = new JsonSlurper().parse(moduleFile(artifact).toFile())
            [(artifact): metadata.variants.collectEntries { variant ->
                [(variant.name): [
                        dependencies: (variant.dependencies ?: []).collect {
                            "${it.group}:${it.module}"
                        }.sort(),
                        files       : (variant.files ?: []).collect { it.url }.sort()
                ]]
            }]
        }

        then:
        contracts['anno-docimal-generator'].keySet() ==
                ['javadocElements', 'sourcesElements', 'shadowRuntimeElements'] as Set
        contracts['anno-docimal-generator'].shadowRuntimeElements.dependencies == ['org.jspecify:jspecify']
        contracts['anno-docimal-gradle-plugin'].keySet() ==
                ['javadocElements', 'sourcesElements', 'shadowRuntimeElements'] as Set
        contracts['anno-docimal-gradle-plugin'].shadowRuntimeElements.dependencies == []

        and:
        ['anno-docimal-annotations', 'anno-docimal-apt', 'anno-docimal-global-ast'].every { artifact ->
            contracts[artifact].keySet() ==
                    ['apiElements', 'runtimeElements', 'javadocElements', 'sourcesElements'] as Set
        }
        contracts['anno-docimal-ast'].keySet() == [
                'apiElements', 'runtimeElements', 'javadocElements', 'sourcesElements',
                'testFixturesApiElements', 'testFixturesRuntimeElements'
        ] as Set

        and:
        contracts.values().every { variants ->
            variants.values().every { variant ->
                variant.files.every { fileName ->
                    Files.isRegularFile(repositoryFileFor(fileName))
                }
            }
        }
    }

    def 'published JARs preserve module services plugins and shaded boundaries'() {
        expect:
        [
                'anno-docimal-annotations': 'com.blackbuild.annodocimal.annotations',
                'anno-docimal-apt': 'com.blackbuild.annodocimal.apt',
                'anno-docimal-ast': 'com.blackbuild.annodocimal.ast',
                'anno-docimal-global-ast': 'com.blackbuild.annodocimal.global.ast',
                'anno-docimal-generator': 'com.blackbuild.annodocimal.generator'
        ].every { artifact, moduleName ->
            withJar(artifact) { jar ->
                jar.manifest.mainAttributes.getValue('Automatic-Module-Name') == moduleName
            }
        }

        and:
        jarEntryText('anno-docimal-apt', 'META-INF/services/javax.annotation.processing.Processor') ==
                'com.blackbuild.annodocimal.ast.AnnoDocimalAnnotationProcessor'
        jarEntries('anno-docimal-apt').contains(
                'com/blackbuild/annodocimal/ast/AnnoDocimalAnnotationProcessor.class')
        jarEntryText('anno-docimal-global-ast',
                'META-INF/services/org.codehaus.groovy.transform.ASTTransformation') ==
                'com.blackbuild.annodocimal.global.ast.InlineJavadocsGlobalTransformation'
        jarEntries('anno-docimal-global-ast').contains(
                'com/blackbuild/annodocimal/global/ast/InlineJavadocsGlobalTransformation.class')

        and:
        jarEntryText('anno-docimal-gradle-plugin',
                'META-INF/gradle-plugins/com.blackbuild.annodocimal.base-plugin.properties') ==
                'implementation-class=com.blackbuild.annodocimal.plugin.AnnoDocimalBasePlugin'
        jarEntryText('anno-docimal-gradle-plugin',
                'META-INF/gradle-plugins/com.blackbuild.annodocimal.groovy-plugin.properties') ==
                'implementation-class=com.blackbuild.annodocimal.plugin.AnnoDocimalGroovyPlugin'
        jarEntries('anno-docimal-gradle-plugin').containsAll([
                'com/blackbuild/annodocimal/plugin/AnnoDocimalBasePlugin.class',
                'com/blackbuild/annodocimal/plugin/AnnoDocimalGroovyPlugin.class'
        ])

        and:
        ['anno-docimal-generator', 'anno-docimal-gradle-plugin'].every { artifact ->
            def entries = jarEntries(artifact)
            entries.contains('com/blackbuild/annodocimal/generator/SourceProjector.class') &&
                    entries.contains('shadow/asm/ClassReader.class') &&
                    entries.contains('shadow/javapoet/JavaFile.class') &&
                    entries.every { entry ->
                        !entry.startsWith('org/objectweb/asm/') &&
                                !entry.startsWith('com/squareup/javapoet/') &&
                                !entry.startsWith('org/jspecify/') &&
                                !entry.startsWith('org/gradle/') &&
                                !entry.startsWith('org/codehaus/groovy/') &&
                                !entry.startsWith('org/apache/groovy/')
                    }
        }
    }

    def 'sources and Javadocs describe every user-facing Java artifact'() {
        expect:
        [
                'anno-docimal-annotations': 'com/blackbuild/annodocimal/annotations/AnnoDoc.java',
                'anno-docimal-apt': 'com/blackbuild/annodocimal/ast/AnnoDocimalAnnotationProcessor.java',
                'anno-docimal-ast': 'com/blackbuild/annodocimal/ast/AstDocumentation.java',
                'anno-docimal-global-ast':
                        'com/blackbuild/annodocimal/global/ast/InlineJavadocsGlobalTransformation.java',
                'anno-docimal-generator': 'com/blackbuild/annodocimal/generator/SourceProjector.java',
                'anno-docimal-gradle-plugin': 'com/blackbuild/annodocimal/plugin/SourceProjectionTask.java'
        ].every { artifact, sourceEntry ->
            archiveEntries(artifactFile(artifact, 'sources')).contains(sourceEntry)
        }

        and:
        [
                'anno-docimal-annotations': 'com/blackbuild/annodocimal/annotations/AnnoDoc.html',
                'anno-docimal-apt': 'com/blackbuild/annodocimal/ast/AnnoDocimalAnnotationProcessor.html',
                'anno-docimal-ast': 'com/blackbuild/annodocimal/ast/AstDocumentation.html',
                'anno-docimal-global-ast':
                        'com/blackbuild/annodocimal/global/ast/InlineJavadocsGlobalTransformation.html',
                'anno-docimal-generator': 'com/blackbuild/annodocimal/generator/SourceProjector.html',
                'anno-docimal-gradle-plugin': 'com/blackbuild/annodocimal/plugin/SourceProjectionTask.html'
        ].every { artifact, javadocEntry ->
            archiveEntries(artifactFile(artifact, 'javadoc')).contains(javadocEntry)
        }
    }

    private Map<String, ?> pomContract(String artifact) {
        def pom = new XmlSlurper(false, false).parse(artifactFile(artifact, null, 'pom').toFile())
        [
                description : pom.description.text(),
                dependencies: pom.dependencies.dependency.collect {
                    "${it.groupId.text()}:${it.artifactId.text()}:${it.scope.text()}"
                }.sort()
        ]
    }

    private String pluginMarkerDependency(String pluginId) {
        def artifact = "${pluginId}.gradle.plugin"
        def pom = new XmlSlurper(false, false).parse(
                artifactFile(pluginId, artifact, null, 'pom').toFile())
        def dependency = pom.dependencies.dependency[0]
        "${dependency.groupId.text()}:${dependency.artifactId.text()}:${dependency.version.text()}"
    }

    private Path moduleFile(String artifact) {
        artifactFile(artifact, null, 'module')
    }

    private Path repositoryFileFor(String fileName) {
        Files.walk(repository).filter { it.fileName.toString() == fileName }.findFirst().orElseThrow()
    }

    private Set<String> jarEntries(String artifact) {
        archiveEntries(artifactFile(artifact))
    }

    private Set<String> archiveEntries(Path archive) {
        new JarFile(archive.toFile()).withCloseable { jar ->
            jar.entries().collect { it.name } as Set
        }
    }

    private String jarEntryText(String artifact, String entry) {
        withJar(artifact) { jar ->
            jar.getInputStream(jar.getJarEntry(entry)).getText('UTF-8').trim()
        }
    }

    private <T> T withJar(String artifact, Closure<T> action) {
        new JarFile(artifactFile(artifact).toFile()).withCloseable(action)
    }

    private Path artifactFile(String artifact, String classifier = null, String extension = 'jar') {
        artifactFile('com.blackbuild.annodocimal', artifact, classifier, extension)
    }

    private Path artifactFile(String group, String artifact, String classifier, String extension) {
        def suffix = classifier == null ? '' : "-${classifier}"
        moduleDirectory(group, artifact).resolve("${artifact}-${version}${suffix}.${extension}")
    }

    private Path moduleDirectory(String group, String artifact) {
        repository.resolve(group.replace('.', '/')).resolve(artifact).resolve(version)
    }
}
