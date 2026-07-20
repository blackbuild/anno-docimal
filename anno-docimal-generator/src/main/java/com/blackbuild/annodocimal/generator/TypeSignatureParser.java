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
import java.util.Objects;
import java.util.function.Function;

abstract class TypeSignatureParser extends SignatureVisitor {

    protected TypeSignatureParser() {
        this(TypeConversion::fromInternalNameToClassName);
    }

    protected TypeSignatureParser(Function<String, ClassName> classNameResolver) {
        this(classNameResolver, name -> TypeVariableName.get(name));
    }

    protected TypeSignatureParser(Function<String, ClassName> classNameResolver,
                                  Function<String, TypeName> typeVariableResolver) {
        super(CompilerConfiguration.ASM_API_VERSION);
        this.classNameResolver = Objects.requireNonNull(classNameResolver, "classNameResolver");
        this.typeVariableResolver = Objects.requireNonNull(typeVariableResolver, "typeVariableResolver");
    }

    abstract void finished(TypeName result);

    private String internalName;
    private ClassName rawType;
    private ParameterizedTypeName enclosingType;
    private final List<TypeName> arguments = new ArrayList<>();
    private final Function<String, ClassName> classNameResolver;
    private final Function<String, TypeName> typeVariableResolver;

    @Override
    public void visitTypeVariable(final String name) {
        finished(typeVariableResolver.apply(name));
    }

    @Override
    public void visitBaseType(final char descriptor) {
        finished(toTypeName(descriptor));
    }

    @Override
    public SignatureVisitor visitArrayType() {
        final TypeSignatureParser outer = this;
        return new TypeSignatureParser(classNameResolver, typeVariableResolver) {
            @Override
            void finished(TypeName result) {
                outer.finished(ArrayTypeName.of(result));
            }
        };
    }

    @Override
    public void visitClassType(final String name) {
        internalName = name;
        rawType = classNameResolver.apply(name);
    }

    @Override
    public void visitTypeArgument() {
        arguments.add(WildcardTypeName.subtypeOf(ClassName.OBJECT));
    }

    @Override
    public SignatureVisitor visitTypeArgument(final char wildcard) {
        return new TypeSignatureParser(classNameResolver, typeVariableResolver) {
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
        TypeName owner = currentType();
        enclosingType = owner instanceof ParameterizedTypeName ? (ParameterizedTypeName) owner : null;
        internalName += "$" + name;
        rawType = classNameResolver.apply(internalName);
        arguments.clear();
    }

    @Override
    public void visitEnd() {
        finished(currentType());
    }

    private TypeName currentType() {
        if (enclosingType != null) {
            return enclosingType.nestedClass(rawType.simpleName(), arguments);
        }
        if (arguments.isEmpty()) return rawType;
        return ParameterizedTypeName.get(rawType, arguments.toArray(new TypeName[0]));
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
