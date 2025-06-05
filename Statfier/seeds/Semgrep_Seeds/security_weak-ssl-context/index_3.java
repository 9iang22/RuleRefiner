import java.lang.Runtime;

class Cls {

    public Cls() {
        System.out.println("Hello");
    }

    public void test4() {
        // ruleid: weak-ssl-context
        SSLContext ctx = SSLContext.getInstance("SSLv3");
    }

    public String getSslContext() {
        return "Anything";
    }
}