package com.blackbuild.annodocimal.ast

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.RUNTIME)
@GroovyASTTransformationClass(classes = MockableTransformation)
@interface MockAST {
    Class<? extends MockableTransformation.Action> value()
}