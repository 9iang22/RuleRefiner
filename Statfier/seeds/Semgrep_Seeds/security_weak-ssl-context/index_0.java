import java.lang.Runtime;

class Cls {

    public Cls() {
        System.out.println("Hello");
    }

    public void test1() {
        // ruleid: weak-ssl-context
        SSLContext ctx = SSLContext.getInstance("SSL");
    }

    public String getSslContext() {
        return "Anything";
    }
}