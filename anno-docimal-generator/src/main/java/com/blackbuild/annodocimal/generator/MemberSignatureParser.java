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

/**
 * Utility methods for lazy class loading
 */
class MemberSignatureParser {

    /*
    static void createMethodNode(MethodSpec.Builder method, String desc, String signature, ) {
        GenericsType[] typeParameters = null;

        Type[] argumentTypes = Type.getArgumentTypes(desc);
        final TypeName[] parameterTypes = new TypeName[argumentTypes.length];
        for (int i = 0; i < argumentTypes.length; i++) {
            parameterTypes[i] = ClassName.bestGuess(argumentTypes[i].getClassName());
        }

        final TypeName[] exceptions = new TypeName[method.exceptions.length];
        for (int i = 0; i < method.exceptions.length; i++) {
            exceptions[i] = resolver.resolveClass(AsmDecompiler.fromInternalName(method.exceptions[i]));
        }

        final ClassNode[] returnType = {resolver.resolveType(Type.getReturnType(method.desc))};

        if (signature != null) {
            FormalParameterParser v = new FormalParameterParser(resolver) {
                int paramIndex = 0;

                @Override
                public SignatureVisitor visitParameterType() {
                    return new TypeSignatureParser(resolver) {
                        @Override
                        void finished(ClassNode result) {
                            parameterTypes[paramIndex] = applyErasure(result, parameterTypes[paramIndex]);
                            paramIndex++;
                        }
                    };
                }

                @Override
                public SignatureVisitor visitReturnType() {
                    return new TypeSignatureParser(resolver) {
                        @Override
                        void finished(ClassNode result) {
                            returnType[0] = applyErasure(result, returnType[0]);
                        }
                    };
                }

                int exceptionIndex = 0;

                @Override
                public SignatureVisitor visitExceptionType() {
                    return new TypeSignatureParser(resolver) {
                        @Override
                        void finished(ClassNode result) {
                            exceptions[exceptionIndex] = applyErasure(result, exceptions[exceptionIndex]);
                            exceptionIndex++;
                        }
                    };
                }
            };
            new SignatureReader(method.signature).accept(v);
            typeParameters = v.getTypeParameters();
        } else {
            returnType[0] = GenericsUtils.nonGeneric(returnType[0]);
            for (int i = 0, n = parameterTypes.length; i < n; i += 1) {
                parameterTypes[i] = GenericsUtils.nonGeneric(parameterTypes[i]);
            }
        }

        Parameter[] parameters = new Parameter[parameterTypes.length];
        List<String> parameterNames = method.parameterNames;
        for (int i = 0; i < parameterTypes.length; i++) {
            String parameterName = "param" + i;
            if (parameterNames != null && i < parameterNames.size()) {
                String decompiledName = parameterNames.get(i);
                if (decompiledName != null) {
                    parameterName = decompiledName;
                }
            }
            parameters[i] = new Parameter(parameterTypes[i], parameterName);
        }

        if (method.parameterAnnotations != null) {
            for (Map.Entry<Integer, List<AnnotationStub>> entry : method.parameterAnnotations.entrySet()) {
                for (AnnotationStub stub : entry.getValue()) {
                    AnnotationNode annotationNode = Annotations.createAnnotationNode(stub, resolver);
                    if (annotationNode != null) {
                        parameters[entry.getKey()].addAnnotation(annotationNode);
                    }
                }
            }
        }

        MethodNode result;
        if ("<init>".equals(method.methodName)) {
            result = new ConstructorNode(method.accessModifiers, parameters, exceptions, null);
        } else {
            result = new MethodNode(method.methodName, method.accessModifiers, returnType[0], parameters, exceptions, null);
            Object annDefault = method.annotationDefault;
            if (annDefault != null) {
                if (annDefault instanceof TypeWrapper) {
                    annDefault = resolver.resolveType(Type.getType(((TypeWrapper) annDefault).desc));
                }
                result.setCode(new ReturnStatement(new ConstantExpression(annDefault)));
                result.setAnnotationDefault(true);
            } else {
                // Seems wrong but otherwise some tests fail (e.g. TestingASTTransformsTest)
                result.setCode(new ReturnStatement(nullX()));
            }

        }
        if (typeParameters != null && typeParameters.length > 0) {
            result.setGenericsTypes(typeParameters);
        }
        return result;
    }

    private static ClassNode applyErasure(ClassNode genericType, ClassNode erasure) {
        if (genericType.isArray() && erasure.isArray() && genericType.getComponentType().isGenericsPlaceHolder()) {
            genericType.setRedirect(erasure);
            genericType.getComponentType().setRedirect(erasure.getComponentType());
        } else if (genericType.isGenericsPlaceHolder()) {
            genericType.setRedirect(erasure);
        }
        return genericType;
    }
     */

    /*
    static FieldNode createFieldNode(FieldStub field, AsmReferenceResolver resolver, DecompiledClassNode owner) {
        final ClassNode[] type = {resolver.resolveType(Type.getType(field.desc))};
        if (field.signature != null) {
            new SignatureReader(field.signature).accept(new TypeSignatureParser(resolver) {
                @Override
                void finished(ClassNode result) {
                    type[0] = applyErasure(result, type[0]);
                }
            });
        } else {
            // ex: java.util.Collections#EMPTY_LIST/EMPTY_MAP/EMPTY_SET
            type[0] = GenericsUtils.nonGeneric(type[0]);
        }
        ConstantExpression value = field.value == null ? null : new ConstantExpression(field.value);
        return new FieldNode(field.fieldName, field.accessModifiers, type[0], owner, value);
    }

     */
}
