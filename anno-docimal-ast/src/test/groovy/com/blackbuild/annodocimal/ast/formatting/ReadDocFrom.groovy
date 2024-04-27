package com.blackbuild.annodocimal.ast.formatting

import com.blackbuild.annodocimal.ast.MockableTransformation
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.RUNTIME)
@GroovyASTTransformationClass(classes = MockableTransformation)
@interface ReadDocFrom {
    Class<? extends MockableTransformation.Action> value()
    Class<?> type()
    String method() default ""
    String field() default ""
}