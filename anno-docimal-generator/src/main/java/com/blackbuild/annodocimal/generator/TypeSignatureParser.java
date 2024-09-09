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
package com.blackbuild.annodocimal.generator;

import com.squareup.javapoet.*;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.List;

abstract class TypeSignatureParser extends SignatureVisitor {

    protected TypeSignatureParser() {
        super(CompilerConfiguration.ASM_API_VERSION);
    }

    abstract void finished(TypeName result);

    private String baseName;
    private final List<TypeName> arguments = new ArrayList<>();

    @Override
    public void visitTypeVariable(final String name) {
        finished(TypeVariableName.get(name));
    }

    @Override
    public void visitBaseType(final char descriptor) {
        finished(toTypeName(descriptor));
    }

    @Override
    public SignatureVisitor visitArrayType() {
        final TypeSignatureParser outer = this;
        return new TypeSignatureParser() {
            @Override
            void finished(TypeName result) {
                outer.finished(ArrayTypeName.of(result));
            }
        };
    }

    @Override
    public void visitClassType(final String name) {
        baseName = JavaPoetClassVisitor.fromInternalName(name);
    }

    @Override
    public void visitTypeArgument() {
        arguments.add(WildcardTypeName.subtypeOf(ClassName.OBJECT));
    }

    @Override
    public SignatureVisitor visitTypeArgument(final char wildcard) {
        return new TypeSignatureParser() {
            @Override
            void finished(TypeName result) {
                if (wildcard == INSTANCEOF) {
                    arguments.add(result);
                } else if (wildcard == EXTENDS) {
                    arguments.add(WildcardTypeName.subtypeOf(result));
                } else if (wildcard == SUPER) {
                    arguments.add(WildcardTypeName.supertypeOf(result));
                } else {
                    throw new AssertionError();
                }
            }
        };
    }

    @Override
    public void visitInnerClassType(final String name) {
        baseName += "$" + name;
        arguments.clear();
    }

    @Override
    public void visitEnd() {
        ClassName baseType = ClassName.bestGuess(baseName);
        if (arguments.isEmpty()) {
            finished(baseType);
        } else {
            finished(ParameterizedTypeName.get(baseType, arguments.toArray(new TypeName[0])));
        }
    }

    private static TypeName toTypeName(char descriptor) {
        switch (descriptor) {
            case 'B':
                return TypeName.BYTE;
            case 'C':
                return TypeName.CHAR;
            case 'D':
                return TypeName.DOUBLE;
            case 'F':
                return TypeName.FLOAT;
            case 'I':
                return TypeName.INT;
            case 'J':
                return TypeName.LONG;
            case 'S':
                return TypeName.SHORT;
            case 'V':
                return TypeName.VOID;
            case 'Z':
                return TypeName.BOOLEAN;
            default:
                throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
        }

    }
}
