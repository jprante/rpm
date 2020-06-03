package org.xbib.rpm.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.bouncycastle.openpgp.PGPException;
import org.junit.jupiter.api.Test;
import org.xbib.rpm.RpmBuilder;
import org.xbib.rpm.exception.RpmException;
import org.xbib.rpm.lead.Architecture;
import org.xbib.rpm.lead.Os;
import org.xbib.rpm.lead.PackageType;
import org.xbib.rpm.payload.Directive;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;

/**
 *
 */
public class SignatureGeneratorTest {

    @Test
    public void testReadingFirstKey() {
        SignatureGenerator generator =
                new SignatureGenerator(getClass().getResourceAsStream("/pgp/test-secring.gpg"),
                        null, "test");
        assertTrue(generator.isEnabled());
    }

    @Test
    public void testFindByKey() {
        SignatureGenerator generator =
                new SignatureGenerator(getClass().getResourceAsStream("/pgp/test-secring.gpg"),
                        0xF02C6D2CL, "test");
        assertTrue(generator.isEnabled());
    }

    @Test
    public void testBuildWithTestSignature() throws IOException, PGPException, RpmException {
        String pubRing = "build/test-pubring.gpg";
        String secRing = "build/test-secring.gpg";
        String id = "test@example.com";
        String pass = "test";

        // create new key
        KeyGenerator keyGenerator = new KeyGenerator();
        keyGenerator.generate(id, pass, Files.newOutputStream(Paths.get(pubRing)), Files.newOutputStream(Paths.get(secRing)));
        Long privateKeyId = keyGenerator.getPgpSecretKeyRing().getPublicKey().getKeyID();
        //logger.info("key ID = " + Long.toHexString(privateKeyId));
        // dump in ascii-armored format
        KeyDumper keyDumper = new KeyDumper();
        keyDumper.asciiArmor(privateKeyId, Files.newInputStream(Paths.get(pubRing)),
                Files.newOutputStream(Paths.get("build/random-test-key.pub")));

        SignatureGenerator generator = new SignatureGenerator(Files.newInputStream(Paths.get(secRing)),
                privateKeyId, "test");
        assertTrue(generator.isEnabled());

        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.setPackage("signature-my-ring-test", "1.0", "1");
        rpmBuilder.setBuildHost("localhost");
        rpmBuilder.setLicense("GPL");
        rpmBuilder.setPlatform(Architecture.NOARCH, Os.LINUX);
        rpmBuilder.setType(PackageType.BINARY);
        rpmBuilder.setPrivateKeyRing(Files.newInputStream(Paths.get(secRing)));
        rpmBuilder.setPrivateKeyPassphrase(pass);
        EnumSet<Directive> directives = EnumSet.of(Directive.CONFIG, Directive.DOC, Directive.NOREPLACE);
        rpmBuilder.addFile("/etc", Paths.get("src/test/resources/prein.sh"), 493, 493,
                directives, "jabberwocky", "vorpal", true);
        rpmBuilder.build(Paths.get("build"));
    }

    @Test
    public void testBuildWithSignature() throws IOException, RpmException {
        String secRing = "src/test/resources/pgp/test-secring.gpg";
        String pass = "test";
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.setPackage("signing-test", "1.0", "3");
        rpmBuilder.setBuildHost("localhost");
        rpmBuilder.setLicense("GPL");
        rpmBuilder.setPlatform(Architecture.NOARCH, Os.LINUX);
        rpmBuilder.setType(PackageType.BINARY);
        rpmBuilder.setPrivateKeyRing(Files.newInputStream(Paths.get(secRing)));
        rpmBuilder.setPrivateKeyPassphrase(pass);
        rpmBuilder.build(Paths.get("build"));
    }
}
