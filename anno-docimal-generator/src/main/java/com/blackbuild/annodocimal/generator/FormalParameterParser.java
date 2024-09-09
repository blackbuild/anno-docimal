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

import com.squareup.javapoet.TypeName;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class FormalParameterParser extends SignatureVisitor {
    private List<TypeName> parameterBounds = new ArrayList<>();
    private final Map<String, List<TypeName>> typeParameters = new LinkedHashMap<>();

    protected FormalParameterParser() {
        super(CompilerConfiguration.ASM_API_VERSION);
    }

    @Override
    public void visitFormalTypeParameter(String name) {
        parameterBounds = new ArrayList<>();
        typeParameters.put(name, parameterBounds);
    }

    @Override
    public SignatureVisitor visitClassBound() {
        return new TypeSignatureParser() {
            @Override
            void finished(TypeName result) {
                parameterBounds.add(result);
            }
        };
    }

    @Override
    public SignatureVisitor visitInterfaceBound() {
        return visitClassBound();
    }

    public Map<String, List<TypeName>> getTypeParameters() {
        return typeParameters;
    }
}
