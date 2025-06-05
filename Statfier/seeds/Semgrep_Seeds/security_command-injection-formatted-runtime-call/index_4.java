import java.lang.Runtime;

class Cls {

    public void test4(String input) {
        // ruleid: command-injection-formatted-runtime-call
        Runtime.getRuntime().exec("bash", "-c", input);
    }
}