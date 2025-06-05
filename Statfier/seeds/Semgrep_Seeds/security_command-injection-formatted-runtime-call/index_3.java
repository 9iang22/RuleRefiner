import java.lang.Runtime;

class Cls {

    public void test3(String input) {
        // ruleid: command-injection-formatted-runtime-call
        Runtime.getRuntime().loadLibrary(String.format("%s.dll", input));
    }
}