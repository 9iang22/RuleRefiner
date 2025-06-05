import java.lang.Runtime;

class Cls {

    public void okTest2(String input) {
        // ok: command-injection-formatted-runtime-call
        Runtime.getRuntime().loadLibrary("lib.dll");
    }
}