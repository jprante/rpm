package org.xbib.rpm.security;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 *
 */
public class KeyDumper {

    public void asciiArmor(Long keyID, InputStream publicKeyRingStream,
                         OutputStream armoredOutputStream) throws PGPException, IOException {
        PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(publicKeyRingStream), new JcaKeyFingerprintCalculator());
        PGPPublicKey publicKey = findMatchingPublicKey(pgpPub, keyID);
        ArmoredOutputStream armored = new ArmoredOutputStream(armoredOutputStream);
        publicKey.encode(armored);
        armored.close();
    }

    private PGPPublicKey findMatchingPublicKey(PGPPublicKeyRingCollection keyRings, Long privateKeyId) {
        Iterator<PGPPublicKeyRing> iter = keyRings.getKeyRings();
        while (iter.hasNext()) {
            PGPPublicKeyRing keyRing = iter.next();
            @SuppressWarnings("unchecked")
            Iterator<PGPPublicKey> keyIter = keyRing.getPublicKeys();
            while (keyIter.hasNext()) {
                PGPPublicKey key = keyIter.next();
                if (key.isEncryptionKey() && isMatchingKeyId(key, privateKeyId)) {
                    return key;
                }
            }
        }
        throw new IllegalArgumentException("can't find signing key in key rings");
    }

    private boolean isMatchingKeyId(PGPPublicKey key, Long privateKeyId) {
        return privateKeyId == null || Long.toHexString(key.getKeyID()).endsWith(Long.toHexString(privateKeyId));
    }

}
