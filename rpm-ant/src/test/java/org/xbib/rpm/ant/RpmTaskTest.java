package org.xbib.rpm.ant;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.junit.jupiter.api.Test;
import org.xbib.rpm.RpmReader;
import org.xbib.rpm.changelog.ChangelogParser;
import org.xbib.rpm.format.Flags;
import org.xbib.rpm.format.Format;
import org.xbib.rpm.header.EntryType;
import org.xbib.rpm.header.HeaderTag;
import org.xbib.rpm.header.IntegerList;
import org.xbib.rpm.header.StringList;
import org.xbib.rpm.header.entry.SpecEntry;
import org.xbib.rpm.payload.Directive;
import org.xbib.rpm.signature.SignatureTag;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;

/**
 *
 */
public class RpmTaskTest {

    @Test
    public void testBadName() throws Exception {
        RpmTask task = new RpmTask();
        task.setDestination(getTargetDir());
        task.setVersion("1.0");
        task.setGroup("groupRequired");
        task.setName("test");
        task.execute();
        // NB: This is no longer a bad name, long names are truncated in the header
        task.setName("ToooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooLong");
        try {
            task.execute();
        } catch (BuildException e) {
            fail();
        }
        task.setName("test/invalid");
        try {
            task.execute();
            fail();
        } catch (IllegalArgumentException iae) {
            // Pass
        }
        task.setName("test invalid");
        try {
            task.execute();
            fail();
        } catch (IllegalArgumentException iae) {
            // Pass
        }
        task.setName("test\tinvalid");
        try {
            task.execute();
            fail();
        } catch (IllegalArgumentException iae) {
            // Pass
        }
    }

    @Test
    public void testBadVersion() throws Exception {
        RpmTask task = new RpmTask();
        task.setName("nameRequired");
        task.setGroup("groupRequired");
        // test version with illegal char -
        task.setVersion("1.0-beta");
        try {
            task.execute();
            fail();
        } catch (IllegalArgumentException iae) {
            // Pass
        }

        // test version with illegal char /
        task.setVersion("1.0/beta");
        try {
            task.execute();
            fail();
        } catch (IllegalArgumentException iae) {
            // Pass
        }
    }

    @Test
    public void testBadRelease() throws Exception {
        RpmTask task = new RpmTask();
        task.setName("nameRequired");
        task.setVersion("versionRequired");
        task.setGroup("groupRequired");
        task.setRelease("2-3");
        try {
            task.execute();
            fail();
        } catch (IllegalArgumentException iae) {
            // Pass
        }
        task.setRelease("2/3");
        try {
            task.execute();
            fail();
        } catch (IllegalArgumentException iae) {
            // Pass
        }
    }

    @Test
    public void testBadEpoch() throws Exception {
        RpmTask task = new RpmTask();
        task.setName("nameRequired");
        task.setVersion("versionRequired");
        task.setGroup("groupRequired");
        task.setEpoch("2-3");
        try {
            task.execute();
            fail();
        } catch (BuildException iae) {
            //
        }
        task.setEpoch("2~3");
        try {
            task.execute();
            fail();
        } catch (BuildException iae) {
            //
        }
        task.setEpoch("2/3");
        try {
            task.execute();
            fail();
        } catch (BuildException iae) {
            //
        }
        task.setEpoch("abc");
        try {
            task.execute();
            fail();
        } catch (BuildException iae) {
            //
        }
    }

    @Test
    public void testRestrict() throws Exception {
        Depends one = new Depends();
        one.setName("one");
        one.setVersion("1.0");
        Depends two = new Depends();
        two.setName("two");
        two.setVersion("1.0");
        RpmTask task = new RpmTask();
        task.addDepends(one);
        task.addDepends(two);
        assertEquals(2, task.depends.size());
        assertEquals("one", task.depends.get(0).getName());
        assertEquals("two", task.depends.get(1).getName());
        task.restrict("one");
        assertEquals(1, task.depends.size());
        assertEquals("two", task.depends.get(0).getName());
    }

