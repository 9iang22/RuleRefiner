import java.lang.Runtime;

class Cls {

    public Cls() {
        System.out.println("Hello");
    }

    public void test8() {
        // ok: weak-ssl-context
        SSLContext ctx = SSLContext.getInstance(getSslContext());
    }

    public String getSslContext() {
        return "Anything";
    }
}