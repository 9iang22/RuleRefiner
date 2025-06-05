import java.lang.Runtime;

class Cls {

    public void okTest(String input) {
        Runtime r = Runtime.getRuntime();
        // ok: command-injection-formatted-runtime-call
        r.exec("echo 'blah'");
    }
}