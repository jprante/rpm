package org.xbib.rpm.signature;

import org.xbib.rpm.header.EntryType;

/**
 *
 */
public enum SignatureTag implements EntryType {

    SIGNATURES(62, BIN_ENTRY, "signatures"),

    SIGSIZE(257, INT32_ENTRY, "sigsize"),
    // 258 is obsolete
    PGP(259, BIN_ENTRY, "pgp"),
    // 260 is obsolete
    MD5(261, BIN_ENTRY, "md5"),
    GPG(262, BIN_ENTRY, "gpg"),
    // 263, 264, 265 are obsolete
    PUBKEYS(266, STRING_LIST_ENTRY, "pubkeys"),
    DSAHEADER(267, BIN_ENTRY, "dsaheader"),
    RSAHEADER(268, BIN_ENTRY, "rsaheader"),
    SHA1HEADER(269, STRING_ENTRY, "sha1header"),
    LONGSIGSIZE(270, INT64_ENTRY, "longsigsize"),
    LONGARCHIVESIZE(271, INT64_ENTRY, "longarchivesize"),
    // 272 is reserved
    SHA256(273, BIN_ENTRY, "sha256"),

    LEGACY_SIGSIZE(1000, INT32_ENTRY, "sigsize"),
    LEGACY_PGP(1002, BIN_ENTRY, "pgp"),
    LEGACY_MD5(1004, BIN_ENTRY, "md5"),
    LEGACY_GPG(1005, BIN_ENTRY, "gpg"),
    PAYLOADSIZE(1007, INT32_ENTRY, "payloadsize"),
    LEGACY_SHA1HEADER(1010, STRING_ENTRY, "sha1header"),
    LEGACY_DSAHEADER(1011, BIN_ENTRY, "dsaheader"),
    LEGACY_RSAHEADER(1012, BIN_ENTRY, "rsaheader")

    ;

    private final int code;

    private final int type;

    private final String name;

    SignatureTag(final int code, final int type, final String name) {
        this.code = code;
        this.type = type;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

}
