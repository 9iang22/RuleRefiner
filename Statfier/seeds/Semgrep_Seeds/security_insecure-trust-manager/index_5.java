package Trust;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

public class GoodTrustManager implements X509TrustManager {

    protected KeyStore loadKeyStore() {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        return ks;
    }

    // ok:insecure-trust-manager
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return loadKeyStore().getCertificate("alias");
    }
}