package org.xbib.rpm.security;

import org.bouncycastle.openpgp.PGPException;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *
 */
public class PrintPublicKeyTest {

    @Test
    public void testAsciiArmor() throws IOException, PGPException {
        InputStream inputStream = getClass().getResourceAsStream("/pgp/test-pubring.gpg");
        OutputStream outputStream = Files.newOutputStream(Paths.get("build/test-key.pub"));
        KeyDumper keyDumper = new KeyDumper();
        keyDumper.asciiArmor(0xF02C6D2CL, inputStream, outputStream);
    }
}
