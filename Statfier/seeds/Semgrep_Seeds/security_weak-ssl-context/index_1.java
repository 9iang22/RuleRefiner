import java.lang.Runtime;

class Cls {

    public Cls() {
        System.out.println("Hello");
    }

    public void test2() {
        // ruleid: weak-ssl-context
        SSLContext ctx = SSLContext.getInstance("TLS");
    }

    public String getSslContext() {
        return "Anything";
    }
}