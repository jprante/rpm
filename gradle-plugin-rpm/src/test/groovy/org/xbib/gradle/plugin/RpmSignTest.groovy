package org.xbib.gradle.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.xbib.rpm.RpmReader
import org.xbib.rpm.signature.SignatureTag
import java.nio.file.Paths
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertNull
import static org.xbib.rpm.lead.Architecture.I386

class RpmSignTest {

    File projectDir

    @BeforeEach
    void setup() {
        projectDir = new File("build/testrpm")
        projectDir.mkdirs()
    }

    @Test
    void testUnsigned() {
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'bleah'
            packageVersion = '1.0'
            packageRelease = '1'
            arch = I386
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        def res = rpmReader.read(Paths.get(projectDir.toString(), 'build', 'distributions', 'bleah-1.0-1.i386.rpm'))
        assertNull(res.format.signatureHeader.getEntry(SignatureTag.LEGACY_PGP))
    }

    @Test
    void testSign() {
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'bleah'
            packageVersion = '1.0'
            packageRelease = '1'
            arch = I386
            signingKeyId = 'F02C6D2C'
            signingKeyPassphrase = 'test'
            signingKeyRing = 'src/test/resources/test-secring.gpg'
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        def res = rpmReader.read(Paths.get(projectDir.toString(), 'build', 'distributions', 'bleah-1.0-1.i386.rpm'))
        assertNotNull(res.format.signatureHeader.getEntry(SignatureTag.LEGACY_PGP))
    }
}
