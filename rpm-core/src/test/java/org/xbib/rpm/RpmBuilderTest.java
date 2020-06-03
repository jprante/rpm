package org.xbib.rpm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.*;
import org.xbib.rpm.format.Flags;
import org.xbib.rpm.format.Format;
import org.xbib.rpm.header.HeaderTag;
import org.xbib.rpm.header.IntegerList;
import org.xbib.rpm.header.StringList;
import org.xbib.rpm.lead.Architecture;
import org.xbib.rpm.lead.Os;
import org.xbib.rpm.lead.PackageType;
import org.xbib.rpm.payload.CompressionType;
import org.xbib.rpm.payload.Directive;
import org.xbib.rpm.security.HashAlgo;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;

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
                directives, "jabberwocky", "vorpal", true);
        rpmBuilder.build(getTargetDir());
        Path path = getTargetDir().resolve("filestest-1.0-1.noarch.rpm");
        Format format = new RpmReader().readHeader(path);
        assertThat(List.of("jabberwocky"), is(format.getHeader().getEntry(HeaderTag.FILEUSERNAME).getValues()));
        assertThat(List.of("vorpal"), is(format.getHeader().getEntry(HeaderTag.FILEGROUPNAME).getValues()));
        int expectedFlags = 0;
        for (Directive d : directives) {
            expectedFlags |= d.flag();
        }
        assertThat(List.of(expectedFlags), is(format.getHeader().getEntry(HeaderTag.FILEFLAGS).getValues()));
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
        StringList require = (StringList) format.getHeader().getEntry(HeaderTag.REQUIRENAME).getValues();
        IntegerList requireflags = (IntegerList) format.getHeader().getEntry(HeaderTag.REQUIREFLAGS).getValues();
        StringList requireversion = (StringList) format.getHeader().getEntry(HeaderTag.REQUIREVERSION).getValues();
        assertThat(List.of("httpd"), is(require.subList(require.size() - 1, require.size())));
        assertThat(IntegerList.of(0), is(requireflags.subList(requireflags.size() - 1, requireflags.size())));
        assertThat(StringList.of(""), is(requireversion.subList(requireversion.size() - 1, requireversion.size())));
        StringList provide = (StringList) format.getHeader().getEntry(HeaderTag.PROVIDENAME).getValues();
        IntegerList provideflags = (IntegerList) format.getHeader().getEntry(HeaderTag.PROVIDEFLAGS).getValues();
        StringList provideversion = (StringList) format.getHeader().getEntry(HeaderTag.PROVIDEVERSION).getValues();
        assertThat(StringList.of("testCapabilities", "frobnicator", "barnacle"), is(provide));
        assertThat(IntegerList.of(Flags.EQUAL, 0, Flags.EQUAL), is(provideflags));
        assertThat(StringList.of("0:1.0-1", "", "3.89"), is(provideversion));
        StringList conflict = (StringList) format.getHeader().getEntry(HeaderTag.CONFLICTNAME).getValues();
        IntegerList conflictflags = (IntegerList) format.getHeader().getEntry(HeaderTag.CONFLICTFLAGS).getValues();
        StringList conflictversion = (StringList) format.getHeader().getEntry(HeaderTag.CONFLICTVERSION).getValues();
        assertThat(StringList.of("fooberry"), is(conflict));
        assertThat(IntegerList.of(Flags.GREATER | Flags.EQUAL), is(conflictflags));
        assertThat(StringList.of("1a"), is(conflictversion));
        StringList obsolete = (StringList) format.getHeader().getEntry(HeaderTag.OBSOLETENAME).getValues();
        IntegerList obsoleteflags = (IntegerList) format.getHeader().getEntry(HeaderTag.OBSOLETEFLAGS).getValues();
        StringList obsoleteversion = (StringList) format.getHeader().getEntry(HeaderTag.OBSOLETEVERSION).getValues();
        assertThat(StringList.of("testCappypkg"), is(obsolete));
        assertThat(IntegerList.of(0), is(obsoleteflags));
        assertThat(StringList.of(""), is(obsoleteversion));
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
        StringList require = (StringList) format.getHeader().getEntry(HeaderTag.REQUIRENAME).getValues();
        IntegerList requireflags = (IntegerList) format.getHeader().getEntry(HeaderTag.REQUIREFLAGS).getValues();
        StringList requireversion = (StringList) format.getHeader().getEntry(HeaderTag.REQUIREVERSION).getValues();
        assertThat(StringList.of("httpd"), is(require.subList(require.size() - 2, require.size() - 1)));
        assertThat(IntegerList.of(Flags.GREATER | Flags.EQUAL), is(requireflags.subList(requireflags.size() - 2, requireflags.size() - 1)));
        assertThat(StringList.of("1.0"), is(requireversion.subList(require.size() - 2, require.size() - 1)));
        assertThat(StringList.of("httpd"), is(require.subList(require.size() - 1, require.size())));
        assertThat(IntegerList.of(Flags.LESS), is(requireflags.subList(requireflags.size() - 1, requireflags.size())));
        assertThat(StringList.of("2.0"), is(requireversion.subList(require.size() - 1, require.size())));
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
        StringList provide = (StringList) format.getHeader().getEntry(HeaderTag.PROVIDENAME).getValues();
        IntegerList provideflags = (IntegerList) format.getHeader().getEntry(HeaderTag.PROVIDEFLAGS).getValues();
        StringList provideversion = (StringList) format.getHeader().getEntry(HeaderTag.PROVIDEVERSION).getValues();
        assertEquals(1, provide.size());
        assertThat(StringList.of("testProvideOverride"), is(provide));
        assertThat(IntegerList.of(Flags.EQUAL), is(provideflags));
        assertThat(StringList.of("1.0"), is(provideversion));
    }

    @Test
    public void testAddHeaderEntry() {
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.addHeaderEntry(HeaderTag.CHANGELOGTIME, Integer.valueOf(1));
        Assertions.assertThrows(ClassCastException.class, () ->
                rpmBuilder.addHeaderEntry(HeaderTag.CHANGELOGNAME, 1L));
        rpmBuilder.addHeaderEntry(HeaderTag.CHANGELOGTIME, "Mon Jan 01 2016");
        Assertions.assertThrows(ClassCastException.class, () ->
                rpmBuilder.addHeaderEntry(HeaderTag.CHANGELOGTIME, 1L));
        Assertions.assertThrows(ClassCastException.class, () -> {
            short s = (short) 1;
            rpmBuilder.addHeaderEntry(HeaderTag.CHANGELOGTIME, s);
        });
        Assertions.assertThrows(ClassCastException.class, () -> {
            char c = 'c';
            rpmBuilder.addHeaderEntry(HeaderTag.CHANGELOGTIME, c);
        });
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
                directives, "jabberwocky", "vorpal", true);
        rpmBuilder.build(getTargetDir());
        Path path = getTargetDir().resolve("test-compressed-1.0-1.noarch.rpm");
        new RpmReader().readHeader(path);
    }

    private Path getTargetDir() {
        return Paths.get("build");
    }
}
