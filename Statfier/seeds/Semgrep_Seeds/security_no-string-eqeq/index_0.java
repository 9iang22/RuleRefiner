public class Example {
    public int foo(String a, int b) {
        // ruleid:no-string-eqeq
        if (a == "hello") return 1;
        return 0;
    }
}