    @Test
    public void testCapabilities() throws Exception {
        Path filename = getTargetDir().resolve("rpmtest-1.0-1.noarch.rpm");
        RpmTask task = createBasicTask(getTargetDir());
        for (String[] def : new String[][]{
                {"depone", "", "1.0"},
                {"deptwo", "less", "2.0"},
                {"depthree", "", ""},
        }) {
            Depends dep = new Depends();
            dep.setName(def[0]);
            if (0 < def[1].length()) {
                dep.setComparison((Depends.ComparisonEnum)
                        EnumeratedAttribute.getInstance(Depends.ComparisonEnum.class, def[1]));
            }
            if (0 < def[2].length()) {
                dep.setVersion(def[2]);
            }
            task.addDepends(dep);
        }
        for (String[] def : new String[][]{
                {"provone", "1.1"},
                {"provtwo", "2.1"},
                {"provthree", ""},
        }) {
            Provides prov = new Provides();
            prov.setName(def[0]);
            if (0 < def[1].length()) {
                prov.setVersion(def[1]);
            }
            task.addProvides(prov);
        }
        for (String[] def : new String[][]{
                {"conone", "", "1.2"},
                {"contwo", "less", "2.2"},
                {"conthree", "", ""},
        }) {
            Conflicts con = new Conflicts();
            con.setName(def[0]);
            if (0 < def[1].length()) {
                con.setComparison((Conflicts.ComparisonEnum)
                        EnumeratedAttribute.getInstance(Conflicts.ComparisonEnum.class, def[1]));
            }
            if (0 < def[2].length()) {
                con.setVersion(def[2]);
            }
            task.addConflicts(con);
        }
        for (String[] def : new String[][]{
                {"obsone", "", "1.3"},
                {"obstwo", "less", "2.3"},
                {"obsthree", "", ""},
        }) {
            Obsoletes obs = new Obsoletes();
            obs.setName(def[0]);
            if (0 < def[1].length()) {
                obs.setComparison((Obsoletes.ComparisonEnum)
                        EnumeratedAttribute.getInstance(Obsoletes.ComparisonEnum.class, def[1]));
            }
            if (0 < def[2].length()) {
                obs.setVersion(def[2]);
            }
            task.addObsoletes(obs);
        }
        task.execute();
        Format format = getFormat(filename);
        StringList require = (StringList)  format.getHeader().getEntry(HeaderTag.REQUIRENAME).getValues();
        IntegerList requireflags = (IntegerList) format.getHeader().getEntry(HeaderTag.REQUIREFLAGS).getValues();
        StringList requireversion = (StringList) format.getHeader().getEntry(HeaderTag.REQUIREVERSION).getValues();
        assertThat(StringList.of("depone", "deptwo", "depthree"), is(require.subList(require.size() - 3, require.size())));
        assertThat(IntegerList.of(Flags.EQUAL | Flags.GREATER, Flags.LESS, 0), is(requireflags.subList(requireflags.size() - 3, requireflags.size())));
        assertThat(StringList.of("1.0", "2.0", ""), is(requireversion.subList(requireversion.size() - 3, requireversion.size())));
        StringList provide = (StringList) format.getHeader().getEntry(HeaderTag.PROVIDENAME).getValues();
        IntegerList provideflags = (IntegerList) format.getHeader().getEntry(HeaderTag.PROVIDEFLAGS).getValues();
        StringList provideversion = (StringList) format.getHeader().getEntry(HeaderTag.PROVIDEVERSION).getValues();
        assertThat(StringList.of("rpmtest", "provone", "provtwo", "provthree"), is(provide));
        assertThat(IntegerList.of(Flags.EQUAL, Flags.EQUAL, Flags.EQUAL, 0), is(provideflags));
        assertThat(StringList.of("0:1.0-1", "1.1", "2.1", ""), is(provideversion));
        StringList conflict = (StringList) format.getHeader().getEntry(HeaderTag.CONFLICTNAME).getValues();
        IntegerList conflictflags = (IntegerList) format.getHeader().getEntry(HeaderTag.CONFLICTFLAGS).getValues();
        StringList conflictversion = (StringList) format.getHeader().getEntry(HeaderTag.CONFLICTVERSION).getValues();
        assertThat(StringList.of("conone", "contwo", "conthree"), is(conflict));
        assertThat(IntegerList.of(Flags.EQUAL | Flags.GREATER, Flags.LESS, 0), is(conflictflags));
        assertThat(StringList.of("1.2", "2.2", ""), is(conflictversion));
        StringList obsolete = (StringList) format.getHeader().getEntry(HeaderTag.OBSOLETENAME).getValues();
        IntegerList obsoleteflags = (IntegerList) format.getHeader().getEntry(HeaderTag.OBSOLETEFLAGS).getValues();
        StringList obsoleteversion = (StringList) format.getHeader().getEntry(HeaderTag.OBSOLETEVERSION).getValues();
        assertThat(StringList.of("obsone", "obstwo", "obsthree"), is(obsolete));
        assertThat(IntegerList.of(Flags.EQUAL | Flags.GREATER, Flags.LESS, 0), is(obsoleteflags));
        assertThat(StringList.of("1.3", "2.3", ""), is(obsoleteversion));
    }

