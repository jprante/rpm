package org.xbib.rpm.format;

import org.xbib.rpm.header.Header;
import org.xbib.rpm.lead.Lead;
import org.xbib.rpm.signature.SignatureHeader;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 *
 */
public class Format {

    private Lead lead = new Lead();

    private Header header = new Header();

    private SignatureHeader signatureHeader = new SignatureHeader();

    public Lead getLead() {
        return lead;
    }

    public Header getHeader() {
        return header;
    }

    public SignatureHeader getSignatureHeader() {
        return signatureHeader;
    }

    public void read(ReadableByteChannel channel) throws IOException {
        lead.read(channel);
        signatureHeader.read(channel);
        header.read(channel);
    }

    public void write(FileChannel channel) throws IOException {
        lead.write(channel);
        signatureHeader.write(channel);
        header.write(channel);
    }

    public String toString() {
        return lead.toString() + signatureHeader + header;
    }
}
