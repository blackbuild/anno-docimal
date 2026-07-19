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

import org.objectweb.asm.Opcodes;

final class ProjectionSelection {

    private ProjectionSelection() {
    }

    static boolean includesMethod(ProjectionPolicy policy, int access, String name, int ownerAccess) {
        if ("<clinit>".equals(name)) return false;
        if (isImplicitEnumMethod(ownerAccess, name)) return false;
        return includesDeclaration(policy, access) && (policy.isGroovyRuntimeArtifactsIncluded() || !isGroovyMethod(name));
    }

    static boolean includesField(ProjectionPolicy policy, int access, String name, int ownerAccess) {
        if ((access & Opcodes.ACC_ENUM) != 0) return true;
        return includesDeclaration(policy, access) && (policy.isGroovyRuntimeArtifactsIncluded() || !isGroovyField(name));
    }

    static DeclarationVisibility visibility(int access) {
        if ((access & Opcodes.ACC_PUBLIC) != 0) return DeclarationVisibility.PUBLIC;
        if ((access & Opcodes.ACC_PROTECTED) != 0) return DeclarationVisibility.PROTECTED;
        if ((access & Opcodes.ACC_PRIVATE) != 0) return DeclarationVisibility.PRIVATE;
        return DeclarationVisibility.PACKAGE_PRIVATE;
    }

    private static boolean includesDeclaration(ProjectionPolicy policy, int access) {
        return policy.getIncludedVisibilities().contains(visibility(access))
                && (policy.isSyntheticDeclarationsIncluded() || (access & Opcodes.ACC_SYNTHETIC) == 0);
    }

    private static boolean isImplicitEnumMethod(int ownerAccess, String name) {
        return (ownerAccess & Opcodes.ACC_ENUM) != 0 && ("values".equals(name) || "valueOf".equals(name));
    }

    private static boolean isGroovyMethod(String name) {
        return name.startsWith("$")
                || "getMetaClass".equals(name)
                || "setMetaClass".equals(name)
                || "invokeMethod".equals(name)
                || "getProperty".equals(name)
                || "setProperty".equals(name)
                || "previous".equals(name)
                || "next".equals(name);
    }

    private static boolean isGroovyField(String name) {
        return name.startsWith("$") || name.startsWith("__$") || "metaClass".equals(name)
                || "MIN_VALUE".equals(name) || "MAX_VALUE".equals(name);
    }
}
