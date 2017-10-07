package org.xbib.rpm.payload;

import org.xbib.rpm.exception.RpmException;
import org.xbib.rpm.io.ChannelWrapper;
import org.xbib.rpm.io.ChannelWrapper.Key;
import org.xbib.rpm.io.ReadableChannelWrapper;
import org.xbib.rpm.security.HashAlgo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The contents of an RPM archive. These entries define the files and links that
 * the RPM archive contains as well as headers those files require.
 * Note that the RPM format requires that files in the archive be naturally ordered.
 */
public class Contents {

    private static final Logger logger = Logger.getLogger(Contents.class.getName());

    private static final Set<String> BUILTIN = new LinkedHashSet<>();

    private static final String hex = "0123456789abcdef";

    static {
        BUILTIN.add("/");
        BUILTIN.add("/bin");
        BUILTIN.add("/dev");
        BUILTIN.add("/etc");
        BUILTIN.add("/etc/bash_completion.d");
        BUILTIN.add("/etc/cron.d");
        BUILTIN.add("/etc/cron.daily");
        BUILTIN.add("/etc/cron.hourly");
        BUILTIN.add("/etc/cron.monthly");
        BUILTIN.add("/etc/cron.weekly");
        BUILTIN.add("/etc/default");
        BUILTIN.add("/etc/init.d");
        BUILTIN.add("/etc/logrotate.d");
        BUILTIN.add("/lib");
        BUILTIN.add("/usr");
        BUILTIN.add("/usr/bin");
        BUILTIN.add("/usr/lib");
        BUILTIN.add("/usr/lib64");
        BUILTIN.add("/usr/local");
        BUILTIN.add("/usr/local/bin");
        BUILTIN.add("/usr/local/lib");
        BUILTIN.add("/usr/sbin");
        BUILTIN.add("/usr/share");
        BUILTIN.add("/usr/share/applications");
        BUILTIN.add("/root");
        BUILTIN.add("/sbin");
        BUILTIN.add("/opt");
        BUILTIN.add("/srv");
        BUILTIN.add("/tmp");
        BUILTIN.add("/var");
        BUILTIN.add("/var/cache");
        BUILTIN.add("/var/lib");
        BUILTIN.add("/var/log");
        BUILTIN.add("/var/run");
        BUILTIN.add("/var/spool");
        /*
        DOC_DIRS.add("/usr/doc");
        DOC_DIRS.add("/usr/man");
        DOC_DIRS.add("/usr/X11R6/man");
        DOC_DIRS.add("/usr/share/doc");
        DOC_DIRS.add("/usr/share/man");
        DOC_DIRS.add("/usr/share/info");
        */
    }

    private final Set<CpioHeader> headers =
            new TreeSet<>(Comparator.comparing(CpioHeader::getName));

    private final Set<String> files = new LinkedHashSet<>();

    private final Map<CpioHeader, Object> sources = new LinkedHashMap<>();

    private final Set<String> builtins = new LinkedHashSet<>();

    private int inode = 1;