    @Test
    public void testScripts() throws Exception {
        Path filename = getTargetDir().resolve("rpmtest-1.0-1.noarch.rpm");
        RpmTask task = createBasicTask(getTargetDir());
        task.setPreInstallScript(Paths.get("src/test/resources/prein.sh"));
        task.setPostInstallScript(Paths.get("src/test/resources/postin.sh"));
        task.setPreUninstallScript(Paths.get("src/test/resources/preun.sh"));
        task.setPostUninstallScript(Paths.get("src/test/resources/postun.sh"));
        task.execute();
        Format format = getFormat(filename);
        assertHeaderEquals("#!/bin/sh\n\necho Hello Pre Install!\n", format,
                HeaderTag.PREINSCRIPT);
        assertHeaderEquals("\n\necho Hello Post Install!\n", format,
                HeaderTag.POSTINSCRIPT);
        assertHeaderEquals("# comment\n\necho Hello Pre Uninstall!\n", format,
                HeaderTag.PREUNSCRIPT);
        assertHeaderEquals("#!/usr/bin/perl\n\nprint \"Hello Post Uninstall!\\n\";\n", format,
                HeaderTag.POSTUNSCRIPT);

        assertHeaderEquals("/bin/sh", format, HeaderTag.PREINPROG);
        assertHeaderEquals("/bin/sh", format, HeaderTag.POSTINPROG);
        assertHeaderEquals("/bin/sh", format, HeaderTag.PREUNPROG);
        assertHeaderEquals("/usr/bin/perl", format, HeaderTag.POSTUNPROG);
    }

