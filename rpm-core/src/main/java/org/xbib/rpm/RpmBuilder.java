package org.xbib.rpm;

import org.xbib.io.compress.bzip2.Bzip2OutputStream;
import org.xbib.io.compress.xz.FilterOptions;
import org.xbib.io.compress.xz.LZMA2Options;
import org.xbib.io.compress.xz.X86Options;
import org.xbib.io.compress.xz.XZOutputStream;
import org.xbib.rpm.changelog.ChangelogHandler;
import org.xbib.rpm.exception.ChangelogParseException;
import org.xbib.rpm.exception.RpmException;
import org.xbib.rpm.format.Flags;
import org.xbib.rpm.format.Format;
import org.xbib.rpm.header.EntryType;
import org.xbib.rpm.header.HeaderTag;
import org.xbib.rpm.header.entry.SpecEntry;
import org.xbib.rpm.io.ChannelWrapper;
import org.xbib.rpm.io.WritableChannelWrapper;
import org.xbib.rpm.lead.Architecture;
import org.xbib.rpm.lead.Os;
import org.xbib.rpm.lead.PackageType;
import org.xbib.rpm.payload.CompressionType;
import org.xbib.rpm.payload.Contents;
import org.xbib.rpm.payload.CpioHeader;
import org.xbib.rpm.payload.Directive;
import org.xbib.rpm.security.HashAlgo;
import org.xbib.rpm.security.SignatureGenerator;
import org.xbib.rpm.signature.SignatureTag;
import org.xbib.rpm.trigger.Trigger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public class RpmBuilder {

    private static final Integer SHASIZE = 41;

    private static final String DEFAULTSCRIPTPROG = "/bin/sh";

    private static final char[] ILLEGAL_CHARS_VARIABLE = new char[]{'-', '/'};

    private static final char[] ILLEGAL_CHARS_NAME = new char[]{'/', ' ', '\t', '\n', '\r'};

    private static final String hex = "0123456789abcdef";

    private final Format format = new Format();

    private final List<Dependency> requires = new ArrayList<>();

    private final List<Dependency> obsoletes = new ArrayList<>();

    private final List<Dependency> conflicts = new ArrayList<>();

    private final Map<String, Dependency> provides = new LinkedHashMap<>();

    private final List<String> triggerscripts = new ArrayList<>();

    private final List<String> triggerscriptprogs = new ArrayList<>();

    private final List<String> triggernames = new ArrayList<>();

    private final List<String> triggerversions = new ArrayList<>();

    private final List<Integer> triggerflags = new ArrayList<>();

    private final List<Integer> triggerindexes = new ArrayList<>();

    private Contents contents = new Contents();

    private InputStream privateKeyRing;

    private Long privateKeyId;

    private String privateKeyPassphrase;

    private int triggerCounter = 0;

    private HashAlgo hashAlgo;

    private CompressionType compressionType;

    private String packageName;

    public RpmBuilder() {
        this(HashAlgo.SHA256, CompressionType.GZIP);
    }

    /**
     * Initializes the builder and sets some required fields to known values.
     * @param hashAlgo the hash algo
     *  @param compressionType compression type
     */
    public RpmBuilder(HashAlgo hashAlgo, CompressionType compressionType) {
        this.hashAlgo = hashAlgo;
        this.compressionType = compressionType;
        format.getHeader().createEntry(HeaderTag.HEADERI18NTABLE, "C");
        format.getHeader().createEntry(HeaderTag.BUILDTIME, (int) (System.currentTimeMillis() / 1000));
        format.getHeader().createEntry(HeaderTag.RPMVERSION, "4.6.0");
        format.getHeader().createEntry(HeaderTag.PAYLOADFORMAT, "cpio");
        format.getHeader().createEntry(HeaderTag.PAYLOADCOMPRESSOR, compressionType.name().toLowerCase());
        addDependencyLess("rpmlib(VersionedDependencies)", "3.0.3-1");
        addDependencyLess("rpmlib(CompressedFileNames)", "3.0.4-1");
        addDependencyLess("rpmlib(PayloadFilesHavePrefix)", "4.0-1");
        addDependencyLess("rpmlib(FileDigests)", "4.6.0-1");
        addDependencyLess("rpmlib(PayloadIsBzip2)", "3.0.5-1");
        addDependencyLess("rpmlib(PayloadIsLzma)", "4.4.2-1");
        addDependencyLess("rpmlib(PayloadIsXz)", "5.2-1");
    }

    /**
     * Returns an array of String with the name of every dependency from a list of dependencies.
     *
     * @param dependencyList List of dependencies
     * @return String[] with all names of the dependencies
     */
    private static String[] getArrayOfNames(List<Dependency> dependencyList) {
        List<String> list = new ArrayList<>();
        for (Dependency dependency : dependencyList) {
            list.add(dependency.getName());
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Returns an array of String with the version of every dependency from a list of dependencies.
     *
     * @param dependencyList List of dependencies
     * @return String[] with all versions of the dependencies
     */
    private static String[] getArrayOfVersions(List<Dependency> dependencyList) {
        List<String> versionList = new ArrayList<>();
        for (Dependency dependency : dependencyList) {
            versionList.add(dependency.getVersion());
        }
        return versionList.toArray(new String[versionList.size()]);
    }

    /**
     * Returns an array of Integer with the flags of every dependency from a list of dependencies.
     *
     * @param dependencyList List of dependencies
     * @return Integer[] with all flags of the dependencies
     */
    private static Integer[] getArrayOfFlags(List<Dependency> dependencyList) {
        List<Integer> flagsList = new ArrayList<>();
        for (Dependency dependency : dependencyList) {
            flagsList.add(dependency.getFlags());
        }
        return flagsList.toArray(new Integer[flagsList.size()]);
    }

    /**
     * Returns an array of String with the name of every dependency from a list of dependencies.
     *
     * @param dependencies List of dependencies
     * @return String[] with all names of the dependencies
     */
    private static String[] getArrayOfNames(Map<String, Dependency> dependencies) {
        List<String> nameList = new ArrayList<>();
        for (Dependency dependency : dependencies.values()) {
            nameList.add(dependency.getName());
        }
        return nameList.toArray(new String[nameList.size()]);
    }

    /**
     * Returns an array of String with the version of every dependency from a list of dependencies.
     *
     * @param dependencies List of dependencies
     * @return String[] with all versions of the dependencies
     */
    private static String[] getArrayOfVersions(Map<String, Dependency> dependencies) {
        List<String> versionList = new ArrayList<>();
        for (Dependency dependency : dependencies.values()) {
            versionList.add(dependency.getVersion());
        }
        return versionList.toArray(new String[versionList.size()]);
    }

    /**
     * Returns an array of Integer with the flags of every dependency from a list of dependencies.
     *
     * @param dependencies List of dependencies
     * @return Integer[] with all flags of the dependencies
     */
    private static Integer[] getArrayOfFlags(Map<String, Dependency> dependencies) {
        List<Integer> flagsList = new ArrayList<>();
        for (Dependency dependency : dependencies.values()) {
            flagsList.add(dependency.getFlags());
        }
        return flagsList.toArray(new Integer[flagsList.size()]);
    }

    private static int difference(int start, int boundary) {
        return ((boundary + 1) - (start & boundary)) & boundary;
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(hex.charAt(((int) aByte & 0xf0) >> 4)).append(hex.charAt((int) aByte & 0x0f));
        }
        return sb.toString();
    }

    public void addBuiltinDirectory(String builtinDirectory) {
        contents.addLocalBuiltinDirectory(builtinDirectory);
    }

    public void addObsoletes(String name, int comparison, String version) {
        obsoletes.add(new Dependency(name, version, comparison));
    }

    public void addObsoletesLess(CharSequence name, CharSequence version) {
        int flag = Flags.LESS | Flags.EQUAL;
        addObsoletes(name, version, flag);
    }

    public void addObsoletesMore(CharSequence name, CharSequence version) {
        int flag = Flags.GREATER | Flags.EQUAL;
        addObsoletes(name, version, flag);
    }

    public void addObsoletes(CharSequence name, CharSequence version, int flag) {
        obsoletes.add(new Dependency(name.toString(), version.toString(), flag));
    }

    public void addConflicts(String name, int comparison, String version) {
        conflicts.add(new Dependency(name, version, comparison));
    }

    public void addConflictsLess(CharSequence name, CharSequence version) {
        int flag = Flags.LESS | Flags.EQUAL;
        addConflicts(name, version, flag);
    }

    public void addConflictsMore(CharSequence name, CharSequence version) {
        int flag = Flags.GREATER | Flags.EQUAL;
        addConflicts(name, version, flag);
    }

    public void addConflicts(CharSequence name, CharSequence version, int flag) {
        conflicts.add(new Dependency(name.toString(), version.toString(), flag));
    }

    public void addProvides(String name, String version) {
        provides.put(name, new Dependency(name, version, version.length() > 0 ? Flags.EQUAL : 0));
    }

    public void addProvides(CharSequence name, CharSequence version, int flag) {
        provides.put(name.toString(), new Dependency(name.toString(), version.toString(), flag));
    }

    /**
     * Adds a dependency to the RPM package. This dependency version will be marked as the exact
     * requirement, and the package will require the named dependency with exactly this version at
     * install time.
     *
     * @param name       the name of the dependency
     * @param comparison the comparison flag
     * @param version    the version identifier
     */
    public void addDependency(String name, int comparison, String version) {
        requires.add(new Dependency(name, version, comparison));
    }

    /**
     * Adds a dependency to the RPM package. This dependency version will be marked as the maximum
     * allowed, and the package will require the named dependency with this version or lower at
     * install time.
     *
     * @param name    the name of the dependency
     * @param version the version identifier
     */
    public void addDependencyLess(CharSequence name, CharSequence version) {
        int flag = Flags.LESS | Flags.EQUAL;
        if (name.toString().startsWith("rpmlib(")) {
            flag = flag | Flags.RPMLIB;
        }
        addDependency(name, version, flag);
    }

    /**
     * Adds a dependency to the RPM package. This dependency version will be marked as the minimum
     * allowed, and the package will require the named dependency with this version or higher at
     * install time.
     *
     * @param name    the name of the dependency.
     * @param version the version identifier.
     */
    public void addDependencyMore(CharSequence name, CharSequence version) {
        addDependency(name, version, Flags.GREATER | Flags.EQUAL);
    }

    /**
     * Adds a dependency to the RPM package. This dependency version will be marked as the exact
     * requirement, and the package will require the named dependency with exactly this version at
     * install time.
     *
     * @param name    the name of the dependency.
     * @param version the version identifier.
     * @param flag    the file flags
     */
    public void addDependency(CharSequence name, CharSequence version, int flag) {
        requires.add(new Dependency(name.toString(), version.toString(), flag));
    }

    /**
     * Adds a header entry value to the header. For example use this to set the source RPM package
     * name on your RPM
     *
     * @param entryType   the header tag to set
     * @param value the value to set the header entry with
     */
    public void addHeaderEntry(EntryType entryType, String value) {
        format.getHeader().createEntry(entryType, value);
    }

    /**
     * Adds a header entry byte (8-bit) value to the header.
     *
     * @param entryType   the header tag to set
     * @param value the value to set the header entry with
     * @throws ClassCastException - if the type required by tag.type() is not byte[]
     */
    public void addHeaderEntry(EntryType entryType, byte value) {
        format.getHeader().createEntry(entryType, new byte[]{value});
    }

    /**
     * Adds a header entry char (8-bit) value to the header.
     *
     * @param entryType   the header tag to set
     * @param value the value to set the header entry with
     * @throws ClassCastException - if the type required by tag.type() is not byte[]
     */
    public void addHeaderEntry(EntryType entryType, char value) {
        format.getHeader().createEntry(entryType, new byte[]{(byte) value});
    }

    /**
     * Adds a header entry short (16-bit) value to the header.
     *
     * @param entryType   the header tag to set
     * @param value the value to set the header entry with
     * @throws ClassCastException - if the type required by tag.type() is not short[]
     */
    public void addHeaderEntry(EntryType entryType, short value) {
        format.getHeader().createEntry(entryType, new short[]{value});
    }

    /**
     * Adds a header entry int (32-bit) value to the header.
     *
     * @param entryType  the header tag to set
     * @param value the value to set the header entry with
     * @throws ClassCastException if the type required by tag.type() is not Integer[]
     */
    public void addHeaderEntry(EntryType entryType, Integer value) {
        format.getHeader().createEntry(entryType, new Integer[]{value});
    }

    /**
     * Adds a header entry long (64-bit) value to the header.
     *
     * @param entryType   the header tag to set
     * @param value the value to set the header entry with
     * @throws ClassCastException - if the type required by tag.type() is not long[]
     */
    public void addHeaderEntry(EntryType entryType, long value) {
        format.getHeader().createEntry(entryType, new long[]{value});
    }

    /**
     * Adds a header entry byte array (8-bit) value to the header.
     *
     * @param entryType   the header tag to set
     * @param value the value to set the header entry with
     * @throws ClassCastException - if the type required by tag.type() is not byte[]
     */
    public void addHeaderEntry(EntryType entryType, byte[] value) {
        format.getHeader().createEntry(entryType, value);
    }

    /**
     * Adds a header entry short array (16-bit) value to the header.
     *
     * @param entryType   the header tag to set
     * @param value the value to set the header entry with
     * @throws ClassCastException - if the type required by tag.type() is not short[]
     */
    public void addHeaderEntry(EntryType entryType, short[] value) {
        format.getHeader().createEntry(entryType, value);
    }

    /**
     * Adds a header entry int (32-bit) array value to the header.
     *
     * @param entryType   the header tag to set
     * @param value the value to set the header entry with
     * @throws ClassCastException - if the type required by tag.type() is not int[]
     */
    public void addHeaderEntry(EntryType entryType, Integer[] value) {
        format.getHeader().createEntry(entryType, value);
    }

    /**
     * Adds a header entry long (64-bit) array value to the header.
     *
     * @param entryType   the header tag to set
     * @param value the value to set the header entry with
     * @throws ClassCastException - if the type required by tag.type() is not long[]
     */
    public void addHeaderEntry(EntryType entryType, long[] value) {
        format.getHeader().createEntry(entryType, value);
    }

    /**
     * @param illegalChars the illegal characters to check for.
     * @param variable     the character sequence to check for illegal characters.
     * @param variableName the name to include in IllegalArgumentException
     * @throws IllegalArgumentException if passed in character sequence contains dashes.
     */
    private void checkVariableContainsIllegalChars(char[] illegalChars, CharSequence variable, String variableName) {
        for (int i = 0; i < variable.length(); i++) {
            char currChar = variable.charAt(i);
            for (char illegalChar : illegalChars) {
                if (currChar == illegalChar) {
                    throw new IllegalArgumentException(variableName + " with value: '" + variable +
                            "' contains illegal character " + currChar);
                }
            }
        }
    }

    /**
     * Required Field. Sets the package information, such as the rpm name, the version, and the release number.
     *
     * @param name    the name of the RPM package.
     * @param version the version of the new package.
     * @param release the release number, specified after the version, of the new RPM.
     * @param epoch   the epoch number of the new RPM
     * @throws IllegalArgumentException if version or release contain
     *                                  dashes, as they are explicitly disallowed by RPM file format.
     */
    public void setPackage(CharSequence name, CharSequence version, CharSequence release, Integer epoch) {
        checkVariableContainsIllegalChars(ILLEGAL_CHARS_NAME, name, "name");
        checkVariableContainsIllegalChars(ILLEGAL_CHARS_VARIABLE, version, "version");
        checkVariableContainsIllegalChars(ILLEGAL_CHARS_VARIABLE, release, "release");
        format.getLead().setName(name + "-" + version + "-" + release);
        format.getHeader().createEntry(HeaderTag.NAME, name);
        format.getHeader().createEntry(HeaderTag.VERSION, version);
        format.getHeader().createEntry(HeaderTag.RELEASE, release);
        format.getHeader().createEntry(HeaderTag.EPOCH, epoch);
        this.provides.clear();
        addProvides(String.valueOf(name), "" + epoch + ":" + version + "-" + release);
    }

    public void setPackage(CharSequence name, CharSequence version, CharSequence release) {
        setPackage(name, version, release, 0);
    }

    /**
     * Required Field. Sets the type of the RPM to be either binary or source.
     *
     * @param type the type of RPM to generate.
     */
    public void setType(PackageType type) {
        format.getLead().setType(type);
    }

    /**
     * Sets the platform related headers for the resulting RPM. The platform is specified as a
     * combination of target architecture and OS.
     *
     * @param arch the target architecture.
     * @param os   the target operating system.
     */
    public void setPlatform(Architecture arch, Os os) {
        format.getLead().setArch(arch);
        format.getLead().setOs(os);
        CharSequence archName = arch.toString().toLowerCase();
        CharSequence osName = os.toString().toLowerCase();
        format.getHeader().createEntry(HeaderTag.ARCH, archName);
        format.getHeader().createEntry(HeaderTag.OS, osName);
        format.getHeader().createEntry(HeaderTag.PLATFORM, archName + "-" + osName);
        format.getHeader().createEntry(HeaderTag.RHNPLATFORM, archName);
    }

    /**
     *Sets the platform related headers for the resulting RPM. The platform is specified as a
     * combination of target architecture and OS.
     *
     * @param arch   the target architecture.
     * @param osName the non-standard target operating system.
     */
    public void setPlatform(Architecture arch, CharSequence osName) {
        format.getLead().setArch(arch);
        format.getLead().setOs(Os.UNKNOWN);
        CharSequence archName = arch.toString().toLowerCase();
        format.getHeader().createEntry(HeaderTag.ARCH, archName);
        format.getHeader().createEntry(HeaderTag.OS, osName);
        format.getHeader().createEntry(HeaderTag.PLATFORM, archName + "-" + osName);
        format.getHeader().createEntry(HeaderTag.RHNPLATFORM, archName);
    }

    /**
     * Sets the summary text for the file. The summary is generally a short, one line description of the
     * function of the package, and is often shown by RPM tools.
     *
     * @param summary summary text.
     */
    public void setSummary(CharSequence summary) {
        if (summary != null) {
            format.getHeader().createEntry(HeaderTag.SUMMARY, summary);
        }
    }

    /**
     * Sets the description text for the file. The description is often a paragraph describing the
     * package in detail.
     *
     * @param description description text.
     */
    public void setDescription(CharSequence description) {
        if (description != null) {
            format.getHeader().createEntry(HeaderTag.DESCRIPTION, description);
        }
    }

    /**
     *  Sets the build host for the RPM. This is an internal field.
     *
     * @param host hostname of the build machine.
     */
    public void setBuildHost(CharSequence host) {
        if (host != null) {
            format.getHeader().createEntry(HeaderTag.BUILDHOST, host);
        }
    }

    /**
     * Lists the license under which this software is distributed. This field may be
     * displayed by RPM tools.
     *
     * @param license the chosen distribution license.
     */
    public void setLicense(CharSequence license) {
        if (license != null) {
            format.getHeader().createEntry(HeaderTag.LICENSE, license);
        }
    }

    /**
     * Software group to which this package belongs. The group describes what sort of
     * function the software package provides.
     *
     * @param group target group.
     */
    public void setGroup(CharSequence group) {
        if (group != null) {
            format.getHeader().createEntry(HeaderTag.GROUP, group);
        }
    }

    /**
     * Distribution tag listing the distributable package.
     *
     * @param distribution the distribution.
     */
    public void setDistribution(CharSequence distribution) {
        if (distribution != null) {
            format.getHeader().createEntry(HeaderTag.DISTRIBUTION, distribution);
        }
    }

    /**
     * Vendor tag listing the organization providing this software package.
     *
     * @param vendor software vendor.
     */
    public void setVendor(CharSequence vendor) {
        if (vendor != null) {
            format.getHeader().createEntry(HeaderTag.VENDOR, vendor);
        }
    }

    /**
     * Build packager, usually the username of the account building this RPM.
     *
     * @param packager packager name.
     */
    public void setPackager(CharSequence packager) {
        if (packager != null) {
            format.getHeader().createEntry(HeaderTag.PACKAGER, packager);
        }
    }

    /**
     * Website URL for this package, usually a project site.
     *
     * @param url the URL
     */
    public void setUrl(CharSequence url) {
        if (url != null) {
            format.getHeader().createEntry(HeaderTag.URL, url);
        }
    }

    /**
     * Declares a dependency that this package exports, and that other packages can use to
     * provide library functions.  Note that this method causes the existing provides set to be
     * overwritten and therefore should be called before adding any other contents via
     * the <code>addProvides()</code> methods.
     * You should use <code>addProvides()</code> instead.
     *
     * @param provides dependency provided by this package.
     */
    public void setProvides(CharSequence provides) {
        if (provides != null) {
            this.provides.clear();
            addProvides(provides, "", Flags.EQUAL);
        }
    }

    /**
     * Sets the group of contents to include in this RPM. Note that this method causes the existing
     * file set to be overwritten and therefore should be called before adding any other contents via
     * the <code>addFile()</code> methods.
     *
     * @param contents the set of contents to use in constructing this RPM.
     */
    public void setFiles(Contents contents) {
        this.contents = contents;
    }

    public Contents getContents() {
        return contents;
    }

    /**
     * Adds a source rpm.
     *
     * @param rpm name of rpm source file
     */
    public void setSourceRpm(String rpm) {
        if (rpm != null) {
            format.getHeader().createEntry(HeaderTag.SOURCERPM, rpm);
        }
    }

    /**
     * Sets the package prefix directories to allow any files installed under
     * them to be relocatable.
     *
     * @param prefixes Path prefixes which may be relocated
     */
    public void setPrefixes(String... prefixes) {
        if (prefixes != null && 0 < prefixes.length) {
            format.getHeader().createEntry(HeaderTag.PREFIXES, prefixes);
        }
    }

    /**
     * Declares a script file to be run as part of the RPM pre-transaction. The
     * script will be run using the interpreter declared with the
     * {@link #setPreTransProgram(String)} method.
     *
     * @param path Script to run (i.e. shell commands)
     * @throws IOException there was an IO error
     */
    public void setPreTrans(String path) throws IOException {
        setPreTrans(Paths.get(path));
    }

    /**
     * Declares a script file to be run as part of the RPM pre-transaction. The
     * script will be run using the interpreter declared with the
     * {@link #setPreTransProgram(String)} method.
     *
     * @param path Script to run (i.e. shell commands)
     * @throws IOException there was an IO error
     */
    public void setPreTrans(Path path) throws IOException {
        setPreTransValue(readScript(path));
    }

    /**
     * Declares a script to be run as part of the RPM pre-transaction. The
     * script will be run using the interpreter declared with the
     * {@link #setPreTransProgram(String)} method.
     *
     * @param content Script contents to run (i.e. shell commands)
     */
    public void setPreTransValue(String content) {
        setPreTransProgram(readProgram(content));
        if (content != null) {
            format.getHeader().createEntry(HeaderTag.PRETRANSSCRIPT, content);
        }
    }

    /**
     * Declares the interpreter to be used when invoking the RPM
     * pre-transaction script that can be set with the
     * {@link #setPreTransValue(String)} method.
     *
     * @param program Path to the interpreter
     */
    public void setPreTransProgram(String program) {
        if (null == program) {
            format.getHeader().createEntry(HeaderTag.PRETRANSPROG, DEFAULTSCRIPTPROG);
        } else if (0 == program.length()) {
            format.getHeader().createEntry(HeaderTag.PRETRANSPROG, DEFAULTSCRIPTPROG);
        } else {
            format.getHeader().createEntry(HeaderTag.PRETRANSPROG, program);
        }
    }

    /**
     * Declares a script file to be run as part of the RPM pre-installation. The
     * script will be run using the interpreter declared with the
     * {@link #setPreInstallProgram(String)} method.
     *
     * @param path Script to run (i.e. shell commands)
     * @throws IOException there was an IO error
     */
    public void setPreInstall(String path) throws IOException {
        setPreInstall(Paths.get(path));
    }

    /**
     * Declares a script file to be run as part of the RPM pre-installation. The
     * script will be run using the interpreter declared with the
     * {@link #setPreInstallProgram(String)} method.
     *
     * @param path Script to run (i.e. shell commands)
     * @throws IOException there was an IO error
     */
    public void setPreInstall(Path path) throws IOException {
        setPreInstallValue(readScript(path));
    }

    /**
     * Declares a script to be run as part of the RPM pre-installation. The
     * script will be run using the interpreter declared with the
     * {@link #setPreInstallProgram(String)} method.
     *
     * @param content Script contents to run (i.e. shell commands)
     */
    public void setPreInstallValue(String content) {
        setPreInstallProgram(readProgram(content));
        if (content != null) {
            format.getHeader().createEntry(HeaderTag.PREINSCRIPT, content);
        }
    }

    /**
     * Declares the interpreter to be used when invoking the RPM
     * pre-installation script that can be set with the
     * {@link #setPreInstallValue(String)} method.
     *
     * @param program Path to the interpretter
     */
    public void setPreInstallProgram(String program) {
        if (program == null) {
            format.getHeader().createEntry(HeaderTag.PREINPROG, DEFAULTSCRIPTPROG);
        } else if (0 == program.length()) {
            format.getHeader().createEntry(HeaderTag.PREINPROG, DEFAULTSCRIPTPROG);
        } else {
            format.getHeader().createEntry(HeaderTag.PREINPROG, program);
        }
    }


    /**
     * Declares a script file to be run as part of the RPM post-installation. The
     * script will be run using the interpreter declared with the
     * {@link #setPostInstallProgram(String)} method.
     *
     * @param path Script to run (i.e. shell commands)
     * @throws IOException there was an IO error
     */
    public void setPostInstall(String path) throws IOException {
        setPostInstall(Paths.get(path));
    }

    /**
     * Declares a script file to be run as part of the RPM post-installation. The
     * script will be run using the interpreter declared with the
     * {@link #setPostInstallProgram(String)} method.
     *
     * @param path Script to run (i.e. shell commands)
     * @throws IOException there was an IO error
     */
    public void setPostInstall(Path path) throws IOException {
        setPostInstallValue(readScript(path));
    }

    /**
     * Declares a script to be run as part of the RPM post-installation. The
     * script will be run using the interpreter declared with the
     * {@link #setPostInstallProgram(String)} method.
     *
     * @param script Script contents to run (i.e. shell commands)
     */
    public void setPostInstallValue(String script) {
        setPostInstallProgram(readProgram(script));
        if (script != null) {
            format.getHeader().createEntry(HeaderTag.POSTINSCRIPT, script);
        }
    }

    /**
     * Declares the interpreter to be used when invoking the RPM
     * post-installation script that can be set with the
     * {@link #setPostInstallValue(String)} method.
     *
     * @param program Path to the interpreter
     */
    public void setPostInstallProgram(String program) {
        if (program == null) {
            format.getHeader().createEntry(HeaderTag.POSTINPROG, DEFAULTSCRIPTPROG);
        } else if (0 == program.length()) {
            format.getHeader().createEntry(HeaderTag.POSTINPROG, DEFAULTSCRIPTPROG);
        } else {
            format.getHeader().createEntry(HeaderTag.POSTINPROG, program);
        }
    }

    /**
     * Declares a script file to be run as part of the RPM pre-uninstallation. The
     * script will be run using the interpreter declared with the
     * {@link #setPreUninstallProgram(String)} method.
     *
     * @param path Script to run (i.e. shell commands)
     * @throws IOException there was an IO error
     */
    public void setPreUninstall(String path) throws IOException {
        setPreUninstall(Paths.get(path));
    }

    /**
     * Declares a script file to be run as part of the RPM pre-uninstallation. The
     * script will be run using the interpreter declared with the
     * {@link #setPreUninstallProgram(String)} method.
     *
     * @param path Script to run (i.e. shell commands)
     * @throws IOException there was an IO error
     */
    public void setPreUninstall(Path path) throws IOException {
        setPreUninstallValue(readScript(path));
    }

    /**
     * Declares a script to be run as part of the RPM pre-uninstallation. The
     * script will be run using the interpreter declared with the
     * {@link #setPreUninstallProgram(String)} method.
     *
     * @param script Script contents to run (i.e. shell commands)
     */
    public void setPreUninstallValue(String script) {
        setPreUninstallProgram(readProgram(script));
        if (script != null) {
            format.getHeader().createEntry(HeaderTag.PREUNSCRIPT, script);
        }
    }

    /**
     * Declares the interpreter to be used when invoking the RPM
     * pre-uninstallation script that can be set with the
     * {@link #setPreUninstallValue(String)} method.
     *
     * @param program Path to the interpreter
     */
    public void setPreUninstallProgram(String program) {
        if (program == null) {
            format.getHeader().createEntry(HeaderTag.PREUNPROG, DEFAULTSCRIPTPROG);
        } else if (0 == program.length()) {
            format.getHeader().createEntry(HeaderTag.PREUNPROG, DEFAULTSCRIPTPROG);
        } else {
            format.getHeader().createEntry(HeaderTag.PREUNPROG, program);
        }
    }


    /**
     * Declares a script file to be run as part of the RPM post-uninstallation. The
     * script will be run using the interpreter declared with the
     * {@link #setPostUninstallProgram(String)} method.
     *
     * @param path Script contents to run (i.e. shell commands)
     * @throws IOException there was an IO error
     */
    public void setPostUninstall(String path) throws IOException {
        setPostUninstall(Paths.get(path));
    }

    /**
     * Declares a script file to be run as part of the RPM post-uninstallation. The
     * script will be run using the interpreter declared with the
     * {@link #setPostUninstallProgram(String)} method.
     *
     * @param path Script contents to run (i.e. shell commands)
     * @throws IOException there was an IO error
     */
    public void setPostUninstall(Path path) throws IOException {
        setPostUninstallValue(readScript(path));
    }

    /**
     * Declares a script to be run as part of the RPM post-uninstallation. The
     * script will be run using the interpreter declared with the
     * {@link #setPostUninstallProgram(String)} method.
     *
     * @param content Script contents to run (i.e. shell commands)
     */
    public void setPostUninstallValue(String content) {
        setPostUninstallProgram(readProgram(content));
        if (content != null) {
            format.getHeader().createEntry(HeaderTag.POSTUNSCRIPT, content);
        }
    }

    /**
     * Declares the interpreter to be used when invoking the RPM
     * post-uninstallation script that can be set with the
     * {@link #setPostUninstallValue(String)} method.
     *
     * @param program Path to the interpreter
     */
    public void setPostUninstallProgram(String program) {
        if (program == null) {
            format.getHeader().createEntry(HeaderTag.POSTUNPROG, DEFAULTSCRIPTPROG);
        } else if (0 == program.length()) {
            format.getHeader().createEntry(HeaderTag.POSTUNPROG, DEFAULTSCRIPTPROG);
        } else {
            format.getHeader().createEntry(HeaderTag.POSTUNPROG, program);
        }
    }

    /**
     * Declares a script file to be run as part of the RPM post-transaction. The
     * script will be run using the interpreter declared with the
     * {@link #setPostTransProgram(String)} method.
     *
     * @param path Script contents to run (i.e. shell commands)
     * @throws IOException there was an IO error
     */
    public void setPostTrans(String path) throws IOException {
        setPostTrans(Paths.get(path));
    }

    /**
     * Declares a script file to be run as part of the RPM post-transaction. The
     * script will be run using the interpreter declared with the
     * {@link #setPostTransProgram(String)} method.
     *
     * @param path Script contents to run (i.e. shell commands)
     * @throws IOException there was an IO error
     */
    public void setPostTrans(Path path) throws IOException {
        setPostTransValue(readScript(path));
    }

    /**
     * Declares a script to be run as part of the RPM post-transaction. The
     * script will be run using the interpreter declared with the
     * {@link #setPostTransProgram(String)} method.
     *
     * @param content Script contents to run (i.e. shell commands)
     */
    public void setPostTransValue(String content) {
        setPostTransProgram(readProgram(content));
        if (content != null) {
            format.getHeader().createEntry(HeaderTag.POSTTRANSSCRIPT, content);
        }
    }

    /**
     * Declares the interpreter to be used when invoking the RPM
     * post-transaction script that can be set with the
     * {@link #setPostTransValue(String)} method.
     *
     * @param program Path to the interpreter
     */
    public void setPostTransProgram(String program) {
        if (program == null) {
            format.getHeader().createEntry(HeaderTag.POSTTRANSPROG, DEFAULTSCRIPTPROG);
        } else if (0 == program.length()) {
            format.getHeader().createEntry(HeaderTag.POSTTRANSPROG, DEFAULTSCRIPTPROG);
        } else {
            format.getHeader().createEntry(HeaderTag.POSTTRANSPROG, program);
        }
    }

    /**
     * Adds a trigger to the RPM package.
     *
     * @param script  the script to add.
     * @param prog    the interpreter with which to run the script.
     * @param depends the map of rpms and versions that will trigger the script
     * @param flag    the trigger type (SCRIPT_TRIGGERPREIN, SCRIPT_TRIGGERIN, SCRIPT_TRIGGERUN, or SCRIPT_TRIGGERPOSTUN)
     * @throws IOException there was an IO error
     */
    public void addTrigger(Path script, String prog, Map<String, Trigger.IntString> depends, int flag)
            throws IOException {
        triggerscripts.add(readScript(script));
        if (null == prog) {
            triggerscriptprogs.add(DEFAULTSCRIPTPROG);
        } else if (0 == prog.length()) {
            triggerscriptprogs.add(DEFAULTSCRIPTPROG);
        } else {
            triggerscriptprogs.add(prog);
        }
        for (Map.Entry<String, Trigger.IntString> depend : depends.entrySet()) {
            triggernames.add(depend.getKey());
            triggerflags.add(depend.getValue().getInt() | flag);
            triggerversions.add(depend.getValue().getString());
            triggerindexes.add(triggerCounter);
        }
        triggerCounter++;
    }

    /**
     * Add the specified file to the repository payload in order.
     * The required header entries will automatically be generated
     * to record the directory names and file names, as well as their
     * digests.
     *
     * @param path   the absolute path at which this file will be installed.
     * @param source the path content to include in this rpm.
     * @param mode   the mode of the target file in standard three octet notation
     * @throws IOException              there was an IO error
     */
    public void addFile(String path, Path source, int mode) throws IOException {
        contents.addFile(path, source, mode);
    }

    /**
     * Add the specified file to the repository payload in order.
     * The required header entries will automatically be generated
     * to record the directory names and file names, as well as their
     * digests.
     *
     * @param path    the absolute path at which this file will be installed.
     * @param source  the file to include in this archive.
     * @param mode    the mode of the target file in standard three octet notation
     * @param dirmode the mode of the parent directories in standard three octet notation, or -1 for default.
     * @throws IOException              there was an IO error
     */
    public void addFile(String path, Path source, int mode, int dirmode) throws IOException {
        contents.addFile(path, source, mode, dirmode);
    }

    /**
     * Add the specified file to the repository payload in order.
     * The required header entries will automatically be generated
     * to record the directory names and file names, as well as their
     * digests.
     *
     * @param path    the absolute path at which this file will be installed.
     * @param source  the file to include in this archive.
     * @param mode    the mode of the target file in standard three octet notation
     * @param dirmode the mode of the parent directories in standard three octet notation, or -1 for default.
     * @param uname   user owner for the given file
     * @param gname   group owner for the given file
     * @throws IOException              there was an IO error
     */
    public void addFile(String path, Path source, int mode, int dirmode, String uname, String gname)
            throws IOException {
        contents.addFile(path, source, mode, null, uname, gname, dirmode);
    }

    /**
     * Add the specified file to the repository payload in order.
     * The required header entries will automatically be generated
     * to record the directory names and file names, as well as their
     * digests.
     *
     * @param path      the absolute path at which this file will be installed.
     * @param source    the file to include in this rpm.
     * @param mode      the mode of the target file in standard three octet notation
     * @param dirmode   the mode of the parent directories in standard three octet notation, or -1 for default.
     * @param directive directive indicating special handling for this file.
     * @param uname     user owner for the given file
     * @param gname     group owner for the given file
     * @throws IOException              there was an IO error
     */
    public void addFile(String path, Path source, int mode, int dirmode, EnumSet<Directive> directive, String uname,
                        String gname) throws IOException {
        contents.addFile(path, source, mode, directive, uname, gname, dirmode);
    }

    /**
     * Add the specified file to the repository payload in order.
     * The required header entries will automatically be generated
     * to record the directory names and file names, as well as their
     * digests.
     *
     * @param path       the absolute path at which this file will be installed.
     * @param source     the file to include in this rpm.
     * @param mode       the mode of the target file in standard three octet notation, or -1 for default.
     * @param dirmode    the mode of the parent directories in standard three octet notation, or -1 for default.
     * @param directive  directive indicating special handling for this file.
     * @param uname      user owner for the given file, or null for default user.
     * @param gname      group owner for the given file, or null for default group.
     * @param addParents whether to create parent directories for the file, defaults to true for other methods.
     * @throws IOException              there was an IO error
     */
    public void addFile(String path, Path source, int mode, int dirmode, EnumSet<Directive> directive, String uname,
                        String gname, boolean addParents) throws IOException {
        contents.addFile(path, source, mode, directive, uname, gname, dirmode, addParents);
    }

    /**
     * Add the specified file to the repository payload in order.
     * The required header entries will automatically be generated
     * to record the directory names and file names, as well as their
     * digests.
     *
     * @param path        the absolute path at which this file will be installed.
     * @param source      the file to include in this rpm.
     * @param mode        the mode of the target file in standard three octet notation, or -1 for default.
     * @param dirmode     the mode of the parent directories in standard three octet notation, or -1 for default.
     * @param directive   directive indicating special handling for this file.
     * @param uname       user owner for the given file, or null for default user.
     * @param gname       group owner for the given file, or null for default group.
     * @param addParents  whether to create parent directories for the file, defaults to true for other methods.
     * @param verifyFlags verify flags
     * @throws IOException              there was an IO error
     */
    public void addFile(String path, Path source, int mode, int dirmode, EnumSet<Directive> directive, String uname,
                        String gname, boolean addParents, int verifyFlags) throws IOException {
        contents.addFile(path, source, mode, directive, uname, gname, dirmode, addParents, verifyFlags);
    }

    /**
     * Add the specified file to the repository payload in order.
     * The required header entries will automatically be generated
     * to record the directory names and file names, as well as their
     * digests.
     *
     * @param path      the absolute path at which this file will be installed.
     * @param source    the file to include in this rpm.
     * @param mode      the mode of the target file in standard three octet notation
     * @param directive directive indicating special handling for this file.
     * @param uname     user owner for the given file
     * @param gname     group owner for the given file
     * @throws IOException              there was an IO error
     */
    public void addFile(String path, Path source, int mode, EnumSet<Directive> directive, String uname, String gname)
            throws IOException {
        contents.addFile(path, source, mode, directive, uname, gname);
    }

    /**
     * Add the specified file to the repository payload in order.
     * The required header entries will automatically be generated
     * to record the directory names and file names, as well as their
     * digests.
     *
     * @param path      the absolute path at which this file will be installed.
     * @param source    the file content to include in this rpm.
     * @param mode      the mode of the target file in standard three octet notation
     * @param directive directive indicating special handling for this file.
     * @throws IOException              there was an IO error
     */
    public void addFile(String path, Path source, int mode, EnumSet<Directive> directive)
            throws IOException {
        contents.addFile(path, source, mode, directive);
    }

    /**
     * Adds the file to the repository with the default mode of <code>644</code>.
     *
     * @param path   the absolute path at which this file will be installed.
     * @param source the file content to include in this archive.
     * @throws IOException              there was an IO error
     */
    public void addFile(String path, Path source)
            throws IOException {
        contents.addFile(path, source);
    }

    /**
     * Add the specified file to the repository payload in order by URL.
     * The required header entries will automatically be generated
     * to record the directory names and file names, as well as their
     * digests.
     *
     * @param path    the absolute path at which this file will be installed.
     * @param source  the file content to include in this rpm.
     * @param mode    the mode of the target file in standard three octet notation
     * @param dirmode the mode of the parent directories in standard three octet notation, or -1 for default.
     * @throws IOException there was an IO error
     */
    public void addURL(String path, URL source, int mode, int dirmode) throws IOException {
        contents.addURL(path, source, mode, null, null, null, dirmode);
    }

    /**
     * Add the specified file to the repository payload in order by URL.
     * The required header entries will automatically be generated
     * to record the directory names and file names, as well as their
     * digests.
     *
     * @param path     the absolute path at which this file will be installed.
     * @param source   the file content to include in this rpm.
     * @param mode     the mode of the target file in standard three octet notation
     * @param dirmode  the mode of the parent directories in standard three octet notation, or -1 for default.
     * @param username ownership of added file
     * @param group    ownership of added file
     * @throws IOException              there was an IO error
     */
    public void addURL(String path, URL source, int mode, int dirmode, String username, String group)
            throws IOException {
        contents.addURL(path, source, mode, null, username, group, dirmode);
    }

    /**
     * Add the specified file to the repository payload in order by URL.
     * The required header entries will automatically be generated
     * to record the directory names and file names, as well as their
     * digests.
     *
     * @param path       the absolute path at which this file will be installed.
     * @param source     the file content to include in this rpm.
     * @param mode       the mode of the target file in standard three octet notation
     * @param dirmode    the mode of the parent directories in standard three octet notation, or -1 for default.
     * @param directives directive indicating special handling for this file.
     * @param username   ownership of added file
     * @param group      ownership of added file
     * @throws IOException              there was an IO error
     */
    public void addURL(String path, URL source, int mode, int dirmode, EnumSet<Directive> directives, String username,
                       String group) throws IOException {
        contents.addURL(path, source, mode, directives, username, group, dirmode);
    }

    /**
     * Adds the directory to the repository with the default mode of <code>644</code>.
     *
     * @param path the absolute path to add as a directory.
     * @throws IOException              there was an IO error
     */
    public void addDirectory(String path) throws IOException {
        contents.addDirectory(path);
    }

    /**
     * Adds the directory to the repository.
     *
     * @param path        the absolute path to add as a directory.
     * @param permissions the mode of the directory in standard three octet notation.
     * @param directives  directive indicating special handling for this file.
     * @param uname       user owner of the directory
     * @param gname       group owner of the directory
     * @throws IOException              there was an IO error
     */
    public void addDirectory(String path, int permissions, EnumSet<Directive> directives, String uname, String gname)
            throws IOException {
        contents.addDirectory(path, permissions, directives, uname, gname);
    }

    /**
     * Adds the directory to the repository.
     *
     * @param path        the absolute path to add as a directory.
     * @param permissions the mode of the directory in standard three octet notation.
     * @param directives  directives indicating special handling for this file.
     * @param uname       user owner of the directory
     * @param gname       group owner of the directory
     * @param addParents  whether to add parent directories to the rpm
     * @throws IOException              there was an IO error
     */
    public void addDirectory(String path, int permissions, EnumSet<Directive> directives, String uname, String gname,
                             boolean addParents) throws IOException {
        contents.addDirectory(path, permissions, directives, uname, gname, addParents);
    }

    /**
     * Adds the directory to the repository with the default mode of <code>644</code>.
     *
     * @param path      the absolute path to add as a directory.
     * @param directive directive indicating special handling for this file.
     * @throws IOException              there was an IO error
     */
    public void addDirectory(String path, EnumSet<Directive> directive) throws IOException {
        contents.addDirectory(path, directive);
    }

    /**
     * Adds a symbolic link to the repository.
     *
     * @param path   the absolute path at which this link will be installed.
     * @param target the path of the file this link will point to.
     * @throws IOException              there was an IO error
     */
    public void addLink(String path, String target) throws IOException {
        contents.addLink(path, target);
    }

    /**
     * Adds a symbolic link to the repository.
     *
     * @param path        the absolute path at which this link will be installed.
     * @param target      the path of the file this link will point to.
     * @param permissions the permissions flags
     * @throws IOException              there was an IO error
     */
    public void addLink(String path, String target, int permissions)
            throws IOException {
        contents.addLink(path, target, permissions);
    }

    /**
     * Adds a symbolic link to the repository.
     *
     * @param path        the absolute path at which this link will be installed.
     * @param target      the path of the file this link will point to.
     * @param permissions the permissions flags
     * @param username    user owner of the link
     * @param groupname   group owner of the link
     * @throws IOException              there was an IO error
     */
    public void addLink(String path, String target, int permissions, String username, String groupname)
            throws IOException {
        contents.addLink(path, target, permissions, username, groupname);
    }

    /**
     * Adds the supplied Changelog path as a Changelog to the header.
     *
     * @param changelogFile File containing the Changelog information
     * @throws IOException             if file does not exist or cannot be read
     * @throws ChangelogParseException if file is not of the correct format.
     */
    public void addChangelog(String changelogFile) throws IOException, ChangelogParseException {
        new ChangelogHandler(format.getHeader()).addChangeLog(Paths.get(changelogFile));
    }

    /**
     * Adds the supplied Changelog path as a Changelog to the header.
     *
     * @param changelogFile File containing the Changelog information
     * @throws IOException             if file does not exist or cannot be read
     * @throws ChangelogParseException if file is not of the correct format.
     */
    public void addChangelog(Path changelogFile) throws IOException, ChangelogParseException {
        new ChangelogHandler(format.getHeader()).addChangeLog(changelogFile);
    }

    /**
     * Adds the supplied Changelog file as a Changelog to the header.
     *
     * @param changelogFile URL containing the Changelog information
     * @throws IOException             if file does not exist or cannot be read
     * @throws ChangelogParseException if file is not of the correct format.
     */
    public void addChangelog(URL changelogFile) throws IOException, ChangelogParseException {
        new ChangelogHandler(format.getHeader()).addChangeLog(changelogFile);
    }

    /**
     * Adds the supplied Changelog file as a Changelog to the header.
     *
     * @param changelogFile URL containing the Changelog information
     * @throws IOException             if file does not exist or cannot be read
     * @throws ChangelogParseException if file is not of the correct format.
     */
    public void addChangelog(InputStream changelogFile) throws IOException, ChangelogParseException {
        new ChangelogHandler(format.getHeader()).addChangeLog(changelogFile);
    }

    /**
     * Sets the PGP key ring used for header and header + payload signature.
     *
     * @param privateKeyRing the private key ring input stream
     * @throws IOException             if file does not exist or cannot be read
     */
    public void setPrivateKeyRing(String privateKeyRing) throws IOException {
        setPrivateKeyRing(Paths.get(privateKeyRing));
    }

    /**
     * Sets the PGP key ring used for header and header + payload signature.
     *
     * @param privateKeyRing the private key ring input stream
     * @throws IOException             if file does not exist or cannot be read
     */
    public void setPrivateKeyRing(Path privateKeyRing) throws IOException {
        setPrivateKeyRing(Files.newInputStream(privateKeyRing));
    }

    /**
     * Sets the PGP key ring used for header and header + payload signature.
     *
     * @param privateKeyRing the private key ring input stream
     */
    public void setPrivateKeyRing(InputStream privateKeyRing) {
        this.privateKeyRing = privateKeyRing;
    }

    /**
     * Selects a private key from the current {@link #setPrivateKeyRing(java.io.InputStream) private key ring}.
     * If no key is specified, the first signing key will be selected.
     *
     * @param privateKeyId long value from hex key id
     */
    public void setPrivateKeyId(String privateKeyId) {
        setPrivateKeyId(Long.decode("0x" + privateKeyId));
    }

    /**
     * Selects a private key from the current {@link #setPrivateKeyRing(java.io.InputStream) private key ring}.
     * If no key is specified, the first signing key will be selected.
     *
     * @param privateKeyId long value from hex key id
     */
    public void setPrivateKeyId(Long privateKeyId) {
        this.privateKeyId = privateKeyId;
    }

    /**
     * Passphrase for the private key.
     *
     * @param privateKeyPassphrase the private key pass phrase
     */
    public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
        this.privateKeyPassphrase = privateKeyPassphrase;
    }

    /**
     * Set RPM package name.
     *
     * @param packageName the RPM package name
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Get package name.
     * @return the RPM package name or null
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Generates an RPM with a standard name consisting of the RPM package name, version, release,
     * and type in the given directory.
     *
     * @param directory the destination path for the new RPM file.
     * @throws IOException  there was an IO error
     * @throws RpmException if RPM could not be generated
     */
    public void build(Path directory) throws RpmException, IOException {
        if (packageName == null) {
            setPackageName(format.getLead().getName() + "." + format.getLead().getArch().toString().toLowerCase() + ".rpm");
        }
        Path path = directory.resolve(packageName);
        try (SeekableByteChannel channel = Files.newByteChannel(path,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            build(channel);
        }
    }

    /**
     * Generates the RPM archive to the provided file channel.
     *
     * @param channel the {@link SeekableByteChannel} to which the resulting RPM archive will be written.
     * @throws IOException              there was an IO error
     * @throws RpmException if RPM generation fails
     */
    @SuppressWarnings("unchecked")
    public void build(SeekableByteChannel channel) throws RpmException, IOException {
        WritableChannelWrapper output = new WritableChannelWrapper(channel);
        format.getHeader().createEntry(HeaderTag.REQUIRENAME, getArrayOfNames(requires));
        format.getHeader().createEntry(HeaderTag.REQUIREVERSION, getArrayOfVersions(requires));
        format.getHeader().createEntry(HeaderTag.REQUIREFLAGS, getArrayOfFlags(requires));
        if (obsoletes.size() > 0) {
            format.getHeader().createEntry(HeaderTag.OBSOLETENAME, getArrayOfNames(obsoletes));
            format.getHeader().createEntry(HeaderTag.OBSOLETEVERSION, getArrayOfVersions(obsoletes));
            format.getHeader().createEntry(HeaderTag.OBSOLETEFLAGS, getArrayOfFlags(obsoletes));
        }
        if (conflicts.size() > 0) {
            format.getHeader().createEntry(HeaderTag.CONFLICTNAME, getArrayOfNames(conflicts));
            format.getHeader().createEntry(HeaderTag.CONFLICTVERSION, getArrayOfVersions(conflicts));
            format.getHeader().createEntry(HeaderTag.CONFLICTFLAGS, getArrayOfFlags(conflicts));
        }
        if (provides.size() > 0) {
            format.getHeader().createEntry(HeaderTag.PROVIDENAME, getArrayOfNames(provides));
            format.getHeader().createEntry(HeaderTag.PROVIDEVERSION, getArrayOfVersions(provides));
            format.getHeader().createEntry(HeaderTag.PROVIDEFLAGS, getArrayOfFlags(provides));
        }
        format.getHeader().createEntry(HeaderTag.SIZE, contents.getTotalSize());
        if (contents.size() > 0) {
            format.getHeader().createEntry(HeaderTag.DIRNAMES, contents.getDirNames());
            format.getHeader().createEntry(HeaderTag.DIRINDEXES, contents.getDirIndexes());
            format.getHeader().createEntry(HeaderTag.BASENAMES, contents.getBaseNames());
        }
        if (triggerCounter > 0) {
            format.getHeader().createEntry(HeaderTag.TRIGGERSCRIPTS,
                    triggerscripts.toArray(new String[triggerscripts.size()]));
            format.getHeader().createEntry(HeaderTag.TRIGGERNAME,
                    triggernames.toArray(new String[triggernames.size()]));
            format.getHeader().createEntry(HeaderTag.TRIGGERVERSION,
                    triggerversions.toArray(new String[triggerversions.size()]));
            format.getHeader().createEntry(HeaderTag.TRIGGERFLAGS,
                    triggerflags.toArray(new Integer[triggerflags.size()]));
            format.getHeader().createEntry(HeaderTag.TRIGGERINDEX,
                    triggerindexes.toArray(new Integer[triggerindexes.size()]));
            format.getHeader().createEntry(HeaderTag.TRIGGERSCRIPTPROG,
                    triggerscriptprogs.toArray(new String[triggerscriptprogs.size()]));
        }
        if (contents.size() > 0) {
            format.getHeader().createEntry(HeaderTag.FILEDIGESTALGOS, hashAlgo.num());
            format.getHeader().createEntry(HeaderTag.FILEDIGESTS, contents.getDigests(hashAlgo));
            format.getHeader().createEntry(HeaderTag.FILESIZES, contents.getSizes());
            format.getHeader().createEntry(HeaderTag.FILEMODES, contents.getModes());
            format.getHeader().createEntry(HeaderTag.FILERDEVS, contents.getRdevs());
            format.getHeader().createEntry(HeaderTag.FILEMTIMES, contents.getMtimes());
            format.getHeader().createEntry(HeaderTag.FILELINKTOS, contents.getLinkTos());
            format.getHeader().createEntry(HeaderTag.FILEFLAGS, contents.getFlags());
            format.getHeader().createEntry(HeaderTag.FILEUSERNAME, contents.getUsers());
            format.getHeader().createEntry(HeaderTag.FILEGROUPNAME, contents.getGroups());
            format.getHeader().createEntry(HeaderTag.FILEVERIFYFLAGS, contents.getVerifyFlags());
            format.getHeader().createEntry(HeaderTag.FILEDEVICES, contents.getDevices());
            format.getHeader().createEntry(HeaderTag.FILEINODES, contents.getInodes());
            format.getHeader().createEntry(HeaderTag.FILELANGS, contents.getLangs());
            format.getHeader().createEntry(HeaderTag.FILECONTEXTS, contents.getContexts());
        }
        format.getHeader().createEntry(HeaderTag.PAYLOADFLAGS, "9");
        SpecEntry<Integer[]> sigsize =
                (SpecEntry<Integer[]>) format.getSignatureHeader().addEntry(SignatureTag.LEGACY_SIGSIZE, 1);
        SpecEntry<Integer[]> signaturHeaderPayloadEntry =
                (SpecEntry<Integer[]>) format.getSignatureHeader().addEntry(SignatureTag.PAYLOADSIZE, 1);
        SpecEntry<byte[]> md5Entry =
                (SpecEntry<byte[]>) format.getSignatureHeader().addEntry(SignatureTag.LEGACY_MD5, 16);
        SpecEntry<String[]> shaEntry =
                (SpecEntry<String[]>) format.getSignatureHeader().addEntry(SignatureTag.SHA1HEADER, 1);
        shaEntry.setSize(SHASIZE);

        SignatureGenerator signatureGenerator = new SignatureGenerator(privateKeyRing, privateKeyId, privateKeyPassphrase);
        signatureGenerator.prepare(format.getSignatureHeader(), hashAlgo);
        format.getLead().write(channel);
        SpecEntry<byte[]> signatureEntry =
                (SpecEntry<byte[]>) format.getSignatureHeader().addEntry(SignatureTag.SIGNATURES, 16);
        signatureEntry.setValues(createHeaderIndex(HeaderTag.SIGNATURES.getCode(), format.getSignatureHeader().count()));
        ChannelWrapper.empty(output, ByteBuffer.allocate(format.getSignatureHeader().write(channel)));

        ChannelWrapper.Key<Integer> sigsizekey = output.start();
        ChannelWrapper.Key<byte[]> shakey = signatureGenerator.startDigest(output, "SHA");
        ChannelWrapper.Key<byte[]> md5key = signatureGenerator.startDigest(output, "MD5");
        signatureGenerator.startBeforeHeader(output, hashAlgo);
        // Region concept. This tag contains an index record which specifies the portion of the Header Record
        // which was used for the calculation of a signature. This data shall be preserved or any header-only signature
        // will be invalidated.
        SpecEntry<byte[]> immutable =
                (SpecEntry<byte[]>) format.getHeader().addEntry(HeaderTag.HEADERIMMUTABLE, 16);
        immutable.setValues(createHeaderIndex(HeaderTag.IMMUTABLE.getCode(), format.getHeader().count()));
        format.getHeader().write(output);
        shaEntry.setValues(new String[]{hex(output.finish(shakey))});
        signatureGenerator.finishAfterHeader(output);
        OutputStream compressedOutputStream = createCompressedStream(Channels.newOutputStream(output));
        WritableChannelWrapper compressedOutput =
                new WritableChannelWrapper(Channels.newChannel(compressedOutputStream));
        ChannelWrapper.Key<Integer> payloadkey = compressedOutput.start();
        int total = 0;
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        for (CpioHeader header : contents.headers()) {
            if ((header.getFlags() & Directive.GHOST.flag()) == Directive.GHOST.flag()) {
                continue;
            }
            String path = header.getName();
            if (path.startsWith("/")) {
                header.setName("." + path);
            }
            total = header.write(compressedOutput, total);
            Object object = contents.getSource(header);
            if (object instanceof Path) {
                try (ReadableByteChannel readableByteChannel = Files.newByteChannel((Path) object)) {
                    while (readableByteChannel.read((ByteBuffer) buffer.rewind()) > 0) {
                        total += compressedOutput.write((ByteBuffer) buffer.flip());
                        buffer.compact();
                    }
                    total += header.skip(compressedOutput, total);
                }
            } else if (object instanceof URL) {
                try (ReadableByteChannel in = Channels.newChannel(((URL) object).openConnection().getInputStream())) {
                    while (in.read((ByteBuffer) buffer.rewind()) > 0) {
                        total += compressedOutput.write((ByteBuffer) buffer.flip());
                        buffer.compact();
                    }
                    total += header.skip(compressedOutput, total);
                }
            } else if (object instanceof CharSequence) {
                CharSequence target = (CharSequence) object;
                total += compressedOutput.write(ByteBuffer.wrap(String.valueOf(target).getBytes(StandardCharsets.UTF_8)));
                total += header.skip(compressedOutput, target.length());
            }
        }
        CpioHeader trailer = new CpioHeader();
        trailer.setLast();
        total = trailer.write(compressedOutput, total);
        trailer.skip(compressedOutput, total);
        int length = compressedOutput.finish(payloadkey);
        int pad = difference(length, 3);
        ChannelWrapper.empty(compressedOutput, ByteBuffer.allocate(pad));
        length += pad;
        signaturHeaderPayloadEntry.setValues(new Integer[]{length});
        // flush compressed stream here
        compressedOutputStream.flush();
        md5Entry.setValues(output.finish(md5key));
        sigsize.setValues(new Integer[]{output.finish(sigsizekey)});
        signatureGenerator.finishAfterPayload(output);
        format.getSignatureHeader().writePending(channel);
    }

    /**
     * Returns the header index.
     *
     * @param tag   the tag to get
     * @param count the number to get
     * @return the header bytes
     */
    private byte[] createHeaderIndex(int tag, int count) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putInt(tag);
        buffer.putInt(0x00000007); // data type (7 = bin entry)
        buffer.putInt(count * -16); // offset,  where to find the data in the storage area
        buffer.putInt(0x00000010); // how many data items are stored in this key
        return buffer.array();
    }

    private OutputStream createCompressedStream(OutputStream outputStream) throws IOException {
        switch (compressionType) {
            case NONE:
                return outputStream;
            case GZIP:
                return new GZIPOutputStream(outputStream, true);
            case BZIP2:
                return new Bzip2OutputStream(outputStream);
            case XZ:
                X86Options x86 = new X86Options();
                LZMA2Options lzma2 = new LZMA2Options();
                FilterOptions[] options = { x86, lzma2 };
                return new XZOutputStream(outputStream, options);
        }
        // not reached
        return outputStream;
    }


    /**
     * Return the content of the specified script file as a String.
     *
     * @param path the script file to be read
     */
    private String readScript(Path path) throws IOException {
        if (path == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = Files.newBufferedReader(path)) {
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Returns the program use to run the specified script (guessed by parsing
     * the shebang at the beginning of the script).
     *
     * @param script script
     */
    private String readProgram(String script) {
        String program = null;
        if (script != null) {
            Pattern pattern = Pattern.compile("^#!(/.*)");
            Matcher matcher = pattern.matcher(script);
            if (matcher.find()) {
                program = matcher.group(1);
            }
        }
        return program;
    }


    /**
     *
     */
    static class Dependency {

        private String name;

        private String version;

        private Integer flags;

        /**
         * Creates a new dependency.
         *
         * @param name    Name (e.g. "httpd")
         * @param version Version (e.g. "1.0")
         * @param flags   Flags (e.g. "GREATER | Flags.EQUAL")
         */
        Dependency(String name, String version, Integer flags) {
            this.name = name;
            this.version = version;
            this.flags = flags;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public Integer getFlags() {
            return flags;
        }
    }

}
