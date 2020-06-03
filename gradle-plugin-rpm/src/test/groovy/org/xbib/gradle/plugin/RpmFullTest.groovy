package org.xbib.gradle.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.xbib.rpm.RpmReader
import org.xbib.rpm.RpmReaderResult
import org.xbib.rpm.format.Format
import org.xbib.rpm.header.IntegerList
import org.xbib.rpm.header.StringList

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.xbib.rpm.format.Flags.EQUAL
import static org.xbib.rpm.format.Flags.GREATER
import static org.xbib.rpm.format.Flags.LESS
import static org.xbib.rpm.header.HeaderTag.CONFLICTFLAGS
import static org.xbib.rpm.header.HeaderTag.CONFLICTNAME
import static org.xbib.rpm.header.HeaderTag.CONFLICTVERSION
import static org.xbib.rpm.header.HeaderTag.DISTRIBUTION
import static org.xbib.rpm.header.HeaderTag.FILEGROUPNAME
import static org.xbib.rpm.header.HeaderTag.FILEMODES
import static org.xbib.rpm.header.HeaderTag.FILEUSERNAME
import static org.xbib.rpm.header.HeaderTag.OBSOLETEFLAGS
import static org.xbib.rpm.header.HeaderTag.OBSOLETENAME
import static org.xbib.rpm.header.HeaderTag.OBSOLETEVERSION
import static org.xbib.rpm.header.HeaderTag.PREFIXES
import static org.xbib.rpm.lead.Architecture.I386
import static org.xbib.rpm.lead.Os.LINUX
import static org.xbib.rpm.lead.PackageType.BINARY
import static org.xbib.rpm.payload.CpioHeader.DIR
import static org.xbib.rpm.payload.CpioHeader.FILE

class RpmFullTest {

    File projectDir

    @BeforeEach
    void setup() {
        projectDir = new File("build/testrpm")
        projectDir.mkdirs()
    }


    @Test
    void testCopySpec() {
        File bananaFile = new File(projectDir, 'test/banana')
        bananaFile.parentFile.mkdirs()
        bananaFile.withWriter {
            out -> out.write('banana')
        }
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        String fruit = 'apple'
        File appleFile = new File(srcDir, fruit)
        appleFile.withWriter {
            out -> out.write(fruit)
        }
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'test'
            packageVersion = '1.0.0'
            packageRelease = '1'
            addParentDirs = true
            from(bananaFile.absoluteFile.parent) {
                into '/usr/share/myproduct/etc'
            }
            from(appleFile.absoluteFile.parent) {
                into '/usr/local/myproduct/bin'
            }
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        RpmReaderResult result = rpmReader.read(project.tasks.buildRpm.outputs.files.first().toPath())
        assertEquals([
                './usr/local/myproduct',
                './usr/local/myproduct/bin',
                './usr/local/myproduct/bin/apple',
                './usr/share/myproduct',
                './usr/share/myproduct/etc',
                './usr/share/myproduct/etc/banana'], result.files*.name)
        assertEquals([DIR, DIR, FILE, DIR, DIR, FILE], result.files*.type)
    }

    @Test
    void testExplicitDirectory() {
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'test'
            packageVersion = '1.0.0'
            packageRelease = '1'
            directory '/lib'
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        RpmReaderResult result = rpmReader.read(project.tasks.buildRpm.outputs.files.first().toPath())
        assertNotNull(result.files.find { it.name == './lib' })
    }

    @Test
    void testFilter() {
        File appleFile = new File(projectDir, 'src/apple')
        appleFile.mkdirs()
        appleFile.withWriter {
            out -> out.write('{{BASE}}/apple')
        }
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'test'
            packageVersion = '1.0.0'
            packageRelease = '1'
            from(appleFile.absoluteFile.parent) {
                into '/usr/local/myproduct/bin'
                filter({ line ->
                    return line.replaceAll(/\{\{BASE\}\}/, '/usr/local/myproduct')
                })
            }
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        RpmReaderResult result = rpmReader.read(project.tasks.buildRpm.outputs.files.first().toPath())
        def scannerApple = result.files.find { it.name == './usr/local/myproduct/bin/apple' }
        assertEquals(StandardCharsets.UTF_8.decode(scannerApple.contents).toString(), '/usr/local/myproduct/apple')
    }

    @Test
    void testCustomCopySpec() {
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        String fruit = 'apple'
        File appleFile = new File(srcDir, fruit)
        appleFile.withWriter {
            out -> out.write(fruit)
        }
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        def customCopySpec = project.copySpec {
            into('lib') {
                from 'src'
            }
        }
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'copyspec'
            packageVersion = '1.0.0'
            packageRelease = '1'
            with customCopySpec
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        RpmReaderResult result = rpmReader.read(project.tasks.buildRpm.outputs.files.first().toPath())
        assertThat(result.files*.name, is(['./lib/apple']))
    }

