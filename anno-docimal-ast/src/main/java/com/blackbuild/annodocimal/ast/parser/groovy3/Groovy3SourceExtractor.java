package com.blackbuild.annodocimal.ast.parser.groovy3;

import com.blackbuild.annodocimal.ast.parser.AbstractSourceExtractor;
import org.codehaus.groovy.ast.AnnotatedNode;

public class Groovy3SourceExtractor extends AbstractSourceExtractor {

    private static final Groovy3SourceExtractor INSTANCE = new Groovy3SourceExtractor();

    public static Groovy3SourceExtractor getInstance() {
        return INSTANCE;
    }

    @Override
    public String getJavaDoc(AnnotatedNode node) {
        return reformat(node.getGroovydoc().getContent());
    }
}
