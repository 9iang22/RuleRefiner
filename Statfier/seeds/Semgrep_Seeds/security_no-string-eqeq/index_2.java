public class Example {
    public int foo(String a, int b) {
        // ruleid:no-string-eqeq
        if ("hello" == a) return 2;
        return 0;
    }
}