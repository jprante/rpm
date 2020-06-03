package org.xbib.gradle.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.xbib.rpm.RpmReaderResult
import org.xbib.rpm.RpmReader
import org.xbib.rpm.format.Format
import java.nio.file.Paths
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.CoreMatchers.*
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.xbib.rpm.format.Flags.EQUAL
import static org.xbib.rpm.format.Flags.GREATER
import static org.xbib.rpm.header.HeaderTag.ARCH
import static org.xbib.rpm.header.HeaderTag.DISTRIBUTION
import static org.xbib.rpm.header.HeaderTag.EPOCH
import static org.xbib.rpm.header.HeaderTag.NAME
import static org.xbib.rpm.header.HeaderTag.OS
import static org.xbib.rpm.header.HeaderTag.RELEASE
import static org.xbib.rpm.header.HeaderTag.VERSION
import static org.xbib.rpm.lead.Architecture.I386
import static org.xbib.rpm.lead.Os.LINUX
import static org.xbib.rpm.lead.PackageType.BINARY

class RpmSimpleTest {

    File projectDir

    @BeforeEach
    void setup() {
        projectDir = new File("build/testrpm")
        projectDir.mkdirs()
    }

    @Test
    void simpleRpm() {
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'apple').withWriter { out ->
            out.write('apple')
        }
        File noParentsDir = new File(projectDir, 'noParentsDir')
        noParentsDir.mkdirs()
        new File(noParentsDir, 'alone').withWriter { out ->
            out.write('alone')
        }
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'bleah'
            packageVersion = '1.0'
            packageRelease = '1'
            packageType = BINARY
            arch = I386
            os = LINUX
            packageGroup = 'Development/Libraries'
            packageDescription = 'Not a very interesting library.'
            summary = 'Bleah blarg'
            license = 'Free'
            distribution = 'SuperSystem'
            vendor = 'Super Associates, LLC'
            url = 'http://www.example.com/'
            requires('blarg', '1.0', GREATER | EQUAL)
            requires('blech')
            into '/opt/bleah'
            from('src') {
                fileType = ['config', 'noreplace']
            }
            from('noParentsDir') {
                into '/a/path/not/to/create'
            }
            link('/opt/bleah/banana', '/opt/bleah/apple')
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        RpmReaderResult result = rpmReader.read(Paths.get(projectDir.toString(), 'build', 'distributions', 'bleah-1.0-1.i386.rpm'))
        Format format = result.format
        assertThat(['bleah'], is(format.header.getEntry(NAME)?.values))
        assertThat(['1.0'], is(format.header.getEntry(VERSION)?.values))
        assertThat(['1'], is(format.header.getEntry(RELEASE)?.values))
        assertThat([0], is(format.header.getEntry(EPOCH)?.values))
        assertThat(['i386'], is(format.header.getEntry(ARCH)?.values))
        assertThat(['linux'], is(format.header.getEntry(OS)?.values))
        assertThat(['SuperSystem'], is(format.header.getEntry(DISTRIBUTION)?.values))
        assertTrue(result.files*.name.every { fileName ->
            ['./a/path/not/to/create/alone',
             './opt/bleah',
             './opt/bleah/apple',
             './opt/bleah/banana'
            ].any { path ->
                path.startsWith(fileName)
            }
        })
        assertTrue(result.files*.name.every { fileName ->
            ['./a/path/not/to/create'].every { path ->
                !path.startsWith(fileName)
            }
        })
    }
}
