/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 Stephan Pauxberger
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
package com.blackbuild.annodocimal.ast

import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.customizers.CompilationCustomizer
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.TempDir

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path

@Issue("43")
class CapturePathConformanceTest extends Specification {

    private static final String FIXTURE_CLASS = 'conformance.CaptureFixture'
    private static final String CARRIER_FIXTURE_CLASS = 'conformance.CarrierFixture'
    private static final String GLOBAL_PROVIDER =
            'com.blackbuild.annodocimal.global.ast.InlineJavadocsGlobalTransformation'
    private static final String GLOBAL_SERVICE = 'META-INF/services/org.codehaus.groovy.transform.ASTTransformation'

    private static final Map<String, String> EXPECTED_PROPERTY_VALUES = [
            (FIXTURE_CLASS + '#classDoc')                         : '''Shared capture fixture.
Second normalized line.
@since 1.0''',
            (FIXTURE_CLASS + '#field.title')                     : 'Documented field or property.',
            (FIXTURE_CLASS + '#method.<init>(java.lang.String)') : '''Creates the fixture.
@param title initial title''',
            (FIXTURE_CLASS + '#method.describe(java.lang.String)'): '''Describes a value across
multiple normalized lines.
@param value value to describe
@return the description
@since 1.0''',
            (FIXTURE_CLASS + '$Nested#classDoc')                 : 'Nested declaration documentation.',
            (FIXTURE_CLASS + '$Nested#method.nested()')          : 'Nested method documentation.'
    ].asImmutable()

    private static final Map<String, String> EXPECTED_DOCUMENTATION = [
            (FIXTURE_CLASS + '#classDoc')                         : '''Shared capture fixture.
Second normalized line.

@since 1.0''',
            (FIXTURE_CLASS + '#field.title')                     : 'Documented field or property.',
            (FIXTURE_CLASS + '#method.<init>(java.lang.String)') : '''Creates the fixture.

@param title initial title''',
            (FIXTURE_CLASS + '#method.describe(java.lang.String)'): '''Describes a value across
multiple normalized lines.

@param value value to describe
@return the description
@since 1.0''',
            (FIXTURE_CLASS + '$Nested#classDoc')                 : 'Nested declaration documentation.',
            (FIXTURE_CLASS + '$Nested#method.nested()')          : 'Nested method documentation.'
    ].asImmutable()

    private static final Set<String> EXPECTED_ABSENT_KEYS = [
            FIXTURE_CLASS + '#method.empty()'
    ].asImmutable()

    private static final Map<String, String> EXPECTED_CARRIER_DOCUMENTATION = [
            (CARRIER_FIXTURE_CLASS + '#method.runtimeOnly()'): 'Runtime GroovyDoc documentation.',
            (CARRIER_FIXTURE_CLASS + '#method.both()')       : 'Canonical AnnoDoc documentation.'
    ].asImmutable()

    private static final String JAVA_SOURCE = '''
package conformance;

import com.blackbuild.annodocimal.annotations.InlineJavadocs;

/**
 * Shared capture fixture.
 * Second normalized line.
 * @since 1.0
 */
@InlineJavadocs
public class CaptureFixture {
    /** Documented field or property. */
    public String title;

    /**
     * Creates the fixture.
     * @param title initial title
     */
    public CaptureFixture(String title) {
        this.title = title;
    }

    /**
     * Describes a value across
     * multiple normalized lines.
     * @param value value to describe
     * @return the description
     * @since 1.0
     */
    public String describe(String value) {
        return value;
    }

    /** */
    public void empty() {
    }

    /** Nested declaration documentation. */
    public static class Nested {
        /** Nested method documentation. */
        public void nested() {
        }
    }
}
'''

    private static final String GROOVY_SOURCE = '''
package conformance

import com.blackbuild.annodocimal.annotations.InlineJavadocs

/**
 * Shared capture fixture.
 * Second normalized line.
 * @since 1.0
 */
class CaptureFixture {
    /** Documented field or property. */
    String title

    /**
     * Creates the fixture.
     * @param title initial title
     */
    CaptureFixture(String title) {
        this.title = title
    }

    /**
     * Describes a value across
     * multiple normalized lines.
     * @param value value to describe
     * @return the description
     * @since 1.0
     */
    String describe(String value) {
        value
    }

    /** */
    void empty() {
    }

    /** Nested declaration documentation. */
    static class Nested {
        /** Nested method documentation. */
        void nested() {
        }
    }
}
'''

    private static final String CARRIER_GROOVY_SOURCE = '''
package conformance

import com.blackbuild.annodocimal.annotations.AnnoDoc
import com.blackbuild.annodocimal.annotations.InlineJavadocs

class CarrierFixture {
    /**@
     * Runtime GroovyDoc documentation.
     */
    void runtimeOnly() {
    }

    /**@
     * Lower-precedence runtime documentation.
     */
    @AnnoDoc('Canonical AnnoDoc documentation.')
    void both() {
    }
}
'''

    @TempDir
    Path temporaryDirectory

