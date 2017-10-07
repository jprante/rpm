package org.xbib.rpm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.xbib.rpm.format.Flags;
import org.xbib.rpm.format.Format;
import org.xbib.rpm.header.HeaderTag;
import org.xbib.rpm.lead.Architecture;
import org.xbib.rpm.lead.Os;
import org.xbib.rpm.lead.PackageType;
import org.xbib.rpm.payload.CompressionType;
import org.xbib.rpm.payload.Directive;
import org.xbib.rpm.security.HashAlgo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;

/**
 *
 */
public class RpmBuilderTest {

    @Test
    public void testLongNameTruncation() throws Exception {
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.setPackage("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
                + "xxxxa", "1.0", "1");
        rpmBuilder.setBuildHost("localhost");
        rpmBuilder.setLicense("GPL");
        rpmBuilder.setPlatform(Architecture.NOARCH, Os.LINUX);
        rpmBuilder.setType(PackageType.BINARY);
        rpmBuilder.build(getTargetDir());
        Path path = getTargetDir().resolve("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
                + "xxxxxxxxxxxa-1.0-1.noarch.rpm");
        Format format = new RpmReader().readHeader(path);
        assertEquals("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                format.getLead().getName());
    }

    @Test
    public void testFiles() throws Exception {
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.setPackage("filestest", "1.0", "1");
        rpmBuilder.setBuildHost("localhost");
        rpmBuilder.setLicense("GPL");
        rpmBuilder.setPlatform(Architecture.NOARCH, Os.LINUX);
        rpmBuilder.setType(PackageType.BINARY);
        EnumSet<Directive> directives = EnumSet.of(Directive.CONFIG, Directive.DOC, Directive.NOREPLACE);
        rpmBuilder.addFile("/etc", Paths.get("src/test/resources/prein.sh"), 493, 493,
                directives, "jabberwocky", "vorpal");
        rpmBuilder.build(getTargetDir());
        Path path = getTargetDir().resolve("filestest-1.0-1.noarch.rpm");
        Format format = new RpmReader().readHeader(path);
        assertArrayEquals(new String[]{"jabberwocky"},
                (String[]) format.getHeader().getEntry(HeaderTag.FILEUSERNAME).getValues());
        assertArrayEquals(new String[]{"vorpal"},
                (String[]) format.getHeader().getEntry(HeaderTag.FILEGROUPNAME).getValues());
        Integer expectedFlags = 0;
        for (Directive d : directives) {
            expectedFlags |= d.flag();
        }
        assertArrayEquals(new Integer[]{expectedFlags},
                (Integer[]) format.getHeader().getEntry(HeaderTag.FILEFLAGS).getValues());
    }

    @Test
    public void testBuild() throws Exception {
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.setPackage("test", "1.0", "1");
        rpmBuilder.setBuildHost("localhost");
        rpmBuilder.setLicense("GPL");
        rpmBuilder.setPlatform(Architecture.NOARCH, Os.LINUX);
        rpmBuilder.setType(PackageType.BINARY);
        rpmBuilder.build(getTargetDir());
    }

    @Test
    public void testBuildWithEpoch() throws Exception {
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.setPackage("testEpoch", "1.0", "1", 1);
        rpmBuilder.setBuildHost("localhost");
        rpmBuilder.setLicense("GPL");
        rpmBuilder.setPlatform(Architecture.NOARCH, Os.LINUX);
        rpmBuilder.setType(PackageType.BINARY);
        rpmBuilder.build(getTargetDir());
    }

    @Test
    public void testBuildMetapackage() throws Exception {
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.setPackage("testMetapkg", "1.0", "1", 1);
        rpmBuilder.setBuildHost("localhost");
        rpmBuilder.setLicense("GPL");
        rpmBuilder.setPlatform(Architecture.NOARCH, Os.LINUX);
        rpmBuilder.setType(PackageType.BINARY);
        rpmBuilder.addDependencyMore("glibc", "2.17");
        rpmBuilder.build(getTargetDir());
    }

    @Test
    public void testCapabilities() throws Exception {
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.setPackage("testCapabilities", "1.0", "1");
        rpmBuilder.setBuildHost("localhost");
        rpmBuilder.setLicense("GPL");
        rpmBuilder.setPlatform(Architecture.NOARCH, Os.LINUX);
        rpmBuilder.setType(PackageType.BINARY);
        rpmBuilder.addDependency("httpd", 0, "");
        rpmBuilder.addProvides("frobnicator", "");
        rpmBuilder.addProvides("barnacle", "3.89");
        rpmBuilder.addConflicts("fooberry", Flags.GREATER | Flags.EQUAL, "1a");
        rpmBuilder.addObsoletes("testCappypkg", 0, "");
        rpmBuilder.build(getTargetDir());
        Path path = getTargetDir().resolve("testCapabilities-1.0-1.noarch.rpm");
        Format format = new RpmReader().readHeader(path);
        String[] require = (String[]) format.getHeader().getEntry(HeaderTag.REQUIRENAME).getValues();
        Integer[] requireflags = (Integer[]) format.getHeader().getEntry(HeaderTag.REQUIREFLAGS).getValues();
        String[] requireversion = (String[]) format.getHeader().getEntry(HeaderTag.REQUIREVERSION).getValues();
        assertArrayEquals(new String[]{"httpd"}, Arrays.copyOfRange(require, require.length - 1, require.length));
        assertArrayEquals(new Integer[]{0}, Arrays.copyOfRange(requireflags, requireflags.length - 1, require.length));
        assertArrayEquals(new String[]{""}, Arrays.copyOfRange(requireversion, requireversion.length - 1, require.length));
        String[] provide = (String[]) format.getHeader().getEntry(HeaderTag.PROVIDENAME).getValues();
        Integer[] provideflags = (Integer[]) format.getHeader().getEntry(HeaderTag.PROVIDEFLAGS).getValues();
        String[] provideversion = (String[]) format.getHeader().getEntry(HeaderTag.PROVIDEVERSION).getValues();
        assertArrayEquals(new String[]{"testCapabilities", "frobnicator", "barnacle"}, provide);
        assertArrayEquals(new Integer[]{Flags.EQUAL, 0, Flags.EQUAL}, provideflags);
        assertArrayEquals(new String[]{"0:1.0-1", "", "3.89"}, provideversion);
        String[] conflict = (String[]) format.getHeader().getEntry(HeaderTag.CONFLICTNAME).getValues();
        Integer[] conflictflags = (Integer[]) format.getHeader().getEntry(HeaderTag.CONFLICTFLAGS).getValues();
        String[] conflictversion = (String[]) format.getHeader().getEntry(HeaderTag.CONFLICTVERSION).getValues();
        assertArrayEquals(new String[]{"fooberry"}, conflict);
        assertArrayEquals(new Integer[]{Flags.GREATER | Flags.EQUAL}, conflictflags);
        assertArrayEquals(new String[]{"1a"}, conflictversion);
        String[] obsolete = (String[]) format.getHeader().getEntry(HeaderTag.OBSOLETENAME).getValues();
        Integer[] obsoleteflags = (Integer[]) format.getHeader().getEntry(HeaderTag.OBSOLETEFLAGS).getValues();
        String[] obsoleteversion = (String[]) format.getHeader().getEntry(HeaderTag.OBSOLETEVERSION).getValues();
        assertArrayEquals(new String[]{"testCappypkg"}, obsolete);
        assertArrayEquals(new Integer[]{0}, obsoleteflags);
        assertArrayEquals(new String[]{""}, obsoleteversion);
    }

    @Test
    public void testMultipleCapabilities() throws Exception {
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.setPackage("testMultipleCapabilities", "1.0", "1");
        rpmBuilder.setBuildHost("localhost");
        rpmBuilder.setLicense("GPL");
        rpmBuilder.setPlatform(Architecture.NOARCH, Os.LINUX);
        rpmBuilder.addDependency("httpd", Flags.GREATER | Flags.EQUAL, "1.0");
        rpmBuilder.addDependency("httpd", Flags.LESS, "2.0");
        rpmBuilder.build(getTargetDir());
        Path path = getTargetDir().resolve("testMultipleCapabilities-1.0-1.noarch.rpm");
        Format format = new RpmReader().readHeader(path);
        String[] require = (String[]) format.getHeader().getEntry(HeaderTag.REQUIRENAME).getValues();
        Integer[] requireflags = (Integer[]) format.getHeader().getEntry(HeaderTag.REQUIREFLAGS).getValues();
        String[] requireversion = (String[]) format.getHeader().getEntry(HeaderTag.REQUIREVERSION).getValues();
        assertArrayEquals(new String[]{"httpd"},
                Arrays.copyOfRange(require, require.length - 2, require.length - 1));
        assertArrayEquals(new Integer[]{Flags.GREATER | Flags.EQUAL},
                Arrays.copyOfRange(requireflags, requireflags.length - 2, require.length - 1));
        assertArrayEquals(new String[]{"1.0"},
                Arrays.copyOfRange(requireversion, requireversion.length - 2, require.length - 1));
        assertArrayEquals(new String[]{"httpd"},
                Arrays.copyOfRange(require, require.length - 1, require.length));
        assertArrayEquals(new Integer[]{Flags.LESS},
                Arrays.copyOfRange(requireflags, requireflags.length - 1, require.length));
        assertArrayEquals(new String[]{"2.0"},
                Arrays.copyOfRange(requireversion, requireversion.length - 1, require.length));
    }

    @Test
    public void testProvideOverride() throws Exception {
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.setPackage("testProvideOverride", "1.0", "1");
        rpmBuilder.setBuildHost("localhost");
        rpmBuilder.setLicense("GPL");
        rpmBuilder.setPlatform(Architecture.NOARCH, Os.LINUX);
        rpmBuilder.setType(PackageType.BINARY);
        rpmBuilder.addProvides("testProvideOverride", "1.0");
        rpmBuilder.build(getTargetDir());
        Path path = getTargetDir().resolve("testProvideOverride-1.0-1.noarch.rpm");
        Format format = new RpmReader().readHeader(path);
        String[] provide = (String[]) format.getHeader().getEntry(HeaderTag.PROVIDENAME).getValues();
        Integer[] provideflags = (Integer[]) format.getHeader().getEntry(HeaderTag.PROVIDEFLAGS).getValues();
        String[] provideversion = (String[]) format.getHeader().getEntry(HeaderTag.PROVIDEVERSION).getValues();
        assertEquals(1, provide.length);
        assertArrayEquals(new String[]{"testProvideOverride"}, Arrays.copyOfRange(provide, 0, provide.length));
        assertArrayEquals(new Integer[]{Flags.EQUAL}, Arrays.copyOfRange(provideflags, 0, provide.length));
        assertArrayEquals(new String[]{"1.0"}, Arrays.copyOfRange(provideversion, 0, provide.length));
    }

    @Test
    @Ignore
    public void testAddHeaderEntry() {
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.addHeaderEntry(HeaderTag.CHANGELOGTIME, new Integer(1));
        try {
            rpmBuilder.addHeaderEntry(HeaderTag.CHANGELOGNAME, 1L);
        } catch (ClassCastException e) {
            //
        }
        try {
            rpmBuilder.addHeaderEntry(HeaderTag.CHANGELOGTIME, "Mon Jan 01 2016");
            fail("ClassCastException expected on setting header String value where int expected.");
        } catch (ClassCastException e) {
            //
        }
        try {
            rpmBuilder.addHeaderEntry(HeaderTag.CHANGELOGTIME, 1L);
            fail("ClassCastException expected on setting header long value where int expected.");
        } catch (ClassCastException e) {
            //
        }
        try {
            short s = (short) 1;
            rpmBuilder.addHeaderEntry(HeaderTag.CHANGELOGTIME, s);
            fail("ClassCastException expected on setting header short value where int expected.");
        } catch (ClassCastException e) {
            //
        }
        try {
            Character c = 'c';
            rpmBuilder.addHeaderEntry(HeaderTag.CHANGELOGTIME, c);
            fail("ClassCastException expected on setting header char value where int expected.");
        } catch (ClassCastException e) {
            //
        }
    }

    @Test
    public void testBzip2Build() throws Exception {
        RpmBuilder rpmBuilder = new RpmBuilder(HashAlgo.SHA256, CompressionType.GZIP);
        rpmBuilder.setPackage("test-compressed", "1.0", "1");
        rpmBuilder.setBuildHost("localhost");
        rpmBuilder.setLicense("GPL");
        rpmBuilder.setPlatform(Architecture.NOARCH, Os.LINUX);
        rpmBuilder.setType(PackageType.BINARY);
        EnumSet<Directive> directives = EnumSet.of(Directive.CONFIG, Directive.DOC, Directive.NOREPLACE);
        rpmBuilder.addFile("/etc", Paths.get("src/test/resources/prein.sh"), 493, 493,
                directives, "jabberwocky", "vorpal");
        rpmBuilder.build(getTargetDir());
        Path path = getTargetDir().resolve("test-compressed-1.0-1.noarch.rpm");
        Format format = new RpmReader().readHeader(path);
    }

    private Path getTargetDir() {
        return Paths.get("build");
    }
}
