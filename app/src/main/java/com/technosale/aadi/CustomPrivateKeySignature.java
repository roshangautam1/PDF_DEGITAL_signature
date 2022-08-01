package com.technosale.aadi;
import com.itextpdf.text.pdf.security.DigestAlgorithms;
import com.itextpdf.text.pdf.security.ExternalSignature;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
public class CustomPrivateKeySignature  implements ExternalSignature {
    private PrivateKey pk;
    private String hashAlgorithm;
    private String encryptionAlgorithm;
    private String provider;

    public CustomPrivateKeySignature(PrivateKey pk, String hashAlgorithm, String provider) {
        this.pk = pk;
        this.provider = provider;
        this.hashAlgorithm = DigestAlgorithms.getDigest(DigestAlgorithms.getAllowedDigests(hashAlgorithm));
        this.encryptionAlgorithm = pk.getAlgorithm();
        if(this.encryptionAlgorithm.startsWith("EC")) {
            this.encryptionAlgorithm = "ECDSA";
        }
    }

    public String getHashAlgorithm() {
        return this.hashAlgorithm;
    }

    public String getEncryptionAlgorithm() {
        return this.encryptionAlgorithm;
    }

    public byte[] sign(byte[] b) throws GeneralSecurityException {
        String signMode = this.hashAlgorithm + "with" + this.encryptionAlgorithm;
        Signature sig;
        sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(this.pk);
        sig.update(b);
        return sig.sign();
    }
}
