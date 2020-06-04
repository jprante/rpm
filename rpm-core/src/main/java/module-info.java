module org.xbib.rpm {
    exports org.xbib.rpm;
    exports org.xbib.rpm.changelog;
    exports org.xbib.rpm.exception;
    exports org.xbib.rpm.format;
    exports org.xbib.rpm.header;
    exports org.xbib.rpm.header.entry;
    exports org.xbib.rpm.io;
    exports org.xbib.rpm.lead;
    exports org.xbib.rpm.payload;
    exports org.xbib.rpm.security;
    exports org.xbib.rpm.signature;
    exports org.xbib.rpm.trigger;
    requires transitive org.bouncycastle.pg;
    requires transitive org.bouncycastle.provider;
    requires org.xbib.io.compress.bzip;
    requires org.xbib.io.compress.xz;
}