    @Test
    public void testScriptsAndChangeLog() throws Exception {
        Path filename = getTargetDir().resolve("rpmtest-1.0-2.noarch.rpm");
        RpmTask task = createBasicTask(getTargetDir());
        task.setRelease("2");
        task.setPreInstallScript(Paths.get("src/test/resources/prein.sh"));
        task.setPostInstallScript(Paths.get("src/test/resources/postin.sh"));
        task.setPreUninstallScript(Paths.get("src/test/resources/preun.sh"));
        task.setPostUninstallScript(Paths.get("src/test/resources/postun.sh"));
        task.setChangeLog(Paths.get("src/test/resources/org/xbib/rpm/changelog/changelog"));
        task.execute();
        Format format = getFormat(filename);
        assertHeaderEquals("#!/bin/sh\n\necho Hello Pre Install!\n", format,
                HeaderTag.PREINSCRIPT);
        assertHeaderEquals("\n\necho Hello Post Install!\n", format,
                HeaderTag.POSTINSCRIPT);
        assertHeaderEquals("# comment\n\necho Hello Pre Uninstall!\n", format,
                HeaderTag.PREUNSCRIPT);
        assertHeaderEquals("#!/usr/bin/perl\n\nprint \"Hello Post Uninstall!\\n\";\n", format,
                HeaderTag.POSTUNSCRIPT);
        assertHeaderEquals("/bin/sh", format, HeaderTag.PREINPROG);
        assertHeaderEquals("/bin/sh", format, HeaderTag.POSTINPROG);
        assertHeaderEquals("/bin/sh", format, HeaderTag.PREUNPROG);
        assertHeaderEquals("/usr/bin/perl", format, HeaderTag.POSTUNPROG);
        assertDateEntryHeaderEqualsAt("Tue Feb 24 2015", format,
                HeaderTag.CHANGELOGTIME, 10, 0);
        assertHeaderEqualsAt("Thomas Jefferson", format,
                HeaderTag.CHANGELOGNAME, 10, 4);
        assertHeaderEqualsAt("- Initial rpm for this package", format,
                HeaderTag.CHANGELOGTEXT, 10, 9);
        String expectedMultiLineDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod \n" +
                "tempor incididunt ut labore et dolore magna aliqua";
        assertHeaderEqualsAt(expectedMultiLineDescription, format,
                HeaderTag.CHANGELOGTEXT, 10, 0);
    }

    @Test
    public void testFiles() throws Exception {
        Path filename = getTargetDir().resolve("rpmtest-1.0-1.noarch.rpm");
        RpmTask task = createBasicTask(getTargetDir());
        RpmFileSet fs = new RpmFileSet();
        fs.setPrefix("/etc");
        fs.setFile(Paths.get("src/test/resources/prein.sh").toFile());
        fs.setConfig(true);
        fs.setNoReplace(true);
        fs.setDoc(true);
        fs.setUserName("jabberwocky");
        fs.setGroup("vorpal");
        task.addRpmfileset(fs);
        task.execute();
        Format format = getFormat(filename);
        assertThat(StringList.of("jabberwocky"), is(format.getHeader().getEntry(HeaderTag.FILEUSERNAME).getValues()));
        assertThat(StringList.of("vorpal"), is(format.getHeader().getEntry(HeaderTag.FILEGROUPNAME).getValues()));
        EnumSet<Directive> directives = EnumSet.of(Directive.CONFIG, Directive.DOC, Directive.NOREPLACE);
        int expectedFlags = 0;
        for (Directive d : directives) {
            expectedFlags |= d.flag();
        }
        assertInt32EntryHeaderEquals(IntegerList.of(expectedFlags), format, HeaderTag.FILEFLAGS);
    }

    @Test
    public void testSigning() throws Exception {
        Path filename = getTargetDir().resolve("rpmtest-1.0-1.noarch.rpm");
        RpmTask task = createBasicTask(getTargetDir());
        task.setPrivateKeyRing(getClass().getResourceAsStream("/pgp/test-secring.gpg"));
        task.setPrivateKeyPassphrase("test");
        task.execute();
        Format format = getFormat(filename);
        assertNotNull(format.getSignatureHeader().getEntry(SignatureTag.RSAHEADER));
        assertNotNull(format.getSignatureHeader().getEntry(SignatureTag.LEGACY_PGP));
    }

    @Test
    public void testPackageNameLength() {
        Path dir = getTargetDir();
        RpmTask task = new RpmTask();
        task.setProject(createProject());
        task.setDestination(dir);
        task.setName("thisfilenameislongdddddddddddddddddfddddddddddddddddddddddddddddddd");
        task.setVersion("1.0");
        task.setRelease("1");
        task.setGroup("Application/Office");
        task.setPreInstallScript(Paths.get("src/test/resources/prein.sh"));
        task.setPostInstallScript(Paths.get("src/test/resources/postin.sh"));
        task.setPreUninstallScript(Paths.get("src/test/resources/preun.sh"));
        task.setPostUninstallScript(Paths.get("src/test/resources/postun.sh"));
        RpmFileSet fs = new RpmFileSet();
        fs.setPrefix("/etc");
        fs.setFile(Paths.get("src/test/resources/prein.sh").toFile());
        fs.setConfig(true);
        fs.setNoReplace(true);
        fs.setDoc(true);
        task.addRpmfileset(fs);
        try {
            task.execute();
        } catch (Exception e) {
            fail("Test failed: should not be thrown: " + e.getClass().getName());
        }
        task.setName("shortpackagename");
        try {
            task.execute();
        } catch (Exception e) {
            fail("Test failed: should not be thrown: " + e.getClass().getName());
        }
    }

