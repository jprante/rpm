package org.xbib.rpm.security;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.xbib.rpm.exception.RpmException;
import org.xbib.rpm.header.entry.SpecEntry;
import org.xbib.rpm.io.ChannelWrapper;
import org.xbib.rpm.io.WritableChannelWrapper;
import org.xbib.rpm.signature.SignatureHeader;
import org.xbib.rpm.signature.SignatureTag;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Iterator;

/**
 * To verify the authenticity of the package, the SIGTAG_PGP tag holds a
 * Version 3 OpenPGP Signature Packet RSA signature of the header and payload areas.
 * The SIGTAG_GPG tag holds a Version 3 OpenPGP Signature Packet DSA signature of the header and payload areas.
 * The SIGTAG_DSAHEADER holds a DSA signature of just the header section.
 * If the SIGTAG_DSAHEADER tag is included, the SIGTAG_GPG tag must also be present.
 * The SIGTAG_ RSAHEADER holds an RSA signature of just the header section.
 * If the SIGTAG_ RSAHEADER tag is included, the SIGTAG_PGP tag must also be present.
 */
public class SignatureGenerator {

    private final boolean enabled;

    private PGPPrivateKey privateKey;

    private SpecEntry<byte[]> headerOnlyEntry;

    private SpecEntry<byte[]> headerAndPayloadEntry;

    private ChannelWrapper.Key<byte[]> headerOnlyKey;

    private ChannelWrapper.Key<byte[]> headerAndPayloadKey;

    public SignatureGenerator(InputStream privateKeyRing, Long privateKeyId, String privateKeyPassphrase) {
        if (privateKeyRing != null) {
            PGPSecretKeyRingCollection keyRings = readKeyRing(privateKeyRing);
            PGPSecretKey secretKey = findMatchingSecretKey(keyRings, privateKeyId);
            this.privateKey = extractPrivateKey(secretKey, privateKeyPassphrase);;
            this.enabled = privateKey != null;
        } else {
            this.enabled = false;
        }
    }

    @SuppressWarnings("unchecked")
    public void prepare(SignatureHeader signature, HashAlgo algo) {
        if (enabled) {
            int count = 287;
            headerOnlyEntry = (SpecEntry<byte[]>) signature.addEntry(SignatureTag.RSAHEADER, count);
            headerAndPayloadEntry = (SpecEntry<byte[]>) signature.addEntry(SignatureTag.LEGACY_PGP, count);
        }
    }

    public void startBeforeHeader(WritableChannelWrapper output, HashAlgo algo) throws RpmException {
        if (enabled) {
            try {
                headerOnlyKey = output.start(new SignatureConsumer(algo.num()));
                headerAndPayloadKey = output.start(new SignatureConsumer(algo.num()));
            } catch (PGPException e) {
                throw new RpmException(e);
            }
        }
    }

