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
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

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
