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

/**
 * Since groovy has some strange behaviours during compilation (like erasing generics in exceptions),
 * we perform additional tests against real java classes.
 */
class AnnoDocGeneratorJavaTest extends JavaClassGeneratingTest {

    String generatedSource
    SourceBlock generated

    def "method name conversion"() {
        given:
        compile("""
            package dummy;
            public abstract class Dummy$generics {
                public $signature {
                    ${signature.startsWith("void") ? "" : "return null;"}
                }
            }
        """)

        when:
        generateSource()

        then:
        generatedSource.contains(signature)

        where:
        signature                        | generics
        "void method()"                  | ""
        "void method(String aString)"    | ""
        "void method(T aParam)"          | "<T>"
        "void method() throws Exception" | ""
    }

    def "default interface methods"() {
        given:
        compile("""
            package dummy;
            public interface Dummy {
                void method();
                default void defaultMethod() {}
                static void staticMethod() {}
            }
        """)

        when:
        generateSourceText()

        then:
        generatedSource == """package dummy;

public interface Dummy {
  void method();

  default void defaultMethod() {
  }

  static void staticMethod() {
  }
}
"""
    }

    def "documentation policy includes public and protected nested classes"() {
        given:
        compile("""
            package dummy;
            public abstract class Dummy {
                public class Inner {}
                protected class InnerProtected {}
                private class InnerPrivate {}
                class InnerDefault {}
                public static class InnerStatic {}
                protected static class InnerProtectedStatic {}
                private static class InnerPrivateStatic {}
                static class InnerDefaultStatic {}
                protected abstract static class InnerAbstractStatic {}
            }
        """)

        when:
        generateSource()

        then:
        generatedSource == '''package dummy;

public abstract class Dummy {
  public Dummy() {
  }

  protected abstract static class InnerAbstractStatic {
    protected InnerAbstractStatic() {
    }
  }

  protected static class InnerProtectedStatic {
    protected InnerProtectedStatic() {
    }
  }

  public static class InnerStatic {
    public InnerStatic() {
    }
  }

  protected class InnerProtected {
    protected InnerProtected() {
    }
  }

  public class Inner {
    public Inner() {
    }
  }
}
'''
    }

    void generateSource() {
        generateSourceText()
        generated = SourceBlock.fromtext(generatedSource)
    }

    void generateSourceText() {
        generatedSource = new SourceProjector(ProjectionPolicy.documentation()).projectToText(file.toPath())
    }
}
