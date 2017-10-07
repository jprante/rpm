package org.xbib.rpm.security;


/**
 * Enumeration of hash algorithms as of RFC 4880.
 *
 * See also {@link org.bouncycastle.bcpg.HashAlgorithmTags}
 */
public enum HashAlgo {

    MD5("MD5", 1),
    SHA1("SHA", 2),
    RIPEMD160("RIPE-MD160", 3),
    DOUBLESHA("Double-SHA", 4),
    MD2("MD2", 5),
    TIGER192("Tiger-192", 6),
    HAVAL_5_160("Haval-5-160", 7),
    SHA256("SHA-256", 8),
    SHA384("SHA-384", 9),
    SHA512("SHA-512", 10),
    SHA224("SHA-224", 11);

    String algo;

    int num;

    HashAlgo(String algo, int num) {
        this.algo = algo;
        this.num = num;
    }

    public String algo() {
        return algo;
    }

    public int num() {
        return num;
    }
}