    def "Java APT and local and packaged-global Groovy capture share normalized semantics"() {
        when:
        def javaProperties = captureJavaProperties()
        def javaDocumentation = javaProperties.collectEntries { key, value ->
            [key, Documentation.parse(value).render()]
        }
        def localDocumentation = captureGroovy(GroovyCapturePath.LOCAL).documentation
        def globalDocumentation = captureGroovy(GroovyCapturePath.PACKAGED_GLOBAL).documentation

        then: 'properties and annotations are intentionally different storage with the same declaration semantics'
        assertCapturePath('java-apt-properties', javaProperties, EXPECTED_PROPERTY_VALUES)
        assertCapturePath('java-apt-semantics', javaDocumentation, EXPECTED_DOCUMENTATION)
        assertCapturePath("${GroovyCapturePath.LOCAL.label}-extraction", localDocumentation)
        assertCapturePath("${GroovyCapturePath.PACKAGED_GLOBAL.label}-extraction", globalDocumentation)
    }

    def "local and packaged-global capture preserve carrier precedence without duplicate emission"() {
        when:
        def local = captureGroovy(GroovyCapturePath.LOCAL_WITH_RUNTIME,
                CARRIER_FIXTURE_CLASS, CARRIER_GROOVY_SOURCE)
        def global = captureGroovy(GroovyCapturePath.PACKAGED_GLOBAL_WITH_RUNTIME,
                CARRIER_FIXTURE_CLASS, CARRIER_GROOVY_SOURCE)

        then:
        assertCarrierPath("${GroovyCapturePath.LOCAL_WITH_RUNTIME.label}-carriers", local)
        assertCarrierPath("${GroovyCapturePath.PACKAGED_GLOBAL_WITH_RUNTIME.label}-carriers", global)
    }

    private Map<String, String> captureJavaProperties() {
        Path source = temporaryDirectory.resolve('java/src/conformance/CaptureFixture.java')
        Path classes = temporaryDirectory.resolve('java/classes')
        Files.createDirectories(source.parent)
        Files.createDirectories(classes)
        Files.writeString(source, JAVA_SOURCE)

        def annotationsJar = moduleArtifact('annotations')
        def aptJar = moduleArtifact('apt')
        def diagnostics = new ByteArrayOutputStream()
        int result = ToolProvider.systemJavaCompiler.run(null, null, diagnostics,
                '-classpath', System.getProperty('java.class.path'),
                '-processorpath', [aptJar, annotationsJar].join(File.pathSeparator),
                '-d', classes.toString(), source.toString())
        assert result == 0: "java-apt-properties compilation failed: ${diagnostics.toString('UTF-8')}"

        def documentation = [:]
        collectProperties(classes, FIXTURE_CLASS, documentation)
        collectProperties(classes, "$FIXTURE_CLASS\$Nested", documentation)
        documentation
    }

    private static void collectProperties(Path classes, String className, Map<String, String> target) {
        Path resource = classes.resolve(className.replace('.', File.separator) + '__annodoc.properties')
        assert Files.isRegularFile(resource): "java-apt-properties missing declaration resource $className"
        def properties = new Properties()
        Files.newInputStream(resource).withCloseable { properties.load(it) }
        properties.each { key, value -> target[className + '#' + key] = value }
    }

    private CaptureObservation captureGroovy(GroovyCapturePath path,
                                             String fixtureClass = FIXTURE_CLASS,
                                             String sourceText = GROOVY_SOURCE) {
        String simpleName = fixtureClass.substring(fixtureClass.lastIndexOf('.') + 1)
        Path source = temporaryDirectory.resolve("$path.label/src/conformance/${simpleName}.groovy")
        Path classes = temporaryDirectory.resolve("$path.label/classes")
        Files.createDirectories(source.parent)
        Files.createDirectories(classes)
        String marker = path.local ? '@InlineJavadocs\n' : ''
        Files.writeString(source, sourceText.replace("class $simpleName", marker + "class $simpleName"))

        def observation = new CaptureObservation()
        def configuration = new CompilerConfiguration(targetDirectory: classes.toFile(), parameters: true)
        configuration.optimizationOptions.groovydoc = Boolean.TRUE
        configuration.optimizationOptions.runtimeGroovydoc = path.runtimeGroovydoc
        configuration.addCompilationCustomizers(new DocumentationCollector(fixtureClass, observation))

        def parent = new GlobalAstFilteringClassLoader(getClass().classLoader)
        def loader = new GroovyClassLoader(parent, configuration)
        try {
            if (path.packagedGlobal) {
                File globalJar = moduleArtifact('globalAst')
                loader.addURL(globalJar.toURI().toURL())
                Class<?> provider = loader.loadClass(GLOBAL_PROVIDER)
                File providerLocation = new File(provider.protectionDomain.codeSource.location.toURI())
                assert providerLocation.canonicalFile == globalJar.canonicalFile:
                        "$path.label provider was not loaded from packaged artifact $globalJar"
            }
            loader.parseClass(source.toFile())
        } finally {
            loader.close()
        }
        observation
    }