    @Test
    void testFileMode() {
        File srcDir1 = new File(projectDir, 'src1')
        srcDir1.mkdirs()
        new File(srcDir1, 'apple').withWriter { out -> out.write('apple') }
        File srcDir2 = new File(projectDir, 'src2')
        srcDir2.mkdirs()
        new File(srcDir2, 'banana').withWriter { out -> out.write('banana') }
        File srcDir3 = new File(projectDir, 'src3')
        srcDir3.mkdirs()
        new File(srcDir3, 'cherry').withWriter { out -> out.write('cherry') }
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'fileMode'
            packageVersion = '2.0'
            packageRelease = '2'
            into '/tiny'
            fileMode 0555
            from(srcDir1.absoluteFile) {
                // should be default group
            }
            from(srcDir2.absoluteFile) {
                fileMode 0666
            }
            from(srcDir3.absoluteFile) {
                fileMode 0555
            }
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        RpmReaderResult result = rpmReader.read(project.tasks.buildRpm.outputs.files.first().toPath())
        assertEquals([FILE, FILE, FILE], result.files*.type)
        assertEquals(['./tiny/apple', './tiny/banana', './tiny/cherry'], result.files*.name)
        assertThat([(short)0100555, (short)0100666, (short)0100555], is(result.format.header.getEntry(FILEMODES).values))
    }

    @Test
    void testUserGroup() {
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        new File(srcDir, 'apple').withWriter { out -> out.write('apple') }
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'userTest'
            packageVersion = '2.0'
            packageRelease = '2'
            user = 'joerg'
            group = 'users'
            from srcDir.absoluteFile
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        RpmReaderResult result = rpmReader.read(project.tasks.buildRpm.outputs.files.first().toPath())
        assertEquals([FILE], result.files*.type)
        StringList users = result.format.header.getEntry(FILEUSERNAME)?.values as StringList
        assertThat(['joerg'], is(users))
        StringList groups = result.format.header.getEntry(FILEGROUPNAME)?.values as StringList
        assertThat(['users'], is(groups))
    }

    @Test
    void testPrefix() {
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'prefixes'
            packageVersion = '1.0.0'
            packageRelease = '1'
            prefixes = ['/opt']
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        RpmReaderResult result = rpmReader.read(project.tasks.buildRpm.outputs.files.first().toPath())
        assertThat(StringList.of('/opt') as StringList, is(result.format.header.getEntry(PREFIXES).values))
    }

    @Test
    void testConflictsAndObsoletes() {
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        File srcDir = new File(projectDir, 'src')
        srcDir.mkdirs()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'testing'
            packageVersion = '1.2'
            packageRelease = '3'
            packageType = BINARY
            arch = I386
            os = LINUX
            license = 'Free'
            distribution = 'SuperSystem'
            vendor = 'Super Associates, LLC'
            url = 'http://www.example.com/'
            obsoletes('blarg', '1.0', GREATER | EQUAL)
            conflicts('blech')
            conflicts('packageA', '1.0', LESS)
            obsoletes('packageB', '2.2', GREATER)
            obsoletes('packageC')
            from(srcDir)
            into '/opt/bleah'
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        def result = rpmReader.read(Paths.get(projectDir.toString(), 'build', 'distributions', 'testing-1.2-3.i386.rpm'))
        Format format = result.format
        StringList obsoletes = format.header.getEntry(OBSOLETENAME).values as StringList
        StringList obsoleteVersions = format.header.getEntry(OBSOLETEVERSION).values as StringList
        IntegerList obsoleteComparisons = format.header.getEntry(OBSOLETEFLAGS).values as IntegerList
        StringList conflicts = format.header.getEntry(CONFLICTNAME).values as StringList
        StringList conflictVersions = format.header.getEntry(CONFLICTVERSION).values as StringList
        IntegerList conflictComparisons = format.header.getEntry(CONFLICTFLAGS).values as IntegerList
        StringList distribution = format.header.getEntry(DISTRIBUTION).values as StringList
        assertThat(StringList.of('SuperSystem') as StringList, is(distribution))
        assertThat('blarg', is(obsoletes.get(0)))
        assertThat(GREATER | EQUAL, is(obsoleteComparisons.get(0)))
        assertThat('blech', is(conflicts.get(0)))
        assertThat('', is(conflictVersions.get(0)))
        assertThat(0, is(conflictComparisons.get(0)))
        assertThat('packageA', is(conflicts.get(1)))
        assertThat('1.0', is(conflictVersions.get(1)))
        assertThat(LESS, is(conflictComparisons.get(1)))
        assertThat('packageB', is(obsoletes.get(1)))
        assertThat('2.2', is(obsoleteVersions.get(1)))
        assertThat(GREATER, is(obsoleteComparisons.get(1)))
        assertThat('packageC', is(obsoletes.get(2)))
    }

    @Test
    void testFilesFromConfiguration() {
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.repositories {
            mavenCentral()
        }
        project.configurations {
            myconf
        }
        project.dependencies {
            myconf "junit:junit:4.12"
        }
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'fromconfiguration'
            packageVersion = '1.0.0'
            packageRelease = '1'
            into('/tmp') {
                from project.configurations.myconf
            }
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        RpmReaderResult result = rpmReader.read(project.tasks.buildRpm.outputs.files.first().toPath())
        assertThat(['./tmp/hamcrest-core-1.3.jar', './tmp/junit-4.12.jar'], is(result.files*.name))
    }
}
