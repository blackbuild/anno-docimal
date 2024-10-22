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
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ClassSignatureParser {

     static void parseClassSignature(String signature, TypeSpec.Builder builder, TypeSpec.Kind kind) {
        final List<TypeName> interfaces = new ArrayList<>();
        FormalParameterParser v = new FormalParameterParser() {

            @Override
            public SignatureVisitor visitSuperclass() {
                return new TypeSignatureParser() {
                    @Override
                    void finished(TypeName result) {
                        if (kind == TypeSpec.Kind.CLASS)
                            builder.superclass(result);
                    }
                };
            }

            @Override
            public SignatureVisitor visitInterface() {
                return new TypeSignatureParser() {
                    @Override
                    void finished(TypeName result) {
                        if (result.toString().equals("groovy.lang.GroovyObject")) return;
                        if (kind == TypeSpec.Kind.ANNOTATION && result.toString().equals("java.lang.annotation.Annotation")) return;
                        interfaces.add(result);
                    }
                };
            }

        };
        new SignatureReader(signature).accept(v);
        v.getTypeParameters().entrySet().stream()
                .map(ClassSignatureParser::toTypeVariable)
                .forEach(builder::addTypeVariable);

        for (TypeName anInterface : interfaces) {
            builder.addSuperinterface(anInterface);
        }
    }

    static TypeVariableName toTypeVariable(Map.Entry<String, List<TypeName>> typeInfo) {
         return TypeVariableName.get(typeInfo.getKey(), typeInfo.getValue().toArray(new TypeName[0]));
    }
}