    private static File moduleArtifact(String name) {
        def value = System.getProperty("annodocimal.module.$name")
        assert value != null: "missing packaged module for capture path: $name"
        def artifact = new File(value)
        assert artifact.isFile(): "missing packaged module for capture path: $name at $artifact"
        artifact
    }

    private static boolean assertCapturePath(String path, Map<String, String> actual,
                                             Map<String, String> expectedDocumentation = EXPECTED_DOCUMENTATION) {
        expectedDocumentation.each { key, expected ->
            assert actual[key] == expected:
                    "$path documentation mismatch for declaration key $key; expected <$expected> but was <${actual[key]}>"
        }
        EXPECTED_ABSENT_KEYS.each { key ->
            assert !actual.containsKey(key): "$path unexpectedly captured empty documentation for declaration key $key"
        }
        def unexpected = actual.keySet() - expectedDocumentation.keySet()
        assert unexpected.empty: "$path produced unexpected documented declaration keys: $unexpected"
        true
    }

    private static boolean assertCarrierPath(String path, CaptureObservation observation) {
        assertCapturePath(path, observation.documentation, EXPECTED_CARRIER_DOCUMENTATION)
        String runtimeKey = "$CARRIER_FIXTURE_CLASS#method.runtimeOnly()"
        assert observation.carriers[runtimeKey] == [annoDoc: 0, groovydoc: 1]:
                "$path duplicate-avoidance mismatch for declaration key $runtimeKey: ${observation.carriers[runtimeKey]}"
        String precedenceKey = "$CARRIER_FIXTURE_CLASS#method.both()"
        assert observation.carriers[precedenceKey] == [annoDoc: 1, groovydoc: 1]:
                "$path precedence fixture mismatch for declaration key $precedenceKey: ${observation.carriers[precedenceKey]}"
        true
    }

    private static final class CaptureObservation {
        final Map<String, String> documentation = [:]
        final Map<String, Map<String, Integer>> carriers = [:]
    }

    private enum GroovyCapturePath {
        LOCAL('groovy-local', true, false, false),
        PACKAGED_GLOBAL('groovy-global-packaged', false, true, false),
        LOCAL_WITH_RUNTIME('groovy-local-runtime', true, false, true),
        PACKAGED_GLOBAL_WITH_RUNTIME('groovy-global-packaged-runtime', false, true, true)

        final String label
        final boolean local
        final boolean packagedGlobal
        final boolean runtimeGroovydoc

        GroovyCapturePath(String label, boolean local, boolean packagedGlobal, boolean runtimeGroovydoc) {
            this.label = label
            this.local = local
            this.packagedGlobal = packagedGlobal
            this.runtimeGroovydoc = runtimeGroovydoc
        }
    }

    private static final class DocumentationCollector extends CompilationCustomizer {
        private final String fixtureClass
        private final CaptureObservation observation

        private DocumentationCollector(String fixtureClass, CaptureObservation observation) {
            super(CompilePhase.CANONICALIZATION)
            this.fixtureClass = fixtureClass
            this.observation = observation
        }

        @Override
        void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
            if (!classNode.name.startsWith(fixtureClass)) {
                return
            }
            collect(classNode, "$classNode.name#classDoc")
            classNode.fields.each { field -> collect(field, "$classNode.name#field.$field.name") }
            classNode.declaredConstructors.each { constructor -> collect(constructor, methodKey(classNode, constructor)) }
            classNode.methods.each { method -> collect(method, methodKey(classNode, method)) }
        }

        private void collect(AnnotatedNode node, String key) {
            def extracted = AstDocumentation.extractExact(node)
            if (extracted.present) {
                observation.documentation[key] = extracted.get().render()
            }
            observation.carriers[key] = [
                    annoDoc  : carrierCount(node, 'com.blackbuild.annodocimal.annotations.AnnoDoc'),
                    groovydoc: carrierCount(node, 'groovy.lang.Groovydoc')
            ]
        }

        private static int carrierCount(AnnotatedNode node, String annotationName) {
            node.annotations.count { it.classNode.name == annotationName }
        }

        private static String methodKey(ClassNode owner, MethodNode method) {
            String name = method instanceof ConstructorNode ? '<init>' : method.name
            String parameters = method.parameters.collect { it.type.name }.join(',')
            "$owner.name#method.$name($parameters)"
        }
    }

    private static final class GlobalAstFilteringClassLoader extends ClassLoader {
        private GlobalAstFilteringClassLoader(ClassLoader parent) {
            super(parent)
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name == GLOBAL_PROVIDER) {
                throw new ClassNotFoundException(name)
            }
            super.loadClass(name, resolve)
        }

        @Override
        URL getResource(String name) {
            name == GLOBAL_SERVICE ? null : super.getResource(name)
        }

        @Override
        Enumeration<URL> getResources(String name) throws IOException {
            name == GLOBAL_SERVICE ? Collections.emptyEnumeration() : super.getResources(name)
        }
    }
}
