import java.lang.Runtime;

class Cls {

    public Cls() {
        System.out.println("Hello");
    }

    public void test3() {
        // ruleid: weak-ssl-context
        SSLContext ctx = SSLContext.getInstance("TLSv1");
    }

    public String getSslContext() {
        return "Anything";
    }
}