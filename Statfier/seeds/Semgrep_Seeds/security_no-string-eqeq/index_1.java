public class Example {
    public int foo(String a, int b) {
        // ok:no-string-eqeq
        if (b == 2) return -1;
        return 0;
    }
}