    public Contents() {
        builtins.addAll(BUILTIN);
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(hex.charAt(((int) aByte & 0xf0) >> 4)).append(hex.charAt((int) aByte & 0x0f));
        }
        return sb.toString();
    }

    /**
     * Adds a directory entry to the archive with the default permissions of 644.
     *
     * @param path   the destination path for the installed file.
     * @param target the target string
     */
    public void addLink(String path, String target) {
        addLink(path, target, -1);
    }

    /**
     * Adds a directory entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param target      the target string
     * @param permissions the permissions flags.
     */
    public void addLink(String path, String target, int permissions) {
        addLink(path, target, permissions, null, null);
    }

    /**
     * Adds a directory entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param target      the target string
     * @param permissions the permissions flags.
     * @param uname       user owner for the given link
     * @param gname       group owner for the given link
     */
    public void addLink(String path, String target, int permissions, String uname, String gname) {
        if (files.contains(path)) {
            return;
        }
        files.add(path);
        logger.log(Level.FINE, "adding link ''{0}''.", path);
        CpioHeader header = new CpioHeader(path);
        header.setType(CpioHeader.SYMLINK);
        header.setFileSize(target.length());
        header.setMtime(System.currentTimeMillis());
        header.setUname(getDefaultIfMissing(uname, CpioHeader.DEFAULT_USERNAME));
        header.setGname(getDefaultIfMissing(gname, CpioHeader.DEFAULT_GROUP));
        if (permissions != -1) {
            header.setPermissions(permissions);
        }
        headers.add(header);
        sources.put(header, target);
    }

    private String getDefaultIfMissing(String value, String defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    /**
     * Adds a directory entry to the archive with the default permissions of 644.
     *
     * @param path the destination path for the installed file.
     */
    public void addDirectory(String path) {
        addDirectory(path, -1);
    }

    /**
     * Adds a directory entry to the archive with the default permissions of 644.
     *
     * @param path      the destination path for the installed file.
     * @param directive directive indicating special handling for this directory.
     */
    public void addDirectory(String path, EnumSet<Directive> directive) {
        addDirectory(path, -1, directive, null, null);
    }

    /**
     * Adds a directory entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param permissions the permissions flags.
     */
    public void addDirectory(String path, int permissions) {
        addDirectory(path, permissions, null, null, null);
    }

    /**
     * Adds a directory entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param permissions the permissions flags.
     * @param directive   directive indicating special handling for this directory.
     * @param uname       user owner for the given file
     * @param gname       group owner for the given file
     */
    public void addDirectory(String path, int permissions, EnumSet<Directive> directive, String uname, String gname) {
        addDirectory(path, permissions, directive, uname, gname, true);
    }

    /**
     * Adds a directory entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param permissions the permissions flags.
     * @param directive   directive indicating special handling for this directory.
     * @param uname       user owner for the given file
     * @param gname       group owner for the given file
     * @param addParents  whether to add parent directories to the rpm
     */
    public void addDirectory(String path, int permissions, EnumSet<Directive> directive, String uname,
                             String gname, boolean addParents) {
        if (files.contains(path)) {
            return;
        }
        if (addParents) {
            addParents(Paths.get(path), permissions, uname, gname);
        }
        files.add(path);
        logger.log(Level.FINE, "adding directory ''{0}''.", path);
        CpioHeader header = new CpioHeader(path);
        header.setType(CpioHeader.DIR);
        header.setInode(inode++);
        if (uname == null) {
            header.setUname(CpioHeader.DEFAULT_USERNAME);
        } else if (0 == uname.length()) {
            header.setUname(CpioHeader.DEFAULT_USERNAME);
        } else {
            header.setUname(uname);
        }
        if (gname == null) {
            header.setGname(CpioHeader.DEFAULT_GROUP);
        } else if (0 == gname.length()) {
            header.setGname(CpioHeader.DEFAULT_GROUP);
        } else {
            header.setGname(gname);
        }
        header.setMtime(System.currentTimeMillis());
        if (-1 == permissions) {
            header.setPermissions(CpioHeader.DEFAULT_DIRECTORY_PERMISSION);
        } else {
            header.setPermissions(permissions);
        }
        headers.add(header);
        sources.put(header, "");
        if (directive != null) {
            int flag = Directive.NONE.flag();
            for (Directive d : directive) {
                flag = flag | d.flag();
            }
            header.setFlags(flag);
        }
    }

    /**
     * Adds a file entry to the archive with the default permissions of 644.
     *
     * @param path   the destination path for the installed file.
     * @param source the local file to be included in the package.
     * @throws IOException file wasn't found
     */
    public void addFile(String path, Path source) throws IOException {
        addFile(path, source, -1);
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param source      the local file to be included in the package.
     * @param permissions the permissions flags.
     * @throws IOException file wasn't found
     */
    public void addFile(String path, Path source, int permissions) throws IOException {
        addFile(path, source, permissions, null, null, null);
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param source      the local file to be included in the package.
     * @param permissions the permissions flags.
     * @param dirmode     permission flags for parent directories, use -1 to leave as default.
     * @throws IOException file wasn't found
     */
    public void addFile(String path, Path source, int permissions, int dirmode) throws IOException {
        addFile(path, source, permissions, null, null, null, dirmode);
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param source      the local file to be included in the package.
     * @param permissions the permissions flags.
     * @param directive   directive indicating special handling for this file.
     * @throws IOException file wasn't found
     */
    public void addFile(String path, Path source, int permissions, EnumSet<Directive> directive)
            throws IOException {
        addFile(path, source, permissions, directive, null, null);
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param source      the local file to be included in the package.
     * @param permissions the permissions flags.
     * @param directive   directive indicating special handling for this file.
     * @param uname       user owner for the given file
     * @param gname       group owner for the given file
     * @throws IOException file wasn't found
     */
    public void addFile(String path, Path source, int permissions, EnumSet<Directive> directive, String uname,
                        String gname) throws IOException {
        addFile(path, source, permissions, directive, uname, gname, -1);
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param source      the local file to be included in the package.
     * @param permissions the permissions flags.
     * @param directives  directives indicating special handling for this file.
     * @param uname       user owner for the given file
     * @param gname       group owner for the given file
     * @param dirmode     permission flags for parent directories, use -1 to leave as default.
     * @throws IOException file wasn't found
     */
    public void addFile(String path, Path source, int permissions, EnumSet<Directive> directives, String uname,
                        String gname, int dirmode) throws IOException {
        addFile(path, source, permissions, directives, uname, gname, dirmode, true);
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param source      the local file to be included in the package.
     * @param permissions the permissions flags, use -1 to leave as default.
     * @param directives  directives indicating special handling for this file, use null to ignore.
     * @param uname       user owner for the given file, use null for default user.
     * @param gname       group owner for the given file, use null for default group.
     * @param dirmode     permission flags for parent directories, use -1 to leave as default.
     * @param addParents  whether to create parent directories for the file, defaults to true for other methods.
     * @throws IOException file wasn't found
     */
    public void addFile(String path, Path source, int permissions, EnumSet<Directive> directives, String uname,
                        String gname, int dirmode, boolean addParents) throws IOException {
        addFile(path, source, permissions, directives, uname, gname, dirmode, addParents, -1);
    }

    /**
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param source      the local file to be included in the package.
     * @param permissions the permissions flags, use -1 to leave as default.
     * @param directives  directives indicating special handling for this file, use null to ignore.
     * @param uname       user owner for the given file, use null for default user.
     * @param gname       group owner for the given file, use null for default group.
     * @param dirmode     permission flags for parent directories, use -1 to leave as default.
     * @param addParents  whether to create parent directories for the file, defaults to true for other methods.
     * @param verifyFlags verify flags
     * @throws java.io.FileNotFoundException file wasn't found
     */
    public void addFile(String path, Path source, int permissions, EnumSet<Directive> directives, String uname,
                        String gname, int dirmode, boolean addParents, int verifyFlags) throws IOException {
        if (files.contains(path)) {
            return;
        }
        if (addParents) {
            addParents(Paths.get(path), dirmode, uname, gname);
        }
        files.add(path);
        logger.log(Level.FINE, "adding file ''{0}''.", path);
        CpioHeader header;
        if (directives != null && directives.contains(Directive.GHOST)) {
            header = new CpioHeader(path);
        } else {
            header = new CpioHeader(path, source);
        }
        header.setType(CpioHeader.FILE);
        header.setInode(inode++);
        if (uname == null) {
            header.setUname(CpioHeader.DEFAULT_USERNAME);
        } else if (0 == uname.length()) {
            header.setUname(CpioHeader.DEFAULT_USERNAME);
        } else {
            header.setUname(uname);
        }
        if (gname == null) {
            header.setGname(CpioHeader.DEFAULT_GROUP);
        } else if (0 == gname.length()) {
            header.setGname(CpioHeader.DEFAULT_GROUP);
        } else {
            header.setGname(gname);
        }
        if (-1 == permissions) {
            header.setPermissions(CpioHeader.DEFAULT_FILE_PERMISSION);
        } else {
            header.setPermissions(permissions);
        }
        header.setVerifyFlags(verifyFlags);
        headers.add(header);
        sources.put(header, source);
        if (directives != null) {
            int flag = Directive.NONE.flag();
            for (Directive d : directives) {
                flag = flag | d.flag();
            }
            header.setFlags(flag);
        }
    }

    /**
     * Adds a URL entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param source      the URL with the data to be added
     * @param permissions the permissions flags.
     * @param directive   directive indicating special handling for this file.
     * @param uname       user owner for the given file
     * @param gname       group owner for the given file
     * @param dirmode     permission flags for parent directories, use -1 to leave as default.
     * @throws IOException file wasn't found
     */
    public void addURL(String path, URL source, int permissions, EnumSet<Directive> directive, String uname,
                       String gname, int dirmode) throws IOException {
        if (files.contains(path)) {
            return;
        }
        addParents(Paths.get(path), dirmode, uname, gname);
        files.add(path);
        logger.log(Level.FINE, "adding file ''{0}''.", path);
        CpioHeader header = new CpioHeader(path, source);
        header.setType(CpioHeader.FILE);
        header.setInode(inode++);
        if (uname != null) {
            header.setUname(uname);
        }
        if (gname != null) {
            header.setGname(gname);
        }
        if (permissions != -1) {
            header.setPermissions(permissions);
        }
        headers.add(header);
        sources.put(header, source);
        if (directive != null) {
            int flag = Directive.NONE.flag();
            for (Directive d : directive) {
                flag = flag | d.flag();
            }
            header.setFlags(flag);
        }
    }

    /**
     * Adds entries for parent directories of this file, so that they may be cleaned up when
     * removing the package.
     *
     * @param path        the file to add parent directories of
     * @param permissions the permissions flags
     * @param uname       user owner for the given file
     * @param gname       group owner for the given file
     */
    private void addParents(Path path, int permissions, String uname, String gname) {
        List<String> parents = new ArrayList<>();
        listParents(parents, path);
        for (String parent : parents) {
            addDirectory(parent, permissions, null, uname, gname);
        }
    }

    /**
     * Add additional directory that is assumed to already exist on system where the RPM will be installed
     * (e.g. /etc) and should not have an entry in the RPM.
     * <p>
     * The builtin will only be added to this instance of Contents.
     *
     * @param directory the directory to add
     */
    public void addLocalBuiltinDirectory(String directory) {
        builtins.add(directory);
    }

    /**
     * Retrieve the size of this archive in number of files. This count includes both directory entries and
     * soft links.
     *
     * @return the number of files in this archive
     */
    public int size() {
        return headers.size();
    }

    /**
     * Retrieve the archive headers. The returned {@link Iterable} will iterate in the correct order for
     * the archive.
     *
     * @return the headers
     */
    public Iterable<CpioHeader> headers() {
        return headers;
    }

    /**
     * Retrieves the content for this archive entry, which may be a {@link Path} if the entry is a regular file or
     * a {@link CharSequence} containing the name of the target path if the entry is a link. This is the value to
     * be written to the archive as the body of the entry.
     *
     * @param header the header to get the content from
     * @return the content
     */
    public Object getSource(CpioHeader header) {
        return sources.get(header);
    }

    /**
     * Accumulated size of all files included in the archive.
     *
     * @return the size of all files included in the archive
     */
    public int getTotalSize() {
        int total = 0;
        try {
            for (Object object : sources.values()) {
                if (object instanceof Path) {
                    total += Files.size((Path) object);
                } else if (object instanceof URL) {
                    URLConnection urlConnection = null;
                    try {
                        urlConnection = ((URL) object).openConnection();
                        total += urlConnection.getContentLength();
                    } catch (IOException e) {
                        //
                    } finally {
                        if (urlConnection != null && urlConnection.getInputStream() != null) {
                            urlConnection.getInputStream().close();
                        }
                        if (urlConnection != null && urlConnection.getOutputStream() != null) {
                            urlConnection.getOutputStream().close();
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return total;
    }

    /**
     * Gets the dirnames headers values.
     *
     * @return the dirnames headers values
     */
    public String[] getDirNames() {
        Set<String> set = new LinkedHashSet<>();
        for (CpioHeader header : headers) {
            Path path = Paths.get(header.getName()).getParent();
            if (path == null) {
                continue;
            }
            String parent = CpioHeader.normalizePath(path.toString());
            if (!parent.endsWith("/")) {
                parent += "/";
            }
            set.add(parent);
        }
        return set.toArray(new String[set.size()]);
    }

    /**
     * Gets the dirindexes headers values.
     *
     * @return the dirindexes
     */
    public Integer[] getDirIndexes() {
        List<String> dirs = Arrays.asList(getDirNames());
        Integer[] array = new Integer[headers.size()];
        int x = 0;
        for (CpioHeader header : headers) {
            Path path = Paths.get(header.getName()).getParent();
            if (path == null) {
                array[x++] = 0; // dummy value, required when including non-existent directories
                continue;
            }
            String parent = CpioHeader.normalizePath(path.toString());
            if (!parent.endsWith("/")) {
                parent += "/";
            }
            array[x++] = dirs.indexOf(parent);
        }
        return array;
    }

    /**
     * Gets the basenames header values.
     *
     * @return the basename header values
     */
    public String[] getBaseNames() {
        String[] array = new String[headers.size()];
        int x = 0;
        for (CpioHeader header : headers) {
            Path path = Paths.get(header.getName()).getFileName();
            array[x++] = path != null ? CpioHeader.normalizePath(path.toString()) : "";
        }
        return array;
    }

    /**
     * Gets the sizes header values.
     *
     * @return the sizes header values
     */
    public Integer[] getSizes() {
        Integer[] array = new Integer[headers.size()];
        int x = 0;
        try {
            for (CpioHeader header : headers) {
                Object object = sources.get(header);
                if (object instanceof Path) {
                    array[x] = (int) Files.size((Path) object);
                } else if (object instanceof URL) {
                    array[x] = ((URL) object).openConnection().getContentLength();
                } else if (header.getType() == CpioHeader.DIR) {
                    array[x] = 4096;
                } else if (header.getType() == CpioHeader.SYMLINK) {
                    array[x] = ((String) object).length();
                }
                ++x;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return array;
    }

    /**
     * Gets the modes header values.
     *
     * @return the modes header values
     */
    public short[] getModes() {
        short[] array = new short[headers.size()];
        int x = 0;
        for (CpioHeader header : headers) {
            array[x++] = (short) header.getMode();
        }
        return array;
    }

    /**
     * Gets the rdevs header values.
     *
     * @return the rdevs header values
     */
    public short[] getRdevs() {
        short[] array = new short[headers.size()];
        int x = 0;
        for (CpioHeader header : headers) {
            array[x++] = (short) ((header.getRdevMajor() << 8) + header.getRdevMinor());
        }
        return array;
    }

    /**
     * Gets the mtimes header values.
     *
     * @return the mtimes header values
     */
    public Integer[] getMtimes() {
        Integer[] array = new Integer[headers.size()];
        int x = 0;
        for (CpioHeader header : headers) {
            array[x++] = header.getMtime();
        }
        return array;
    }

    /**
     * Caclulates a digest hash for each file in the archive.
     *
     * @param hashAlgo the hash algo
     * @return the digest hashes
     * @throws RpmException if the algorithm isn't supported
     * @throws IOException there was an IO error
     */
    public String[] getDigests(HashAlgo hashAlgo) throws IOException, RpmException {
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        String[] array = new String[headers.size()];
        int x = 0;
        for (CpioHeader header : headers) {
            Object object = sources.get(header);
            String value = "";
            if (object instanceof Path) {
                try (ReadableByteChannel readableByteChannel = FileChannel.open((Path) object)) {
                    try (ReadableChannelWrapper input = new ReadableChannelWrapper(readableByteChannel)) {
                        Key<byte[]> key = startDigest(input, MessageDigest.getInstance(hashAlgo.algo()));
                        while (input.read(buffer) != -1) {
                            buffer.rewind();
                        }
                        value = hex(input.finish(key));
                    } catch (NoSuchAlgorithmException e) {
                        throw new RpmException(e);
                    }
                }
            } else if (object instanceof URL) {
                URL url = (URL) object;
                try (InputStream inputStream = url.openStream()) {
                    try (ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream)) {
                        try (ReadableChannelWrapper input = new ReadableChannelWrapper(readableByteChannel)) {
                            Key<byte[]> key = startDigest(input, MessageDigest.getInstance(hashAlgo.algo()));
                            while (input.read(buffer) != -1) {
                                buffer.rewind();
                            }
                            value = hex(input.finish(key));
                        }
                    } catch (NoSuchAlgorithmException e) {
                        throw new RpmException(e);
                    }
                }
            }
            array[x++] = value;
        }
        return array;
    }

    /**
     * Start a digest.
     *
     * @param input the input channel
     * @return reference to the new key added to the consumers
     */
    private ChannelWrapper.Key<byte[]> startDigest(ReadableChannelWrapper input, MessageDigest digest) {
        ChannelWrapper.Consumer<byte[]> consumer = new ChannelWrapper.Consumer<byte[]>() {
            @Override
            public void consume(ByteBuffer buffer) {
                try {
                    digest.update(buffer);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public byte[] finish() {
                try {
                    return digest.digest();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return input.start(consumer);
    }

    /**
     * Get the linktos header.
     *
     * @return the linktos header
     */
    public String[] getLinkTos() {
        String[] array = new String[headers.size()];
        int x = 0;
        for (CpioHeader header : headers) {
            Object object = sources.get(header);
            String value = "";
            if (object instanceof String) {
                value = String.valueOf(object);
            }
            array[x++] = value;
        }
        return array;
    }

    /**
     * Gets the flags header values.
     *
     * @return the flags header values
     */
    public Integer[] getFlags() {
        Integer[] array = new Integer[headers.size()];
        int x = 0;
        for (CpioHeader header : headers) {
            array[x++] = header.getFlags();
        }
        return array;
    }

    /**
     * Gets the users header values.
     *
     * @return the users header values
     */
    public String[] getUsers() {
        String[] array = new String[headers.size()];
        int x = 0;
        for (CpioHeader header : headers) {
            array[x++] = header.getUname() == null ? "root" : header.getUname();
        }
        return array;
    }

    /**
     * Gets the groups header values.
     *
     * @return the groups header values
     */
    public String[] getGroups() {
        String[] array = new String[headers.size()];
        int x = 0;
        for (CpioHeader header : headers) {
            array[x++] = header.getGname() == null ? "root" : header.getGname();
        }
        return array;
    }

    /**
     * Gets the colors header values.
     *
     * @return the colors header values
     */
    public Integer[] getColors() {
        return new Integer[headers.size()];
    }

    /**
     * Gets the verifyflags header values.
     *
     * @return the verifyflags header values
     */
    public Integer[] getVerifyFlags() {
        Integer[] array = new Integer[headers.size()];
        int x = 0;
        for (CpioHeader header : headers) {
            array[x++] = header.getVerifyFlags();
        }
        return array;
    }

    /**
     * Gets the classes header values.
     *
     * @return the classes header values
     */
    public Integer[] getClasses() {
        Integer[] array = new Integer[headers.size()];
        Arrays.fill(array, 1);
        return array;
    }

    /**
     * Gets the devices header values.
     *
     * @return the devices header values
     */
    public Integer[] getDevices() {
        Integer[] array = new Integer[headers.size()];
        int x = 0;
        for (CpioHeader header : headers) {
            array[x++] = (header.getDevMajor() << 8) + header.getDevMinor();
        }
        return array;
    }

    /**
     * Gets the inodes header values.
     *
     * @return the iNodes header values
     */
    public Integer[] getInodes() {
        Integer[] array = new Integer[headers.size()];
        int x = 0;
        for (CpioHeader header : headers) {
            array[x++] = header.getInode();
        }
        return array;
    }

    /**
     * Gets the langs header values.
     *
     * @return the langs header values
     */
    public String[] getLangs() {
        String[] array = new String[headers.size()];
        Arrays.fill(array, "");
        return array;
    }

    /**
     * Gets the dependsx header values.
     *
     * @return the dependsx header values
     */
    public Integer[] getDependsX() {
        return new Integer[headers.size()];
    }

    /**
     * Gets the dependsn header values.
     *
     * @return the dependsn header values
     */
    public Integer[] getDependsN() {
        return new Integer[headers.size()];
    }

    /**
     * Gets the contexts header values.
     *
     * @return the contexts header values
     */
    public String[] getContexts() {
        String[] array = new String[headers.size()];
        Arrays.fill(array, "<<none>>");
        return array;
    }

    /**
     * Generates a list of parent paths given a starting path.
     *
     * @param parents the list to add the parents to
     * @param path    the file to search for parents of
     */
    protected void listParents(List<String> parents, Path path) {
        Path parent = path.getParent();
        if (parent == null) {
            return;
        }
        String s = CpioHeader.normalizePath(parent.toString());
        if (builtins.contains(s)) {
            return;
        }
        parents.add(s);
        listParents(parents, parent);
    }
}
