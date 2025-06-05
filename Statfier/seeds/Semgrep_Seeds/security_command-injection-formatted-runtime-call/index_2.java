import java.lang.Runtime;

class Cls {

    public void test2(String input) {
        Runtime r = Runtime.getRuntime();
        // ruleid: command-injection-formatted-runtime-call
        r.exec("bash", "-c", input);
    }
}