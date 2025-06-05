import java.lang.Runtime;

class Cls {

    public void test1(String input) {
        Runtime r = Runtime.getRuntime();
        // ruleid: command-injection-formatted-runtime-call
        r.loadLibrary(String.format("%s.dll", input));
    }
}