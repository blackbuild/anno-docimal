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

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javax.lang.model.element.Modifier;
import java.util.EnumSet;

public class TypeConversion {

    private TypeConversion() {}


    static String fromInternalName(String name) {
        return name.replace('/', '.');
    }

    static ClassName fromInternalNameToClassName(String name) {
        String packageName = name.substring(0, name.lastIndexOf('/')).replace('/', '.');
        String className = name.substring(name.lastIndexOf('/') + 1);

        if (!className.contains("$")) {
            return ClassName.get(packageName, className);
        }

        String outerClassName = className.substring(0, className.lastIndexOf('$'));
        String nestedClassName = className.substring(className.lastIndexOf('$') + 1);

        return ClassName.get(packageName, outerClassName, nestedClassName.split("\\$"));
    }

    static Modifier[] decodeModifiers(int flags) {
        EnumSet<Modifier> result = EnumSet.noneOf(Modifier.class);

        if ((flags & Opcodes.ACC_PUBLIC) != 0) result.add(Modifier.PUBLIC);
        if ((flags & Opcodes.ACC_PRIVATE) != 0) result.add(Modifier.PRIVATE);
        if ((flags & Opcodes.ACC_PROTECTED) != 0) result.add(Modifier.PROTECTED);
        if ((flags & Opcodes.ACC_STATIC) != 0) result.add(Modifier.STATIC);
        if ((flags & Opcodes.ACC_FINAL) != 0) result.add(Modifier.FINAL);
        if ((flags & Opcodes.ACC_ABSTRACT) != 0) result.add(Modifier.ABSTRACT);

        return result.toArray(new Modifier[0]);
    }

    static TypeName toTypeName(Type type) {
        switch (type.getSort()) {
            case Type.OBJECT:
                return fromInternalNameToClassName(type.getInternalName());
            case Type.ARRAY:
                return ArrayTypeName.of(toTypeName(type.getElementType()));
            case Type.METHOD:
                throw new IllegalArgumentException("Method type not supported");
            case Type.VOID:
                return TypeName.VOID;
            case Type.BOOLEAN:
                return TypeName.BOOLEAN;
            case Type.BYTE:
                return TypeName.BYTE;
            case Type.CHAR:
                return TypeName.CHAR;
            case Type.DOUBLE:
                return TypeName.DOUBLE;
            case Type.FLOAT:
                return TypeName.FLOAT;
            case Type.INT:
                return TypeName.INT;
            case Type.LONG:
                return TypeName.LONG;
            case Type.SHORT:
                return TypeName.SHORT;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }
}
