package com.blackbuild.annodocimal.ast.extractor.mock;

import com.blackbuild.annodocimal.annotations.InlineJavadocs;

/**
 * A class for testing.
 */
@InlineJavadocs
public class AClass {

    /**
     * Creates a new instance of {@link AClass}.
     */
    public AClass() {}

    /**
     * A method that does nothing.
     */
    public void aMethod() {
        // do nothing
    }

    public void noJavaDocMethod() {
        // do nothing
    }

    /**
     * A method that does something.
     * @param what the thing to do
     * @return the result of doing it
     */
    public String doIt(String what) {
        return "I did it: " + what;
    }

    /**
     * A field.
     */
    public String field = "field";

    /**
     * An inner class.
     */
    static class InnerClass {
        /**
         * Another method that does nothing.
         */
        public void innerMethod() {}
    }

}