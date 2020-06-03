package org.xbib.gradle.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.xbib.rpm.RpmReader
import org.xbib.rpm.changelog.ChangelogParser
import org.xbib.rpm.format.Format
import org.xbib.rpm.header.EntryType
import org.xbib.rpm.header.IntegerList
import org.xbib.rpm.header.StringList
import org.xbib.rpm.header.entry.SpecEntry
import java.time.LocalDateTime
import java.time.ZoneOffset
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.CoreMatchers.*
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.xbib.rpm.header.HeaderTag.POSTINSCRIPT
import static org.xbib.rpm.header.HeaderTag.POSTTRANSSCRIPT
import static org.xbib.rpm.header.HeaderTag.POSTUNSCRIPT
import static org.xbib.rpm.header.HeaderTag.PREINSCRIPT
import static org.xbib.rpm.header.HeaderTag.PRETRANSSCRIPT
import static org.xbib.rpm.header.HeaderTag.PREUNSCRIPT
import static org.xbib.rpm.header.HeaderTag.CHANGELOGNAME
import static org.xbib.rpm.header.HeaderTag.CHANGELOGTEXT
import static org.xbib.rpm.header.HeaderTag.CHANGELOGTIME

class RpmScriptTest {

    File projectDir

    @BeforeEach
    void setup() {
        projectDir = new File("build/testrpm")
        projectDir.mkdirs()
    }

    @Test
    void testInstallScripts() {
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'installscripts'
            packageVersion = '1.0'
            packageRelease = '1'
            preInstall = "echo 'Hello Pre World'"
            postInstall = "echo 'Hello Post World'"
            preUninstall = "echo 'Hello PreUn World'"
            postUninstall = "echo 'Hello PostUn World'"
            preTrans = "echo 'Hello PreTrans World'"
            postTrans = "echo 'Hello PostTrans World'"
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        def result = rpmReader.read(project.tasks.buildRpm.outputs.files.first().toPath())
        Format format = result.format
        assertThat(["echo 'Hello Pre World'"], is(format.header.getEntry(PREINSCRIPT)?.values))
        assertThat(["echo 'Hello Post World'"], is(format.header.getEntry(POSTINSCRIPT)?.values))
        assertThat(["echo 'Hello PreUn World'"], is(format.header.getEntry(PREUNSCRIPT)?.values))
        assertThat(["echo 'Hello PostUn World'"], is(format.header.getEntry(POSTUNSCRIPT)?.values))
        assertThat(["echo 'Hello PreTrans World'"], is(format.header.getEntry(PRETRANSSCRIPT)?.values))
        assertThat(["echo 'Hello PostTrans World'"], is(format.header.getEntry(POSTTRANSSCRIPT)?.values))
    }


    @Test
    void testInstallScriptFiles() {
        String preinstall = getClass().getResource('preinstall.sh').text
        File scriptDir = new File(projectDir, 'scripts')
        scriptDir.mkdirs()
        new File(scriptDir, 'preinstall.sh').withWriter { out -> out.write(preinstall) }
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'installscripts'
            packageVersion = '1.0'
            packageRelease = '1'
            preInstallFile = project.file('scripts/preinstall.sh')
            postInstall = "echo 'Hello Post World'"
            preUninstall = "echo 'Hello PreUn World'"
            postUninstall = "echo 'Hello PostUn World'"
            preTrans = "echo 'Hello PreTrans World'"
            postTrans = "echo 'Hello PostTrans World'"
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        def result = rpmReader.read(project.tasks.buildRpm.outputs.files.first().toPath())
        Format format = result.format
        assertThat(["#!/usr/bin/env bash\necho 'Hello from file'\n"], is(format.header.getEntry(PREINSCRIPT)?.values))
        assertThat(["echo 'Hello Post World'"], is(format.header.getEntry(POSTINSCRIPT)?.values))
        assertThat(["echo 'Hello PreUn World'"], is(format.header.getEntry(PREUNSCRIPT)?.values))
        assertThat(["echo 'Hello PostUn World'"], is(format.header.getEntry(POSTUNSCRIPT)?.values))
        assertThat(["echo 'Hello PreTrans World'"], is(format.header.getEntry(PRETRANSSCRIPT)?.values))
        assertThat(["echo 'Hello PostTrans World'"], is(format.header.getEntry(POSTTRANSSCRIPT)?.values))
    }

    @Test
    void testChangeLog() {
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        String changelog = getClass().getResource('changelog').text
        File changelogDir = new File(projectDir, 'changelog')
        changelogDir.mkdirs()
        new File(changelogDir, 'changelog').withWriter { out -> out.write(changelog) }
        project.apply plugin: 'org.xbib.gradle.plugin.rpm'
        project.task([type: Rpm], 'buildRpm', {
            packageName = 'changelog'
            packageVersion = '1.0'
            packageRelease = '1'
            changeLogFile = project.file('changelog/changelog')
        })
        project.tasks.buildRpm.copy()
        RpmReader rpmReader = new RpmReader()
        def result = rpmReader.read(project.tasks.buildRpm.outputs.files.first().toPath())
        Format format = result.format
        assertDateEntryHeaderEqualsAt("Tue Feb 24 2015", format,
                CHANGELOGTIME, 10, 0)
        assertHeaderEqualsAt("Thomas Jefferson", format,
                CHANGELOGNAME, 10, 4)
        assertHeaderEqualsAt("- Initial rpm for this package", format,
                CHANGELOGTEXT, 10, 9)
        assertHeaderEqualsAt("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod \n" +
                "tempor incididunt ut labore et dolore magna aliqua", format,
                CHANGELOGTEXT, 10, 0)
    }

    private static void assertDateEntryHeaderEqualsAt(String expected, Format format, EntryType entryType, int size, int pos) {
        assertNotNull(format, "null format")
        SpecEntry<?> entry = format.getHeader().getEntry(entryType)
        assertNotNull(entry, "Entry not found : " + entryType.getName())
        assertEquals(4, entry.getType(), "Entry type : " + entryType.getName())
        IntegerList values = (IntegerList) entry.getValues()
        assertNotNull(values, "null values")
        assertEquals(size, values.size(), "Entry size : " + entryType.getName())
        LocalDateTime localDate = LocalDateTime.ofEpochSecond(values.get(pos), 0, ZoneOffset.UTC)
        assertEquals(expected, ChangelogParser.CHANGELOG_FORMAT.format(localDate), "Entry value : " + entryType.getName())
    }

    private static void assertHeaderEqualsAt(String expected, Format format, EntryType entryType, int size, int pos) {
        assertNotNull(format, "null format")
        SpecEntry<?> entry = format.getHeader().getEntry(entryType)
        assertNotNull(entry, "Entry not found : " + entryType.getName())
        assertEquals(8, entry.getType(), "Entry type : " + entryType.getName())
        StringList values = (StringList) entry.getValues()
        assertNotNull(values, "null values")
        assertEquals(size, values.size(), "Entry size : " + entryType.getName())
        assertEquals(expected, values.get(pos), "Entry value : " + entryType.getName())
    }
}
