package org.xbib.rpm.ant;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.junit.Test;
import org.xbib.rpm.RpmReader;
import org.xbib.rpm.changelog.ChangelogParser;
import org.xbib.rpm.format.Flags;
import org.xbib.rpm.format.Format;
import org.xbib.rpm.header.EntryType;
import org.xbib.rpm.header.HeaderTag;
import org.xbib.rpm.header.entry.SpecEntry;
import org.xbib.rpm.payload.Directive;
import org.xbib.rpm.signature.SignatureTag;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
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
        String[] require = (String[]) format.getHeader().getEntry(HeaderTag.REQUIRENAME).getValues();
        Integer[] requireflags = (Integer[]) format.getHeader().getEntry(HeaderTag.REQUIREFLAGS).getValues();
        String[] requireversion = (String[]) format.getHeader().getEntry(HeaderTag.REQUIREVERSION).getValues();
        assertArrayEquals(new String[]{"depone", "deptwo", "depthree"},
                Arrays.copyOfRange(require, require.length - 3, require.length));
        assertArrayEquals(new Integer[]{Flags.EQUAL | Flags.GREATER, Flags.LESS, 0},
                Arrays.copyOfRange(requireflags, requireflags.length - 3, require.length));
        assertArrayEquals(new String[]{"1.0", "2.0", ""},
                Arrays.copyOfRange(requireversion, requireversion.length - 3, require.length));
        String[] provide = (String[]) format.getHeader().getEntry(HeaderTag.PROVIDENAME).getValues();
        Integer[] provideflags = (Integer[]) format.getHeader().getEntry(HeaderTag.PROVIDEFLAGS).getValues();
        String[] provideversion = (String[]) format.getHeader().getEntry(HeaderTag.PROVIDEVERSION).getValues();
        assertArrayEquals(new String[]{"rpmtest", "provone", "provtwo", "provthree"}, provide);
        assertArrayEquals(new Integer[]{Flags.EQUAL, Flags.EQUAL, Flags.EQUAL, 0}, provideflags);
        assertArrayEquals(new String[]{"0:1.0-1", "1.1", "2.1", ""}, provideversion);
        String[] conflict = (String[]) format.getHeader().getEntry(HeaderTag.CONFLICTNAME).getValues();
        Integer[] conflictflags = (Integer[]) format.getHeader().getEntry(HeaderTag.CONFLICTFLAGS).getValues();
        String[] conflictversion = (String[]) format.getHeader().getEntry(HeaderTag.CONFLICTVERSION).getValues();
        assertArrayEquals(new String[]{"conone", "contwo", "conthree"}, conflict);
        assertArrayEquals(new Integer[]{Flags.EQUAL | Flags.GREATER, Flags.LESS, 0}, conflictflags);
        assertArrayEquals(new String[]{"1.2", "2.2", ""}, conflictversion);
        String[] obsolete = (String[]) format.getHeader().getEntry(HeaderTag.OBSOLETENAME).getValues();
        Integer[] obsoleteflags = (Integer[]) format.getHeader().getEntry(HeaderTag.OBSOLETEFLAGS).getValues();
        String[] obsoleteversion = (String[]) format.getHeader().getEntry(HeaderTag.OBSOLETEVERSION).getValues();
        assertArrayEquals(new String[]{"obsone", "obstwo", "obsthree"}, obsolete);
        assertArrayEquals(new Integer[]{Flags.EQUAL | Flags.GREATER, Flags.LESS, 0}, obsoleteflags);
        assertArrayEquals(new String[]{"1.3", "2.3", ""}, obsoleteversion);
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
        assertArrayEquals(new String[]{"jabberwocky"},
                (String[]) format.getHeader().getEntry(HeaderTag.FILEUSERNAME).getValues());
        assertArrayEquals(new String[]{"vorpal"},
                (String[]) format.getHeader().getEntry(HeaderTag.FILEGROUPNAME).getValues());
        EnumSet<Directive> directives = EnumSet.of(Directive.CONFIG, Directive.DOC, Directive.NOREPLACE);
        Integer expectedFlags = 0;
        for (Directive d : directives) {
            expectedFlags |= d.flag();
        }
        assertInt32EntryHeaderEquals(new Integer[]{expectedFlags}, format, HeaderTag.FILEFLAGS);
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
        assertNotNull("null format", format);
        SpecEntry<?> entry = format.getHeader().getEntry(entryType);
        assertNotNull("Entry not found : " + entryType.getName(), entry);
        assertEquals("Entry type : " + entryType.getName(), 6, entry.getType());
        String[] values = (String[]) entry.getValues();
        assertNotNull("null values", values);
        assertEquals("Entry size : " + entryType.getName(), 1, values.length);
        assertEquals("Entry value : " + entryType.getName(), expected, values[0]);
    }

    private void assertHeaderEqualsAt(String expected, Format format, EntryType entryType, int size, int pos) {
        assertNotNull("null format", format);
        SpecEntry<?> entry = format.getHeader().getEntry(entryType);
        assertNotNull("Entry not found : " + entryType.getName(), entry);
        assertEquals("Entry type : " + entryType.getName(), 8, entry.getType());
        String[] values = (String[]) entry.getValues();
        assertNotNull("null values", values);
        assertEquals("Entry size : " + entryType.getName(), size, values.length);
        assertEquals("Entry value : " + entryType.getName(), expected, values[pos]);
    }

    private void assertInt32EntryHeaderEquals(Integer[] expected, Format format, EntryType entryType) {
        assertNotNull("null format", format);
        SpecEntry<?> entry = format.getHeader().getEntry(entryType);
        assertNotNull("Entry not found : " + entryType.getName(), entry);
        assertEquals("Entry type : " + entryType.getName(), 4, entry.getType());
        Integer[] values = (Integer[]) entry.getValues();
        assertNotNull("null values", values);
        assertEquals("Entry size : " + entryType.getName(), 1, values.length);
        assertArrayEquals("Entry value : " + entryType.getName(), expected, values);
    }

    private void assertDateEntryHeaderEqualsAt(String expected, Format format, EntryType entryType, int size, int pos) {
        assertNotNull("null format", format);
        SpecEntry<?> entry = format.getHeader().getEntry(entryType);
        assertNotNull("Entry not found : " + entryType.getName(), entry);
        assertEquals("Entry type : " + entryType.getName(), 4, entry.getType());
        Integer[] values = (Integer[]) entry.getValues();
        assertNotNull("null values", values);
        assertEquals("Entry size : " + entryType.getName(), size, values.length);
        LocalDateTime localDate = LocalDateTime.ofEpochSecond(values[pos], 0, ZoneOffset.UTC);
        assertEquals("Entry value : " + entryType.getName(), expected, ChangelogParser.CHANGELOG_FORMAT.format(localDate));
    }
}
