package org.xbib.rpm.payload;

import org.xbib.rpm.exception.RpmException;
import org.xbib.rpm.header.IntegerList;
import org.xbib.rpm.header.ShortList;
import org.xbib.rpm.header.StringList;
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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The contents of an RPM archive. These entries define the files and links that
 * the RPM archive contains as well as headers those files require.
 * Note that the RPM format requires that files in the archive be naturally ordered.
 */
public class Contents {

    private static final String DEFAULT_USERNAME = "root";

    private static final int DEFAULT_UID = 0;

    private static final String DEFAULT_GROUP = "root";

    private static final int DEFAULT_GID = 0;

    private static final int DEFAULT_FILE_PERMISSION = 0644;

    private static final int DEFAULT_DIRECTORY_PERMISSION = 0755;

    private static final int DEFAULT_LINK_PERMISSION = 0755;

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
    }

    private final Set<CpioHeader> headers =
            new TreeSet<>(Comparator.comparing(CpioHeader::getName));

    private final Set<String> files = new LinkedHashSet<>();

    private final Map<CpioHeader, Object> sources = new LinkedHashMap<>();

    private final Map<CpioHeader, UserGroup> usergroups = new LinkedHashMap<>();

    private final Set<String> builtins = new LinkedHashSet<>();

    private int inode = 1;

    public Contents() {
        builtins.addAll(BUILTIN);
    }

    /**
     * Adds a link to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param target      the target string
     * @param permissions the permissions flags.
     * @param uname       user owner for the given link
     * @param gname       group owner for the given link
     * @param uid the user id or -1 for the default value
     * @param gid the group id or -1 for the default value
     * @param addParents whether to add parent directories
     */
    public void addLink(String path, String target, int permissions,
                        String uname, String gname, int uid, int gid, boolean addParents) {
        if (files.contains(path)) {
            return;
        }
        if (addParents) {
            addParents(Paths.get(path), permissions, uname, gname, uid, gid);
        }
        files.add(path);
        CpioHeader header = new CpioHeader(path);
        header.setType(CpioHeader.SYMLINK);
        header.setFileSize(target.length());
        header.setMtime(System.currentTimeMillis());
        header.setUid(uid == -1 ? DEFAULT_UID : uid);
        header.setGid(gid == -1 ? DEFAULT_GID : gid);
        header.setPermissions(permissions == -1 ? DEFAULT_LINK_PERMISSION : permissions);
        headers.add(header);
        UserGroup userGroup = new UserGroup();
        userGroup.user = getDefaultIfMissing(uname, DEFAULT_USERNAME);
        userGroup.group = getDefaultIfMissing(gname, DEFAULT_GROUP);
        usergroups.put(header, userGroup);
        sources.put(header, target);
    }

    /**
     * Adds a directory entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param permissions the permissions flags.
     * @param directive   directive indicating special handling for this directory.
     * @param uname       user owner of the directory
     * @param gname       group owner of the directory
     * @param uid       user owner for the given file
     * @param gid       group owner for the given file
     * @param addParents  whether to add parent directories
     */
    public void addDirectory(String path, int permissions, EnumSet<Directive> directive,
                             String uname, String gname, int uid, int gid, boolean addParents) {
        if (files.contains(path)) {
            return;
        }
        if (addParents) {
            addParents(Paths.get(path), permissions, uname, gname, uid, gid);
        }
        files.add(path);
        CpioHeader header = new CpioHeader(path);
        header.setType(CpioHeader.DIR);
        header.setInode(inode++);
        header.setUid(uid == -1 ? DEFAULT_UID : uid);
        header.setGid(gid == -1 ? DEFAULT_GID : gid);
        header.setMtime(System.currentTimeMillis());
        header.setPermissions(permissions == -1 ? DEFAULT_DIRECTORY_PERMISSION : permissions);
        headers.add(header);
        UserGroup userGroup = new UserGroup();
        userGroup.user = getDefaultIfMissing(uname, DEFAULT_USERNAME);
        userGroup.group = getDefaultIfMissing(gname, DEFAULT_GROUP);
        usergroups.put(header, userGroup);
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
     * Adds a file entry to the archive with the specified permissions.
     *
     * @param path        the destination path for the installed file.
     * @param source      the local file to be included in the package.
     * @param permissions the permissions flags, use -1 to leave as default.
     * @param directives  directives indicating special handling for this file, use null to ignore.
     * @param uname       user owner of the file
     * @param gname       group owner of the file
     * @param uid       user owner for the given file, use null for default user.
     * @param gid       group owner for the given file, use null for default group.
     * @param dirmode     permission flags for parent directories, use -1 to leave as default.
     * @param addParents  whether to create parent directories for the file, defaults to true for other methods.
     * @param verifyFlags verify flags
     * @throws java.io.FileNotFoundException file wasn't found
     */
    public void addFile(String path, Path source, int permissions, int dirmode,
                        EnumSet<Directive> directives,
                        String uname, String gname, int uid, int gid,
                        boolean addParents, int verifyFlags) throws IOException {
        if (files.contains(path)) {
            return;
        }
        if (addParents) {
            addParents(Paths.get(path), dirmode, uname, gname, uid, gid);
        }
        files.add(path);
        CpioHeader header;
        if (directives != null && directives.contains(Directive.GHOST)) {
            header = new CpioHeader(path);
        } else {
            header = new CpioHeader(path, source);
        }
        header.setType(CpioHeader.FILE);
        header.setInode(inode++);
        header.setUid(uid == -1 ? (int) Files.getAttribute(source, "unix:uid") : uid);
        header.setGid(gid == -1 ? (int) Files.getAttribute(source, "unix:gid") : gid);
        header.setPermissions(permissions == -1 ? DEFAULT_FILE_PERMISSION : permissions);
        header.setVerifyFlags(verifyFlags);
        headers.add(header);
        UserGroup userGroup = new UserGroup();
        userGroup.user = getDefaultIfMissing(uname, DEFAULT_USERNAME);
        userGroup.group = getDefaultIfMissing(gname, DEFAULT_GROUP);
        usergroups.put(header, userGroup);
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
     * @param uname       user owner of the URL
     * @param gname       group owner of the URL
     * @param uid       user owner for the given file
     * @param gid       group owner for the given file
     * @param dirmode     permission flags for parent directories, use -1 to leave as default.
     */
    public void addURL(String path, URL source, int permissions, EnumSet<Directive> directive,
                       String uname, String gname, int uid, int gid, int dirmode) {
        if (files.contains(path)) {
            return;
        }
        addParents(Paths.get(path), dirmode, uname, gname, uid, gid);
        files.add(path);
        CpioHeader header = new CpioHeader(path, source);
        header.setType(CpioHeader.FILE);
        header.setInode(inode++);
        header.setUid(uid == -1 ? DEFAULT_UID : uid);
        header.setGid(gid == -1 ? DEFAULT_GID : gid);
        header.setPermissions(permissions == -1 ? DEFAULT_FILE_PERMISSION : permissions);
        headers.add(header);
        UserGroup userGroup = new UserGroup();
        userGroup.user = getDefaultIfMissing(uname, DEFAULT_USERNAME);
        userGroup.group = getDefaultIfMissing(gname, DEFAULT_GROUP);
        usergroups.put(header, userGroup);
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
     * @param uid       user owner for the given file
     * @param gid       group owner for the given file
     */
    private void addParents(Path path, int permissions, String uname, String gname, int uid, int gid) {
        List<String> parents = new ArrayList<>();
        listParents(parents, path);
        for (String parent : parents) {
            addDirectory(parent, permissions, null, uname, gname, uid, gid, true);
        }
    }

    /**
     * Add additional directory that is assumed to already exist on system where the RPM will be installed
     * (e.g. /etc) and should not have an entry in the RPM.
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
    public StringList getDirNames() {
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
        return new StringList(set);
    }

    /**
     * Gets the dirindexes headers values.
     *
     * @return the dirindexes
     */
    public IntegerList getDirIndexes() {
        StringList dirs = getDirNames();
        IntegerList array = new IntegerList();
        for (CpioHeader header : headers) {
            Path path = Paths.get(header.getName()).getParent();
            if (path == null) {
                array.add(0);
                continue;
            }
            String parent = CpioHeader.normalizePath(path.toString());
            if (!parent.endsWith("/")) {
                parent += "/";
            }
            array.add(dirs.indexOf(parent));
        }
        return array;
    }

    /**
     * Gets the basenames header values.
     *
     * @return the basename header values
     */
    public StringList getBaseNames() {
        StringList array = new StringList();
        for (CpioHeader header : headers) {
            Path path = Paths.get(header.getName()).getFileName();
            array.add(path != null ? CpioHeader.normalizePath(path.toString()) : "");
        }
        return array;
    }

    /**
     * Gets the sizes header values.
     *
     * @return the sizes header values
     */
    public IntegerList getSizes() {
        IntegerList array = new IntegerList();
        try {
            for (CpioHeader header : headers) {
                Object object = sources.get(header);
                if (object instanceof Path) {
                    array.add((int) Files.size((Path) object));
                } else if (object instanceof URL) {
                    array.add(((URL) object).openConnection().getContentLength());
                } else if (header.getType() == CpioHeader.DIR) {
                    array.add(4096);
                } else if (header.getType() == CpioHeader.SYMLINK) {
                    array.add(((String) object).length());
                }
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
    public ShortList getModes() {
        ShortList array = new ShortList();
        for (CpioHeader header : headers) {
            array.add((short) header.getMode());
        }
        return array;
    }

    /**
     * Gets the rdevs header values.
     *
     * @return the rdevs header values
     */
    public ShortList getRdevs() {
        ShortList array = new ShortList();
        for (CpioHeader header : headers) {
            array.add((short) ((header.getRdevMajor() << 8) + header.getRdevMinor()));
        }
        return array;
    }

    /**
     * Gets the mtimes header values.
     *
     * @return the mtimes header values
     */
    public IntegerList getMtimes() {
        IntegerList array = new IntegerList();
        for (CpioHeader header : headers) {
            array.add(header.getMtime());
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
    public StringList getDigests(HashAlgo hashAlgo) throws IOException, RpmException {
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        StringList array = new StringList();
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
            array.add(value);
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
        ChannelWrapper.Consumer<byte[]> consumer = new ChannelWrapper.Consumer<>() {
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
    public StringList getLinkTos() {
        StringList array = new StringList();
        for (CpioHeader header : headers) {
            Object object = sources.get(header);
            String value = "";
            if (object instanceof String) {
                value = String.valueOf(object);
            }
            array.add(value);
        }
        return array;
    }

    /**
     * Gets the flags header values.
     *
     * @return the flags header values
     */
    public IntegerList getFlags() {
        IntegerList array = new IntegerList();
        for (CpioHeader header : headers) {
            array.add(header.getFlags());
        }
        return array;
    }

    /**
     * Gets the users.
     *
     * @return the users
     */
    public StringList getUsers() {
        StringList list = new StringList();
        for (CpioHeader header : headers) {
            UserGroup userGroup = usergroups.get(header);
            list.add(userGroup.user);
        }
        return list;
    }

    /**
     * Gets the groups.
     *
     * @return the groups
     */
    public StringList getGroups() {
        StringList list = new StringList();
        for (CpioHeader header : headers) {
            UserGroup userGroup = usergroups.get(header);
            list.add(userGroup.group);
        }
        return list;
    }

    /**
     * Gets the verifyflags header values.
     *
     * @return the verifyflags header values
     */
    public IntegerList getVerifyFlags() {
        IntegerList array = new IntegerList();
        for (CpioHeader header : headers) {
            array.add(header.getVerifyFlags());
        }
        return array;
    }

    /**
     * Gets the classes header values.
     *
     * @return the classes header values
     */
    public IntegerList getClasses() {
        IntegerList array = new IntegerList();
        for (int i = 0; i < headers.size(); i++) {
            array.add(1);
        }
        return array;
    }

    /**
     * Gets the devices header values.
     *
     * @return the devices header values
     */
    public IntegerList getDevices() {
        IntegerList array = new IntegerList();
        for (CpioHeader header : headers) {
            array.add((header.getDevMajor() << 8) + header.getDevMinor());
        }
        return array;
    }

    /**
     * Gets the inodes header values.
     *
     * @return the iNodes header values
     */
    public IntegerList getInodes() {
        IntegerList array = new IntegerList();
        for (CpioHeader header : headers) {
            array.add(header.getInode());
        }
        return array;
    }

    /**
     * Gets the langs header values.
     *
     * @return the langs header values
     */
    public StringList getLangs() {
        StringList array = new StringList();
        for (int i =0; i < headers.size(); i++) {
            array.add("");
        }
        return array;
    }

    /**
     * Gets the contexts header values.
     *
     * @return the contexts header values
     */
    public StringList getContexts() {
        StringList array = new StringList();
        for (int i = 0; i< headers.size(); i++) {
            array.add("<<none>>");
        }
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

    private String getDefaultIfMissing(String value, String defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(hex.charAt(((int) aByte & 0xf0) >> 4)).append(hex.charAt((int) aByte & 0x0f));
        }
        return sb.toString();
    }
}
