import javax;

import javax.*;
// import javax.crypto;

import javax.crypto.*;
// import javax.crypto.Cipher;

class AES{
  public void ok() {
    // ok: use-of-default-aes
    KeyGenerator.getInstance("AES/CBC/PKCS7PADDING");
  }
}