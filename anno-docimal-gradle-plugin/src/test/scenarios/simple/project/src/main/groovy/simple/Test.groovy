package simple

import com.blackbuild.annodocimal.annotations.AnnoDoc

@AnnoDoc("This is a test class")
class Test {

    @AnnoDoc("A method")
    void aMethod() {}

    @AnnoDoc("""Method with param

@param input The input""")
    String methodWithParam(String input) {
        return input.toLowerCase()
    }

    @AnnoDoc("Inner class")
    static class InnerClass {
        @AnnoDoc("Inner class method")
        void innerMethod() {}
    }
}