    private Format getFormat(Path filename) throws IOException {
        RpmReader rpmReader = new RpmReader();
        return rpmReader.readHeader(filename);
    }

    private RpmTask createBasicTask(Path dir) {
        RpmTask task = new RpmTask();
        task.setProject(createProject());
        task.setDestination(dir);
        task.setName("rpmtest");
        task.setVersion("1.0");
        task.setRelease("1");
        task.setGroup("Application/Office");
        return task;
    }

    private Project createProject() {
        Project project = new Project();
        project.setCoreLoader(getClass().getClassLoader());
        project.init();
        return project;
    }

    private Path getTargetDir() {
        return Paths.get("build");
    }

    private void assertHeaderEquals(String expected, Format format, EntryType entryType) {
        assertNotNull(format, "null format");
        SpecEntry<?> entry = format.getHeader().getEntry(entryType);
        assertNotNull(entry, "Entry not found : " + entryType.getName());
        assertEquals(6, entry.getType(), "Entry type : " + entryType.getName());
        StringList values = (StringList) entry.getValues();
        assertNotNull(values, "null values");
        assertEquals(1, values.size(), "Entry size : " + entryType.getName());
        assertEquals(expected, values.get(0), "Entry value : " + entryType.getName());
    }

    private void assertHeaderEqualsAt(String expected, Format format, EntryType entryType, int size, int pos) {
        assertNotNull(format, "null format");
        SpecEntry<?> entry = format.getHeader().getEntry(entryType);
        assertNotNull(entry, "Entry not found : " + entryType.getName());
        assertEquals(8, entry.getType(), "Entry type : " + entryType.getName());
        StringList values = (StringList) entry.getValues();
        assertNotNull(values, "null values");
        assertEquals(size, values.size(), "Entry size : " + entryType.getName());
        assertEquals(expected, values.get(pos), "Entry value : " + entryType.getName());
    }

    private void assertInt32EntryHeaderEquals(IntegerList expected, Format format, EntryType entryType) {
        assertNotNull(format, "null format");
        SpecEntry<?> entry = format.getHeader().getEntry(entryType);
        assertNotNull(entry, "Entry not found : " + entryType.getName());
        assertEquals(4, entry.getType(), "Entry type : " + entryType.getName());
        IntegerList values = (IntegerList) entry.getValues();
        assertNotNull(values, "null values");
        assertEquals(1, values.size(), "Entry size : " + entryType.getName());
        assertThat("Entry value : " + entryType.getName(), expected, is(values));
    }

    private void assertDateEntryHeaderEqualsAt(String expected, Format format, EntryType entryType, int size, int pos) {
        assertNotNull(format, "null format");
        SpecEntry<?> entry = format.getHeader().getEntry(entryType);
        assertNotNull(entry, "Entry not found : " + entryType.getName());
        assertEquals(4, entry.getType(), "Entry type : " + entryType.getName());
        IntegerList values = (IntegerList) entry.getValues();
        assertNotNull(values, "null values");
        assertEquals(size, values.size(), "Entry size : " + entryType.getName());
        LocalDateTime localDate = LocalDateTime.ofEpochSecond(values.get(pos), 0, ZoneOffset.UTC);
        assertEquals(expected, ChangelogParser.CHANGELOG_FORMAT.format(localDate), "Entry value : " + entryType.getName());
    }
}