    /**
     * Start a digest.
     *
     * @param output the output channel
     * @param digest the message digest
     * @throws RpmException if digest could not be generated
     * @return reference to the new key added to the consumers
     */
    public ChannelWrapper.Key<byte[]> startDigest(WritableChannelWrapper output, String digest) throws RpmException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(digest);
            ChannelWrapper.Consumer<byte[]> consumer = new ChannelWrapper.Consumer<>() {
                @Override
                public void consume(ByteBuffer buffer) {
                    try {
                        messageDigest.update(buffer);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public byte[] finish() {
                    try {
                        return messageDigest.digest();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            return output.start(consumer);
        } catch (NoSuchAlgorithmException e) {
            throw new RpmException(e);
        }
    }

    private static int getTempArraySize(int totalSize) {
        return Math.min(4096, totalSize);
    }

    public void finishAfterHeader(WritableChannelWrapper output) {
        finishEntry(output, headerOnlyEntry, headerOnlyKey);
    }

    public void finishAfterPayload(WritableChannelWrapper output) {
        finishEntry(output, headerAndPayloadEntry, headerAndPayloadKey);
    }

    public boolean isEnabled() {
        return enabled;
    }

    private PGPSecretKeyRingCollection readKeyRing(InputStream privateKeyRing) {
        try {
            try (InputStream decoderStream = PGPUtil.getDecoderStream(new BufferedInputStream(privateKeyRing))) {
                return new PGPSecretKeyRingCollection(decoderStream, new JcaKeyFingerprintCalculator());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read key ring", e);
        } catch (PGPException e) {
            throw new IllegalArgumentException("Could not extract key ring", e);
        }
    }

    private PGPSecretKey findMatchingSecretKey(PGPSecretKeyRingCollection keyRings, Long privateKeyId) {
        Iterator<PGPSecretKeyRing> iter = keyRings.getKeyRings();
        while (iter.hasNext()) {
            PGPSecretKeyRing keyRing = iter.next();
            Iterator<PGPSecretKey> keyIter = keyRing.getSecretKeys();
            while (keyIter.hasNext()) {
                PGPSecretKey key = keyIter.next();
                if (key.isSigningKey() && isMatchingKeyId(key, privateKeyId)) {
                    return key;
                }
            }
        }
        throw new IllegalArgumentException("can't find signing key in key rings");
    }

    private boolean isMatchingKeyId(PGPSecretKey key, Long privateKeyId) {
        return privateKeyId == null || Long.toHexString(key.getKeyID()).endsWith(Long.toHexString(privateKeyId));
    }

    private PGPPrivateKey extractPrivateKey(PGPSecretKey secretKey, String privateKeyPassphrase) {
        try {
            PBESecretKeyDecryptor secretKeyDecryptor =
                    new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider())
                            .build(privateKeyPassphrase.toCharArray());
            return secretKey.extractPrivateKey(secretKeyDecryptor);
        } catch (Exception e) {
            throw new IllegalArgumentException("could not extract private key from key ring", e);
        }
    }

    private void finishEntry(WritableChannelWrapper output, SpecEntry<byte[]> entry, ChannelWrapper.Key<byte[]> key) {
        if (enabled) {
            if (key == null) {
                throw new IllegalStateException("key is not initialized");
            }
            if (entry == null) {
                throw new IllegalStateException("entry not initialized");
            }
            byte[] b = output.finish(key);
            entry.setCount(b.length);
            entry.setValues(b);
        }
    }

    class SignatureConsumer implements ChannelWrapper.Consumer<byte[]> {

        PGPSignatureGenerator pgpSignatureGenerator;

        SignatureConsumer(int hashAlgorithm) throws PGPException {
            int keyAlgorithm = privateKey.getPublicKeyPacket().getAlgorithm();
            BcPGPContentSignerBuilder contentSignerBuilder =
                    new BcPGPContentSignerBuilder(keyAlgorithm, hashAlgorithm);
            this.pgpSignatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
            pgpSignatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);
        }

        @Override
        public void consume(ByteBuffer buffer) {
            if (!buffer.hasRemaining()) {
                return;
            }
            write(buffer);
        }

        private void write(ByteBuffer buffer) {
            if (buffer.hasArray()) {
                byte[] bufferBytes = buffer.array();
                int offset = buffer.arrayOffset();
                int position = buffer.position();
                int limit = buffer.limit();
                pgpSignatureGenerator.update(bufferBytes, offset + position, limit - position);
                buffer.position(limit);
            } else {
                int length = buffer.remaining();
                byte[] bytes = new byte[getTempArraySize(length)];
                while (length > 0) {
                    int chunk = Math.min(length, bytes.length);
                    buffer.get(bytes, 0, chunk);
                    pgpSignatureGenerator.update(bytes, 0, chunk);
                    length -= chunk;
                }
            }
        }

        @Override
        public byte[] finish() {
            try {
                return pgpSignatureGenerator.generate().getEncoded();
            } catch (Exception e) {
                throw new RuntimeException("could not generate signature", e);
            }
        }
    }
}
