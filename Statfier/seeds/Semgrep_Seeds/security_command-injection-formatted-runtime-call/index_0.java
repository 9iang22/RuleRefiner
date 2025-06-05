import java.lang.Runtime;

class Cls {

    public Cls(String input) {
        Runtime r = Runtime.getRuntime();
        // ruleid: command-injection-formatted-runtime-call
        r.exec("/bin/sh -c some_tool" + input);
    }
}