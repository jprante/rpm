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
import org.xbib.rpm.header.IntegerList;
import org.xbib.rpm.header.LongList;
import org.xbib.rpm.header.ShortList;
import org.xbib.rpm.header.StringList;
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
import java.io.InputStreamReader;
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

    private HashAlgo privateKeyHashAlgo;

    private int triggerCounter = 0;

    private final CompressionType compressionType;

    private String packageName;

    public RpmBuilder() {
        this(HashAlgo.SHA1, CompressionType.GZIP);
    }

    /**
     * Initializes the builder and sets some required fields to known values.
     * @param privateKeyHashAlgo the hash algo
     * @param compressionType compression type
     */
    public RpmBuilder(HashAlgo privateKeyHashAlgo, CompressionType compressionType) {
        this.privateKeyHashAlgo = privateKeyHashAlgo;
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

    public void addBuiltinDirectory(String builtinDirectory) {
        contents.addLocalBuiltinDirectory(builtinDirectory);
    }

    public void addObsoletes(String name, int comparison, String version) {
        obsoletes.add(new Dependency(name, version, comparison));
    }

    public void addObsoletesLess(String name, String version) {
        int flag = Flags.LESS | Flags.EQUAL;
        addObsoletes(name, version, flag);
    }

    public void addObsoletesMore(String name, String version) {
        int flag = Flags.GREATER | Flags.EQUAL;
        addObsoletes(name, version, flag);
    }

    public void addObsoletes(String name, String version, int flag) {
        obsoletes.add(new Dependency(name, version, flag));
    }

    public void addConflicts(String name, int comparison, String version) {
        conflicts.add(new Dependency(name, version, comparison));
    }

    public void addConflictsLess(String name, String version) {
        int flag = Flags.LESS | Flags.EQUAL;
        addConflicts(name, version, flag);
    }

    public void addConflictsMore(String name, String version) {
        int flag = Flags.GREATER | Flags.EQUAL;
        addConflicts(name, version, flag);
    }

    public void addConflicts(String name, String version, int flag) {
        conflicts.add(new Dependency(name, version, flag));
    }

    public void addProvides(String name, String version) {
        provides.put(name, new Dependency(name, version, version.length() > 0 ? Flags.EQUAL : 0));
    }

    public void addProvides(String name, int flag, String version) {
        provides.put(name, new Dependency(name, version, flag));
    }

    /**
     * Adds a dependency to the RPM package. This dependency version will be marked as the exact
     * requirement, and the package will require the named dependency with exactly this version at
     * install time.
     *
     * @param name       the name of the dependency
     * @param flags the comparison flag
     * @param version    the version identifier
     */
    public void addDependency(String name, int flags, String version) {
        requires.add(new Dependency(name, version, flags));
    }

    /**
     * Adds a dependency to the RPM package. This dependency version will be marked as the maximum
     * allowed, and the package will require the named dependency with this version or lower at
     * install time.
     *
     * @param name    the name of the dependency
     * @param version the version identifier
     */
    public void addDependencyLess(String name, String version) {
        int flag = Flags.LESS | Flags.EQUAL;
        if (name.startsWith("rpmlib(")) {
            flag = flag | Flags.RPMLIB;
        }
        addDependency(name, flag, version);
    }

    /**
     * Adds a dependency to the RPM package. This dependency version will be marked as the minimum
     * allowed, and the package will require the named dependency with this version or higher at
     * install time.
     *
     * @param name    the name of the dependency.
     * @param version the version identifier.
     */
    public void addDependencyMore(String name, String version) {
        int flag = Flags.GREATER | Flags.EQUAL;
        if (name.startsWith("rpmlib(")) {
            flag = flag | Flags.RPMLIB;
        }
        addDependency(name, flag, version);
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
        format.getHeader().createEntry(entryType, ShortList.of(value));
    }

    /**
     * Adds a header entry int (32-bit) value to the header.
     *
     * @param entryType  the header tag to set
     * @param value the value to set the header entry with
     * @throws ClassCastException if the type required by tag.type() is not IntegerList
     */
    public void addHeaderEntry(EntryType entryType, Integer value) {
        format.getHeader().createEntry(entryType, IntegerList.of(value));
    }

    /**
     * Adds a header entry long (64-bit) value to the header.
     *
     * @param entryType   the header tag to set
     * @param value the value to set the header entry with
     * @throws ClassCastException - if the type required by tag.type() is not LongList
     */
    public void addHeaderEntry(EntryType entryType, long value) {
        format.getHeader().createEntry(entryType, LongList.of(value));
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
     * @throws ClassCastException - if the type required by tag.type() is not ShortList
     */
    public void addHeaderEntry(EntryType entryType, ShortList value) {
        format.getHeader().createEntry(entryType, value);
    }

    /**
     * Adds a header entry int (32-bit) array value to the header.
     *
     * @param entryType   the header tag to set
     * @param value the value to set the header entry with
     * @throws ClassCastException - if the type required by tag.type() is not IntegerList
     */
    public void addHeaderEntry(EntryType entryType, IntegerList value) {
        format.getHeader().createEntry(entryType, value);
    }

    /**
     * Adds a header entry long (64-bit) array value to the header.
     *
     * @param entryType   the header tag to set
     * @param value the value to set the header entry with
     * @throws ClassCastException - if the type required by tag.type() is not LongList
     */
    public void addHeaderEntry(EntryType entryType, LongList value) {
        format.getHeader().createEntry(entryType, value);
    }

    /**
     * @param illegalChars the illegal characters to check for.
     * @param variable     the character sequence to check for illegal characters.
     * @param variableName the name to include in IllegalArgumentException
     * @throws IllegalArgumentException if passed in character sequence contains dashes.
     */
    private void checkVariableContainsIllegalChars(char[] illegalChars, String variable, String variableName) {
        if (variable != null) {
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
    public void setPackage(String name, String version, String release, Integer epoch) {
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

    public void setPackage(String name, String version, String release) {
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
        String archName = arch.toString().toLowerCase();
        String osName = os.toString().toLowerCase();
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
    public void setPlatform(Architecture arch, String osName) {
        format.getLead().setArch(arch);
        format.getLead().setOs(Os.UNKNOWN);
        String archName = arch.toString().toLowerCase();
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
    public void setSummary(String summary) {
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
    public void setDescription(String description) {
        if (description != null) {
            format.getHeader().createEntry(HeaderTag.DESCRIPTION, description);
        }
    }

    /**
     *  Sets the build host for the RPM. This is an internal field.
     *
     * @param host hostname of the build machine.
     */
    public void setBuildHost(String host) {
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
    public void setLicense(String license) {
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
    public void setGroup(String group) {
        if (group != null) {
            format.getHeader().createEntry(HeaderTag.GROUP, group);
        }
    }

    /**
     * Distribution tag listing the distributable package.
     *
     * @param distribution the distribution.
     */
    public void setDistribution(String distribution) {
        if (distribution != null) {
            format.getHeader().createEntry(HeaderTag.DISTRIBUTION, distribution);
        }
    }

    /**
     * Vendor tag listing the organization providing this software package.
     *
     * @param vendor software vendor.
     */
    public void setVendor(String vendor) {
        if (vendor != null) {
            format.getHeader().createEntry(HeaderTag.VENDOR, vendor);
        }
    }

    /**
     * Build packager, usually the username of the account building this RPM.
     *
     * @param packager packager name.
     */
    public void setPackager(String packager) {
        if (packager != null) {
            format.getHeader().createEntry(HeaderTag.PACKAGER, packager);
        }
    }

    /**
     * Website URL for this package, usually a project site.
     *
     * @param url the URL
     */
    public void setUrl(String url) {
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
    public void setProvides(String provides) {
        if (provides != null) {
            this.provides.clear();
            addProvides(provides, Flags.EQUAL, "");
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

    /**
     * Get the contents.
     *
     * @return the contents
     */
    public Contents getContents() {
        return contents;
    }

    /**
     * Adds a source rpm.
     *
     * @param sourceRpm name of rpm source file
     */
    public void setSourceRpm(String sourceRpm) {
        if (sourceRpm != null && !sourceRpm.isEmpty()) {
            format.getHeader().createEntry(HeaderTag.SOURCERPM, sourceRpm);
        }
    }

    /**
     * Sets the package prefix directories to allow any files installed under
     * them to be relocatable.
     *
     * @param prefixes Path prefixes which may be relocated
     */
    public void setPrefixes(List<String> prefixes) {
        if (prefixes != null && !prefixes.isEmpty()) {
            format.getHeader().createEntry(HeaderTag.PREFIXES, new StringList(prefixes));
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
    public void setPreTrans(Path path) throws IOException {
        setPreTrans(readScript(path));
    }

    /**
     * Declares a script to be run as part of the RPM pre-transaction. The
     * script will be run using the interpreter declared with the
     * {@link #setPreTransProgram(String)} method.
     *
     * @param content Script contents to run (i.e. shell commands)
     */
    public void setPreTrans(String content) {
        setPreTransProgram(readProgram(content));
        if (content != null) {
            format.getHeader().createEntry(HeaderTag.PRETRANSSCRIPT, content);
        }
    }

    /**
     * Declares the interpreter to be used when invoking the RPM
     * pre-transaction script that can be set with the
     * {@link #setPreTrans(String)} method.
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
    public void setPreInstall(Path path) throws IOException {
        setPreInstall(readScript(path));
    }

    /**
     * Declares a script to be run as part of the RPM pre-installation. The
     * script will be run using the interpreter declared with the
     * {@link #setPreInstallProgram(String)} method.
     *
     * @param content Script contents to run (i.e. shell commands)
     */
    public void setPreInstall(String content) {
        setPreInstallProgram(readProgram(content));
        if (content != null && !content.isEmpty()) {
            format.getHeader().createEntry(HeaderTag.PREINSCRIPT, content);
        }
    }

    /**
     * Declares the interpreter to be used when invoking the RPM
     * pre-installation script that can be set with the
     * {@link #setPreInstall(String)} method.
     *
     * @param program Path to the interpretter
     */
    public void setPreInstallProgram(String program) {
        if (program == null || program.length() == 0) {
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
    public void setPostInstall(Path path) throws IOException {
        setPostInstall(readScript(path));
    }

    /**
     * Declares a script to be run as part of the RPM post-installation. The
     * script will be run using the interpreter declared with the
     * {@link #setPostInstallProgram(String)} method.
     *
     * @param script Script contents to run (i.e. shell commands)
     */
    public void setPostInstall(String script) {
        setPostInstallProgram(readProgram(script));
        if (script != null && !script.isEmpty()) {
            format.getHeader().createEntry(HeaderTag.POSTINSCRIPT, script);
        }
    }

    /**
     * Declares the interpreter to be used when invoking the RPM
     * post-installation script that can be set with the
     * {@link #setPostInstall(String)} method.
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
    public void setPreUninstall(Path path) throws IOException {
        setPreUninstall(readScript(path));
    }

    /**
     * Declares a script to be run as part of the RPM pre-uninstallation. The
     * script will be run using the interpreter declared with the
     * {@link #setPreUninstallProgram(String)} method.
     *
     * @param script Script contents to run (i.e. shell commands)
     */
    public void setPreUninstall(String script) {
        if (script != null && !script.isEmpty()) {
            setPreUninstallProgram(readProgram(script));
            format.getHeader().createEntry(HeaderTag.PREUNSCRIPT, script);
        }
    }

    /**
     * Declares the interpreter to be used when invoking the RPM
     * pre-uninstallation script that can be set with the
     * {@link #setPreUninstall(String)} method.
     *
     * @param program Path to the interpreter
     */
    public void setPreUninstallProgram(String program) {
        if (program == null || program.length() == 0) {
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
    public void setPostUninstall(Path path) throws IOException {
        setPostUninstall(readScript(path));
    }

    /**
     * Declares a script to be run as part of the RPM post-uninstallation. The
     * script will be run using the interpreter declared with the
     * {@link #setPostUninstallProgram(String)} method.
     *
     * @param content Script contents to run (i.e. shell commands)
     */
    public void setPostUninstall(String content) {
        setPostUninstallProgram(readProgram(content));
        if (content != null) {
            format.getHeader().createEntry(HeaderTag.POSTUNSCRIPT, content);
        }
    }

    /**
     * Declares the interpreter to be used when invoking the RPM
     * post-uninstallation script that can be set with the
     * {@link #setPostUninstall(String)} method.
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
    public void setPostTrans(Path path) throws IOException {
        setPostTrans(readScript(path));
    }

    /**
     * Declares a script to be run as part of the RPM post-transaction. The
     * script will be run using the interpreter declared with the
     * {@link #setPostTransProgram(String)} method.
     *
     * @param content Script contents to run (i.e. shell commands)
     */
    public void setPostTrans(String content) {
        setPostTransProgram(readProgram(content));
        if (content != null) {
            format.getHeader().createEntry(HeaderTag.POSTTRANSSCRIPT, content);
        }
    }

    /**
     * Declares the interpreter to be used when invoking the RPM
     * post-transaction script that can be set with the
     * {@link #setPostTrans(String)} method.
     *
     * @param program Path to the interpreter
     */
    public void setPostTransProgram(String program) {
        if (program == null || program.length() == 0) {
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
     * @param path       the absolute path at which this file will be installed.
     * @param source     the file to include in this rpm.
     * @param mode       the mode of the target file in standard three octet notation, or -1 for default.
     * @param dirmode    the mode of the parent directories in standard three octet notation, or -1 for default.
     * @param directive  directive indicating special handling for this file.
     * @param uname       user owner of the file
     * @param gname       group owner of the file
     * @param addParents whether to create parent directories for the file, defaults to true for other methods.
     * @throws IOException              there was an IO error
     */
    public void addFile(String path, Path source, int mode, int dirmode,
                        EnumSet<Directive> directive,
                        String uname, String gname, boolean addParents) throws IOException {
        addFile(path, source, mode, dirmode, directive, uname, gname, addParents, -1);
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
     * @param uname       user owner of the directory
     * @param gname       group owner of the directory
     * @param addParents  whether to create parent directories for the file, defaults to true for other methods.
     * @param verifyFlags verify flags
     * @throws IOException there was an IO error
     */
    public void addFile(String path, Path source, int mode, int dirmode,
                        EnumSet<Directive> directive,
                        String uname, String gname, boolean addParents, int verifyFlags) throws IOException {
        contents.addFile(path, source, mode, dirmode, directive, uname, gname, -1, -1, addParents, verifyFlags);
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
     */
    public void addDirectory(String path, int permissions, EnumSet<Directive> directives,
                             String uname, String gname,
                             boolean addParents) {
        contents.addDirectory(path, permissions, directives, uname, gname,-1, -1, addParents);
    }

    /**
     * Adds an URL (from a jar) to the repository.
     *
     * @param path the absolute path
     * @param source the URL source
     * @param permissions the file mode
     * @param dirmode the directory mode
     * @param directives directives indicating special handling for this file
     * @param uname user owner of the entry
     * @param gname group owner of the entry
     */
    public void addURL(String path, URL source, int permissions, int dirmode,
                       EnumSet<Directive> directives,
                       String uname, String gname) {
        contents.addURL(path, source, permissions, directives, uname, gname, -1, -1, dirmode);
    }

    /**
     * Adds a symbolic link to the repository.
     *
     * @param path        the absolute path at which this link will be installed.
     * @param target      the path of the file this link will point to.
     * @param permissions the permissions flags
     */
    public void addLink(String path, String target, int permissions) {
        addLink(path, target, permissions, null, null);
    }

    /**
     * Adds a symbolic link to the repository.
     *
     * @param path        the absolute path at which this link will be installed.
     * @param target      the path of the file this link will point to.
     * @param permissions the permissions flags
     * @param username    user owner of the link
     * @param groupname   group owner of the link
     */
    public void addLink(String path, String target, int permissions,
                        String username, String groupname) {
        contents.addLink(path, target, permissions, username, groupname, -1, -1, true);
    }

    /**
     * Adds the supplied Changelog path as a Changelog to the header.
     *
     * @param changelogContents File containing the Changelog information
     * @throws IOException             if file does not exist or cannot be read
     * @throws ChangelogParseException if file is not of the correct format.
     */
    public void addChangelog(String changelogContents) throws IOException, ChangelogParseException {
        if (changelogContents != null) {
            new ChangelogHandler(format.getHeader()).addChangeLog(changelogContents);
        }
    }

    /**
     * Adds the supplied Changelog path as a Changelog to the header.
     *
     * @param changelogFile File containing the Changelog information
     * @throws IOException             if file does not exist or cannot be read
     * @throws ChangelogParseException if file is not of the correct format.
     */
    public void addChangelog(Path changelogFile) throws IOException, ChangelogParseException {
        if (changelogFile != null) {
            new ChangelogHandler(format.getHeader()).addChangeLog(changelogFile);
        }
    }

    /**
     * Adds the supplied Changelog file as a Changelog to the header.
     *
     * @param changelogInputStream input stream containing the Changelog information
     * @throws IOException             if file does not exist or cannot be read
     * @throws ChangelogParseException if file is not of the correct format.
     */
    public void addChangelog(InputStream changelogInputStream) throws IOException, ChangelogParseException {
        if (changelogInputStream != null) {
            new ChangelogHandler(format.getHeader())
                    .addChangeLog(new InputStreamReader(changelogInputStream, StandardCharsets.UTF_8));
        }
    }

    /**
     * Sets the PGP key ring used for header and header + payload signature.
     *
     * @param privateKeyRing the private key ring input stream
     * @throws IOException             if file does not exist or cannot be read
     */
    public void setPrivateKeyRing(String privateKeyRing) throws IOException {
        if (privateKeyRing != null) {
            setPrivateKeyRing(Paths.get(privateKeyRing));
        }
    }

    /**
     * Sets the PGP key ring used for header and header + payload signature.
     *
     * @param privateKeyRing the private key ring input stream
     * @throws IOException             if file does not exist or cannot be read
     */
    public void setPrivateKeyRing(Path privateKeyRing) throws IOException {
        if (privateKeyRing != null && Files.exists(privateKeyRing)) {
            // will be closed in the SignatureGeneratur
            setPrivateKeyRing(Files.newInputStream(privateKeyRing));
        }
    }

    /**
     * Sets the PGP key ring used for header and header + payload signature.
     *
     * @param privateKeyRing the private key ring input stream
     */
    public void setPrivateKeyRing(InputStream privateKeyRing) {
        if (privateKeyRing != null) {
            this.privateKeyRing = privateKeyRing;
        }
    }

    /**
     * Selects a private key from the current {@link #setPrivateKeyRing(java.io.InputStream) private key ring}.
     * If no key is specified, the first signing key will be selected.
     *
     * @param privateKeyId long value from hex key id
     */
    public void setPrivateKeyId(String privateKeyId) {
        if (privateKeyId != null && !privateKeyId.isEmpty()) {
            setPrivateKeyId(Long.decode("0x" + privateKeyId));
        }
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
        if (privateKeyPassphrase != null && !privateKeyPassphrase.isEmpty()) {
            this.privateKeyPassphrase = privateKeyPassphrase;
        }
    }

    /**
     * Hash algo for the private key.
     *
     * @param privateKeyHashAlgo the private key hash algo
     */
    public void setPrivateKeyHashAlgo(String privateKeyHashAlgo) {
        if (privateKeyHashAlgo != null && !privateKeyHashAlgo.isEmpty()) {
            this.privateKeyHashAlgo = HashAlgo.valueOf(privateKeyHashAlgo);
        }
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
        format.getHeader().createEntry(HeaderTag.REQUIRENAME, getStringList(requires));
        format.getHeader().createEntry(HeaderTag.REQUIREVERSION, getVersions(requires));
        format.getHeader().createEntry(HeaderTag.REQUIREFLAGS, getFlags(requires));
        if (obsoletes.size() > 0) {
            format.getHeader().createEntry(HeaderTag.OBSOLETENAME, getStringList(obsoletes));
            format.getHeader().createEntry(HeaderTag.OBSOLETEVERSION, getVersions(obsoletes));
            format.getHeader().createEntry(HeaderTag.OBSOLETEFLAGS, getFlags(obsoletes));
        }
        if (conflicts.size() > 0) {
            format.getHeader().createEntry(HeaderTag.CONFLICTNAME, getStringList(conflicts));
            format.getHeader().createEntry(HeaderTag.CONFLICTVERSION, getVersions(conflicts));
            format.getHeader().createEntry(HeaderTag.CONFLICTFLAGS, getFlags(conflicts));
        }
        if (provides.size() > 0) {
            format.getHeader().createEntry(HeaderTag.PROVIDENAME, getStringList(provides));
            format.getHeader().createEntry(HeaderTag.PROVIDEVERSION, getVersions(provides));
            format.getHeader().createEntry(HeaderTag.PROVIDEFLAGS, getFlags(provides));
        }
        format.getHeader().createEntry(HeaderTag.SIZE, contents.getTotalSize());
        if (contents.size() > 0) {
            format.getHeader().createEntry(HeaderTag.DIRNAMES, contents.getDirNames());
            format.getHeader().createEntry(HeaderTag.DIRINDEXES, contents.getDirIndexes());
            format.getHeader().createEntry(HeaderTag.BASENAMES, contents.getBaseNames());
        }
        if (triggerCounter > 0) {
            format.getHeader().createEntry(HeaderTag.TRIGGERSCRIPTS,
                    triggerscripts.toArray(new String[0]));
            format.getHeader().createEntry(HeaderTag.TRIGGERNAME,
                    triggernames.toArray(new String[0]));
            format.getHeader().createEntry(HeaderTag.TRIGGERVERSION,
                    triggerversions.toArray(new String[0]));
            format.getHeader().createEntry(HeaderTag.TRIGGERFLAGS,
                    triggerflags.toArray(new Integer[0]));
            format.getHeader().createEntry(HeaderTag.TRIGGERINDEX,
                    triggerindexes.toArray(new Integer[0]));
            format.getHeader().createEntry(HeaderTag.TRIGGERSCRIPTPROG,
                    triggerscriptprogs.toArray(new String[0]));
        }
        if (contents.size() > 0) {
            format.getHeader().createEntry(HeaderTag.FILEDIGESTALGOS, HashAlgo.MD5.num());
            format.getHeader().createEntry(HeaderTag.FILEDIGESTS, contents.getDigests(HashAlgo.MD5));
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
        SpecEntry<IntegerList> sigsize =
                (SpecEntry<IntegerList>) format.getSignatureHeader().addEntry(SignatureTag.LEGACY_SIGSIZE, 1);
        SpecEntry<IntegerList> signaturHeaderPayloadEntry =
                (SpecEntry<IntegerList>) format.getSignatureHeader().addEntry(SignatureTag.PAYLOADSIZE, 1);
        SpecEntry<byte[]> md5Entry =
                (SpecEntry<byte[]>) format.getSignatureHeader().addEntry(SignatureTag.LEGACY_MD5, 16);
        SpecEntry<StringList> shaEntry =
                (SpecEntry<StringList>) format.getSignatureHeader().addEntry(SignatureTag.SHA1HEADER, 1);
        shaEntry.setSize(SHASIZE);
        SignatureGenerator signatureGenerator = new SignatureGenerator(privateKeyRing, privateKeyId, privateKeyPassphrase);
        signatureGenerator.prepare(format.getSignatureHeader(), privateKeyHashAlgo);
        format.getLead().write(channel);
        SpecEntry<byte[]> signatureEntry =
                (SpecEntry<byte[]>) format.getSignatureHeader().addEntry(SignatureTag.SIGNATURES, 16);
        signatureEntry.setValues(createHeaderIndex(HeaderTag.SIGNATURES.getCode(), format.getSignatureHeader().count()));
        ChannelWrapper.empty(output, ByteBuffer.allocate(format.getSignatureHeader().write(channel)));
        ChannelWrapper.Key<Integer> sigsizekey = output.start();
        ChannelWrapper.Key<byte[]> shakey = signatureGenerator.startDigest(output, "SHA");
        ChannelWrapper.Key<byte[]> md5key = signatureGenerator.startDigest(output, "MD5");
        signatureGenerator.startBeforeHeader(output, privateKeyHashAlgo);
        // Region concept. This tag contains an index record which specifies the portion of the Header Record
        // which was used for the calculation of a signature. This data shall be preserved or any header-only signature
        // will be invalidated.
        SpecEntry<byte[]> immutable =
                (SpecEntry<byte[]>) format.getHeader().addEntry(HeaderTag.HEADERIMMUTABLE, 16);
        immutable.setValues(createHeaderIndex(HeaderTag.IMMUTABLE.getCode(), format.getHeader().count()));
        format.getHeader().write(output);
        shaEntry.setValues(StringList.of(hex(output.finish(shakey))));
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
                    while (readableByteChannel.read(buffer.rewind()) > 0) {
                        total += compressedOutput.write(buffer.flip());
                        buffer.compact();
                    }
                    total += header.skip(compressedOutput, total);
                }
            } else if (object instanceof InputStream) {
                try (ReadableByteChannel in = Channels.newChannel(((InputStream) object))) {
                    while (in.read(buffer.rewind()) > 0) {
                        total += compressedOutput.write(buffer.flip());
                        buffer.compact();
                    }
                    total += header.skip(compressedOutput, total);
                }
            } else if (object instanceof URL) {
                try (ReadableByteChannel in = Channels.newChannel(((URL) object).openConnection().getInputStream())) {
                    while (in.read(buffer.rewind()) > 0) {
                        total += compressedOutput.write(buffer.flip());
                        buffer.compact();
                    }
                    total += header.skip(compressedOutput, total);
                }
            } else if (object != null) {
                String string = object.toString();
                total += compressedOutput.write(ByteBuffer.wrap(string.getBytes(StandardCharsets.UTF_8)));
                total += header.skip(compressedOutput, string.length());
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
        signaturHeaderPayloadEntry.setValues(IntegerList.of(length));
        // flush compressed stream here
        compressedOutputStream.flush();
        md5Entry.setValues(output.finish(md5key));
        sigsize.setValues(IntegerList.of(output.finish(sigsizekey)));
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
     * Returns an list of String with the name of every dependency from a list of dependencies.
     *
     * @param dependencyList List of dependencies
     * @return list of strings with all names of the dependencies
     */
    private static StringList getStringList(List<Dependency> dependencyList) {
        StringList list = new StringList();
        for (Dependency dependency : dependencyList) {
            list.add(dependency.getPackageName());
        }
        return list;
    }

    /**
     * Returns an list of String with the version of every dependency from a list of dependencies.
     *
     * @param dependencyList List of dependencies
     * @return list of strings with all versions of the dependencies
     */
    private static StringList getVersions(List<Dependency> dependencyList) {
        StringList versionList = new StringList();
        for (Dependency dependency : dependencyList) {
            versionList.add(dependency.getVersion());
        }
        return versionList;
    }

    /**
     * Returns an list of Integer with the flags of every dependency from a list of dependencies.
     *
     * @param dependencyList List of dependencies
     * @return IntegerList with all flags of the dependencies
     */
    private static IntegerList getFlags(List<Dependency> dependencyList) {
        IntegerList flagsList = new IntegerList();
        for (Dependency dependency : dependencyList) {
            flagsList.add(dependency.getFlags());
        }
        return flagsList;
    }

    /**
     * Returns an list of String with the name of every dependency from a list of dependencies.
     *
     * @param dependencies List of dependencies
     * @return list of strings with all names of the dependencies
     */
    private static StringList getStringList(Map<String, Dependency> dependencies) {
        StringList nameList = new StringList();
        for (Dependency dependency : dependencies.values()) {
            nameList.add(dependency.getPackageName());
        }
        return nameList;
    }

    /**
     * Returns an list of String with the version of every dependency from a list of dependencies.
     *
     * @param dependencies List of dependencies
     * @return list of strings with all versions of the dependencies
     */
    private static StringList getVersions(Map<String, Dependency> dependencies) {
        StringList versionList = new StringList();
        for (Dependency dependency : dependencies.values()) {
            versionList.add(dependency.getVersion());
        }
        return versionList;
    }

    /**
     * Returns an list of Integer with the flags of every dependency from a list of dependencies.
     *
     * @param dependencies List of dependencies
     * @return IntegerList with all flags of the dependencies
     */
    private static IntegerList getFlags(Map<String, Dependency> dependencies) {
        IntegerList flagsList = new IntegerList();
        for (Dependency dependency : dependencies.values()) {
            flagsList.add(dependency.getFlags());
        }
        return flagsList;
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


}
