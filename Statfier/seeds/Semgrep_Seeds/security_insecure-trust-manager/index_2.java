package Trust;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

//cf. https://find-sec-bugs.github.io/bugs.htm#WEAK_TRUST_MANAGER
public class TrustAllManager implements X509TrustManager {

    // ruleid:insecure-trust-manager
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}