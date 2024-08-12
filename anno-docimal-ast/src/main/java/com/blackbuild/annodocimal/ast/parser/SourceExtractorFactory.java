package com.blackbuild.annodocimal.ast.parser;

import groovy.lang.GroovySystem;
import org.codehaus.groovy.control.SourceUnit;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class SourceExtractorFactory {

    private static final SourceExtractorFactory INSTANCE = new SourceExtractorFactory();
    public static final String G3_EXTRACTOR_CLASS_NAME = "com.blackbuild.annodocimal.ast.parser.groovy3.Groovy3SourceExtractor";

    public static SourceExtractorFactory getInstance() {
        return INSTANCE;
    }

    public SourceExtractor createSourceExtractor(SourceUnit sourceUnit) {
        if (GroovySystem.getVersion().startsWith("2.")) {
            try {
                return GroovyDocToolSourceExtractor.create(sourceUnit);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // use reflection since we are compatible to Groovy2
        // FIXME #2
        Class<?> aClass = null;
        try {
            aClass = this.getClass().getClassLoader().loadClass(G3_EXTRACTOR_CLASS_NAME);
            return (SourceExtractor) aClass.getDeclaredMethod("getInstance").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
