public class Example {
    public int foo(String a, int b) {
        // ok:no-string-eqeq
        if ("hello" == null) return 0;
        return 0;
    }
}