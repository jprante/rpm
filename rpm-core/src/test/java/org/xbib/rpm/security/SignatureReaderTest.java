package org.xbib.rpm.security;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.xbib.rpm.RpmReader;
import org.xbib.rpm.format.Format;
import org.xbib.rpm.signature.SignatureHeader;
import org.xbib.rpm.signature.SignatureTag;

import java.nio.file.Paths;

/**
 *
 */
public class SignatureReaderTest {

    @Test
    public void readBadSignedRpm() throws Exception {
        RpmReader rpmReader = new RpmReader();
        Format format = rpmReader.read(Paths.get("src/test/resources/signature-my-ring-test-1.0-1.noarch.rpm"));
        SignatureHeader signatureHeader = format.getSignatureHeader();
        assertNotNull(signatureHeader.getEntry(SignatureTag.RSAHEADER));
        assertNotNull(signatureHeader.getEntry(SignatureTag.LEGACY_PGP));
    }

    @Test
    public void readGoodSignedRpm() throws Exception {
        RpmReader rpmReader = new RpmReader();
        Format format = rpmReader.read(Paths.get("src/test/resources/signing-test-1.0-1.noarch.rpm"));
        SignatureHeader signatureHeader = format.getSignatureHeader();
        assertNotNull(signatureHeader.getEntry(SignatureTag.RSAHEADER));
        assertNotNull(signatureHeader.getEntry(SignatureTag.LEGACY_PGP));
    }
}
