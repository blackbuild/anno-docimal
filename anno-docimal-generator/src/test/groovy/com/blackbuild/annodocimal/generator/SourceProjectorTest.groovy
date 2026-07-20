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
package com.blackbuild.annodocimal.generator

import com.google.testing.compile.Compilation
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import spock.lang.Issue

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class SourceProjectorTest extends JavaClassGeneratingTest {

    def "documentation policy is an immutable reusable value"() {
        when:
        ProjectionPolicy policy = ProjectionPolicy.documentation()
        ProjectionPolicy expanded = policy.toBuilder()
                .includedVisibilities(EnumSet.allOf(DeclarationVisibility))
                .includeNestedDeclarations(false)
                .includeSyntheticDeclarations(true)
                .includeGroovyRuntimeArtifacts(true)
                .build()

        then:
        policy.includedVisibilities == [DeclarationVisibility.PUBLIC, DeclarationVisibility.PROTECTED] as Set
        policy.nestedDeclarationsIncluded
        !policy.syntheticDeclarationsIncluded
        !policy.groovyRuntimeArtifactsIncluded
        policy == ProjectionPolicy.builder().build()
        policy.hashCode() == ProjectionPolicy.documentation().hashCode()

        and:
        expanded.includedVisibilities == EnumSet.allOf(DeclarationVisibility)
        !expanded.nestedDeclarationsIncluded
        expanded.syntheticDeclarationsIncluded
        expanded.groovyRuntimeArtifactsIncluded
        expanded != policy

        when:
        policy.includedVisibilities.clear()

        then:
        thrown(UnsupportedOperationException)
    }

    def "projects one caller-selected class to text and its managed source path"() {
        given:
        compile('''
            package dummy;
            public class ProjectionFixture {
                public void visible() {}
            }
        ''')
        SourceProjector projector = new SourceProjector(ProjectionPolicy.documentation())
        File destination = new File(outputDirectory, 'projection-output')
        File unrelated = new File(destination, 'keep.txt')
        unrelated.parentFile.mkdirs()
        unrelated.text = 'untouched'

        when:
        String source = projector.projectToText(file.toPath())
        def written = projector.projectToDirectory(file.toPath(), destination.toPath())

        then:
        written == destination.toPath().resolve('dummy/ProjectionFixture.java')
        Files.readString(written, StandardCharsets.UTF_8) == source
        source == '''package dummy;

public class ProjectionFixture {
  public ProjectionFixture() {
  }

  public void visible() {
  }
}
'''
        unrelated.text == 'untouched'
    }

    def "documentation policy selects declarations and retains signature closure"() {
        given:
        compile('''
            package dummy;
            public class PolicyFixture {
                public void publicMethod() {}
                protected void protectedMethod() {}
                void packageMethod() {}
                private void privateMethod() {}

                public static class UnusedPublic {}
                protected static class UnusedProtected {}
                static class UnusedPackage {}
                private static class RequiredPrivate {}

                public RequiredPrivate required() { return null; }
            }
        ''')

        when:
        String documented = new SourceProjector(ProjectionPolicy.documentation()).projectToText(file.toPath())
        ProjectionPolicy closureOnly = ProjectionPolicy.builder()
                .includeNestedDeclarations(false)
                .build()
        String withoutUnreferencedNested = new SourceProjector(closureOnly).projectToText(file.toPath())

        then:
        documented == '''package dummy;

public class PolicyFixture {
  public PolicyFixture() {
  }

  public void publicMethod() {
  }

  protected void protectedMethod() {
  }

  public RequiredPrivate required() {
    return null;
  }

  private static class RequiredPrivate {
  }

  protected static class UnusedProtected {
    protected UnusedProtected() {
    }
  }

  public static class UnusedPublic {
    public UnusedPublic() {
    }
  }
}
'''

        and:
        withoutUnreferencedNested == '''package dummy;

public class PolicyFixture {
  public PolicyFixture() {
  }

  public void publicMethod() {
  }

  protected void protectedMethod() {
  }

  public RequiredPrivate required() {
    return null;
  }

  private static class RequiredPrivate {
  }
}
'''
    }

    def "visibility and synthetic controls are applied independently"() {
        given:
        def classFile = new File(outputDirectory, 'dummy/ControlledFixture.class')
        classFile.parentFile.mkdirs()
        ClassWriter writer = new ClassWriter(0)
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, 'dummy/ControlledFixture', null, 'java/lang/Object', null)
        writer.visitField(Opcodes.ACC_PUBLIC, 'regularField', 'Ljava/lang/String;', null, null).visitEnd()
        writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, 'syntheticField', 'Ljava/lang/String;', null, null).visitEnd()
        writer.visitField(Opcodes.ACC_PRIVATE, 'privateField', 'Ljava/lang/String;', null, null).visitEnd()
        writer.visitEnd()
        Files.write(classFile.toPath(), writer.toByteArray())

        when:
        String documented = new SourceProjector(ProjectionPolicy.documentation()).projectToText(classFile.toPath())
        ProjectionPolicy expanded = ProjectionPolicy.builder()
                .includedVisibilities(EnumSet.allOf(DeclarationVisibility))
                .includeSyntheticDeclarations(true)
                .build()
        String complete = new SourceProjector(expanded).projectToText(classFile.toPath())

        then:
        documented == '''package dummy;

import java.lang.String;

public class ControlledFixture {
  public String regularField;
}
'''

        and:
        complete == '''package dummy;

import java.lang.String;

public class ControlledFixture {
  public String regularField;

  public String syntheticField;

  private String privateField;
}
'''
    }

    def "unrepresentable selected bridge methods fail with declaration context"() {
        given:
        compile('''
            package dummy;
            import java.util.function.Supplier;
            public class BridgeFixture implements Supplier<String> {
                public String get() { return "value"; }
            }
        ''')
        ProjectionPolicy policy = ProjectionPolicy.builder().includeSyntheticDeclarations(true).build()

        when:
        new SourceProjector(policy).projectToText(file.toPath())

        then:
        SourceProjectionException exception = thrown()
        exception.inputPath == file.toPath()
        exception.declarationIdentifier.orElseThrow() == 'dummy.BridgeFixture#get'
    }

    def "Java names are not Groovy scaffolding and selected final fields remain valid"() {
        given:
        compile('''
            package dummy;
            public class JavaNamesFixture {
                public static final int MIN_VALUE = 7;
                public final String metaClass;

                public JavaNamesFixture() {
                    metaClass = "value";
                }

                public void next() {}
                public Object getProperty() { return null; }
                public void $api() {}
            }
        ''')

        when:
        String source = new SourceProjector(ProjectionPolicy.documentation()).projectToText(file.toPath())

        then:
        source == '''package dummy;

import java.lang.Object;
import java.lang.String;

public class JavaNamesFixture {
  public static final int MIN_VALUE = 7;

  public final String metaClass = null;

  public JavaNamesFixture() {
  }

  public void next() {
  }

  public Object getProperty() {
    return null;
  }

  public void $api() {
  }
}
'''

        when:
        compile(source)

        then:
        compilation.status() == Compilation.Status.SUCCESS
    }

    def "signature closure includes nested declarations referenced by selected annotations"() {
        given:
        compile('''
            package dummy;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            @AnnotationClosureFixture.Marker(AnnotationClosureFixture.Hidden.class)
            public class AnnotationClosureFixture {
                @Retention(RetentionPolicy.RUNTIME)
                @interface Marker {
                    Class<?> value();
                }
                static class Hidden {}
            }
        ''')

        when:
        String source = new SourceProjector(ProjectionPolicy.documentation()).projectToText(file.toPath())

        then:
        source == '''package dummy;

import java.lang.Class;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@AnnotationClosureFixture.Marker(AnnotationClosureFixture.Hidden.class)
public class AnnotationClosureFixture {
  public AnnotationClosureFixture() {
  }

  static class Hidden {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Marker {
    Class<?> value();
  }
}
'''
    }

    def "recursive nested signatures retain their complete source owner chain"() {
        given:
        compile('''
            package dummy;
            public class DeepNestedFixture {
                public Middle.Inner nested() { return null; }

                public static class Middle {
                    public static class Inner {}
                }
            }
        ''')

        when:
        String source = new SourceProjector(ProjectionPolicy.documentation()).projectToText(file.toPath())

        then:
        source == '''package dummy;

public class DeepNestedFixture {
  public DeepNestedFixture() {
  }

  public Middle.Inner nested() {
    return null;
  }

  public static class Middle {
    public Middle() {
    }

    public static class Inner {
      public Inner() {
      }
    }
  }
}
'''

        when:
        compile(source)

        then:
        compilation.status() == Compilation.Status.SUCCESS
    }

    @Issue("32")
    def "inherited generic API projections retain their enclosing type context"() {
        given:
        compile('''
            package dummy;
            import java.util.List;
            public class InheritedGenericFixture<T extends Comparable<? super T>> {
                public class Nested<U> {
                }

                public interface Api<V extends Comparable<? super V>> {
                    InheritedGenericFixture<V>.Nested<? extends V> inherited(List<? extends V> values);
                }

                public class Implementation implements Api<T> {
                    @Override
                    public InheritedGenericFixture<T>.Nested<? extends T> inherited(List<? extends T> values) {
                        return null;
                    }
                }
            }
        ''')
        SourceProjector projector = new SourceProjector(ProjectionPolicy.documentation())

        when:
        String source = projector.projectToText(file.toPath())

        then: 'the relevant projection is deterministic'
        projector.projectToText(file.toPath()) == source

        when: 'the projected declaration is compiled as the semantic validity oracle'
        compile(source)

        then:
        compilation.status() == Compilation.Status.SUCCESS

        and: 'exact assertions independently specify the required generic structure'
        source.contains('class InheritedGenericFixture<T extends Comparable<? super T>>')
        source.contains('class Implementation implements Api<T>')
        source.contains('InheritedGenericFixture<V>.Nested<? extends V> inherited(List<? extends V> values)')
        source.contains('InheritedGenericFixture<T>.Nested<? extends T> inherited(List<? extends T> values)')
    }

    @Issue("32")
    def "inherited generic method signatures resolve their interface type variables"() {
        given:
        compile('''
            package dummy;
            import java.util.List;
            public class InheritedGenericMethodFixture {
                public static class Owner<T extends Comparable<? super T>> {
                    public class Nested<U> {
                    }
                }

                public interface Api<T extends Comparable<? super T>> {
                    Owner<T>.Nested<? extends T> inherited(List<? extends T> values);
                }

                public static final class Value implements Comparable<Value> {
                    @Override
                    public int compareTo(Value other) {
                        return 0;
                    }
                }

                public static final class Implementation implements Api<Value> {
                    @Override
                    public Owner<Value>.Nested<? extends Value> inherited(List<? extends Value> values) {
                        return null;
                    }
                }
            }
        ''')
        def implementationClass = file.toPath().resolveSibling('InheritedGenericMethodFixture$Implementation.class')
        ClassReader reader = new ClassReader(Files.readAllBytes(implementationClass))
        ClassWriter writer = new ClassWriter(reader, 0)
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                      String[] exceptions) {
                if (name == 'inherited') {
                    signature = '(Ljava/util/List<+TT;>;)Ldummy/InheritedGenericMethodFixture$Owner<TT;>.Nested<+TT;>;'
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }
        }, 0)
        Files.write(implementationClass, writer.toByteArray())
        SourceProjector projector = new SourceProjector(ProjectionPolicy.documentation())

        when:
        String source = projector.projectToText(file.toPath())

        then: 'the relevant projection is deterministic'
        projector.projectToText(file.toPath()) == source

        when: 'the inherited type variable is resolved before the projected declaration is compiled'
        compile(source)

        then:
        compilation.status() == Compilation.Status.SUCCESS

        and: 'the inherited interface substitution retains its owner, nesting, bound, and wildcard context'
        source.contains('interface Api<T extends Comparable<? super T>>')
        source.contains('class Implementation implements Api<Value>')
        source.contains('Owner<Value>.Nested<? extends Value> inherited(List<? extends Value> values)')
    }

    def "semantic top-level names retain legal dollar characters"() {
        given:
        compile('''
            package dummy;
            public class Dollar$Top {
                public Dollar$Top self() { return this; }
            }
        ''')
        def destination = new File(outputDirectory, 'dollar-output').toPath()

        when:
        SourceProjector projector = new SourceProjector(ProjectionPolicy.documentation())
        String source = projector.projectToText(file.toPath())
        def written = projector.projectToDirectory(file.toPath(), destination)

        then:
        source == '''package dummy;

public class Dollar$Top {
  public Dollar$Top() {
  }

  public Dollar$Top self() {
    return null;
  }
}
'''
        written == destination.resolve('dummy/Dollar$Top.java')
        Files.readString(written) == source

        when:
        compile(source)

        then:
        compilation.status() == Compilation.Status.SUCCESS
    }

    def "external semantic top-level dollar names retain their identity in signatures and annotations"() {
        given:
        String dollarType = '''
            package api;
            public class Dollar$Type {}
        '''
        String markerType = '''
            package api;
            public @interface Marker$Type {
                Class<?> value();
            }
        '''
        compile([
                'api.Dollar$Type': dollarType,
                'api.Marker$Type': markerType,
                'dummy.ExternalDollarFixture': '''
                    package dummy;
                    import api.Dollar$Type;
                    import api.Marker$Type;

                    @Marker$Type(Dollar$Type.class)
                    public class ExternalDollarFixture {
                        public Dollar$Type value() { return null; }
                    }
                '''
        ], 'dummy.ExternalDollarFixture')

        when:
        String source = new SourceProjector(ProjectionPolicy.documentation()).projectToText(file.toPath())

        then:
        source == '''package dummy;

import api.Dollar$Type;
import api.Marker$Type;

@Marker$Type(Dollar$Type.class)
public class ExternalDollarFixture {
  public ExternalDollarFixture() {
  }

  public Dollar$Type value() {
    return null;
  }
}
'''

        when:
        compile([
                'api.Dollar$Type': dollarType,
                'api.Marker$Type': markerType,
                'dummy.ExternalDollarFixture': source
        ], 'dummy.ExternalDollarFixture')

        then:
        compilation.status() == Compilation.Status.SUCCESS
    }

    def "direct projection roots must be top-level declarations"() {
        given:
        compile('''
            package dummy;
            public class TopLevelFixture {
                public static class Nested {}
            }
        ''')
        def nestedClass = new File(file.parentFile, 'TopLevelFixture$Nested.class').toPath()

        when:
        new SourceProjector(ProjectionPolicy.documentation()).projectToText(nestedClass)

        then:
        SourceProjectionException exception = thrown()
        exception.inputPath == nestedClass
        exception.declarationIdentifier.orElseThrow() == 'dummy.TopLevelFixture.Nested'
    }

    def "default-package classes use the managed output root"() {
        given:
        def classFile = new File(outputDirectory, 'DefaultPackageFixture.class')
        ClassWriter writer = new ClassWriter(0)
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, 'DefaultPackageFixture', null, 'java/lang/Object', null)
        writer.visitEnd()
        Files.write(classFile.toPath(), writer.toByteArray())
        def destination = new File(outputDirectory, 'default-package-output').toPath()

        when:
        def written = new SourceProjector(ProjectionPolicy.documentation())
                .projectToDirectory(classFile.toPath(), destination)

        then:
        written == destination.resolve('DefaultPackageFixture.java')
        Files.readString(written) == '''public class DefaultPackageFixture {
}
'''
    }
}
