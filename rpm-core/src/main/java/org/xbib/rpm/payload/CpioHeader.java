package org.xbib.rpm.payload;

import org.xbib.rpm.io.ChannelWrapper;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

/**
 * This class provides a means to read file content from the compressed CPIO stream
 * that is the body of an RPM distributable. Iterative calls to to read header will
 * result in a header description being returned which includes a count of how many bytes
 * to read from the channel for the file content.
 */
public class CpioHeader {

    public static final int FIFO = 1;

    public static final int CDEV = 2;

    public static final int DIR = 4;

    public static final int BDEV = 6;

    public static final int FILE = 8;

    public static final int SYMLINK = 10;

    public static final int SOCKET = 12;

    private static final int CPIO_HEADER = 110;

    private static final String MAGIC = "070701";

    private static final String TRAILER = "TRAILER!!!";

    private final Charset charset = StandardCharsets.UTF_8;

    private int inode;

    protected int type;

    protected int permissions = 0644;

    private int uid;

    private int gid;

    private int nlink = 1;

    private long mtime;

    private int filesize;

    private int devMinor = 1;

    private int devMajor = 9;

    private int rdevMinor;

    private int rdevMajor;

    private int checksum;

    protected String name;

    protected int flags;

    private int verifyFlags = -1;

    public CpioHeader() {
    }

    public CpioHeader(String name) {
        this.name = name;
    }

    public CpioHeader(String name, URL url) {
        try {
            URLConnection connection = url.openConnection();
            mtime = connection.getLastModified();
            filesize = connection.getContentLength();
            this.name = normalizePath(name);
            setType(FILE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CpioHeader(String name, Path path) throws IOException {
        mtime = Files.getLastModifiedTime(path).toMillis();
        filesize = (int) Files.size(path);
        this.name = normalizePath(name);
        setType(Files.isDirectory(path) ? DIR : FILE);
    }

    public static String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    public static int difference(int start, int boundary) {
        return ((boundary + 1) - (start & boundary)) & boundary;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getPermissions() {
        return permissions;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public int getRdevMajor() {
        return rdevMajor;
    }

    public int getRdevMinor() {
        return rdevMinor;
    }

    public int getDevMajor() {
        return devMajor;
    }

    public int getDevMinor() {
        return devMinor;
    }

    public int getMtime() {
        return (int) (mtime / 1000L);
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public int getInode() {
        return inode;
    }

    public void setInode(int inode) {
        this.inode = inode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getFlags() {
        return flags;
    }

    public void setVerifyFlags(int verifyFlags) {
        this.verifyFlags = verifyFlags;
    }

    public int getVerifyFlags() {
        return verifyFlags;
    }

    public int getMode() {
        return (type << 12) | (permissions & 07777);
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getUid() {
        return uid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public int getGid() {
        return gid;
    }

    /**
     * Test to see if this is the last header, and is therefore the end of the
     * archive. Uses the CPIO magic trailer value to denote the last header of
     * the stream.
     *
     * @return true if last, false if not
     */
    public boolean isLast() {
        return TRAILER.equals(name);
    }

    public void setLast() {
        name = TRAILER;
    }

    public int getFileSize() {
        return filesize;
    }

    public void setFileSize(int filesize) {
        this.filesize = filesize;
    }

    protected ByteBuffer writeSix(CharSequence data) {
        return charset.encode(pad(data, 6));
    }

    protected ByteBuffer writeEight(int data) {
        return charset.encode(pad(Integer.toHexString(data), 8));
    }

    protected CharSequence readSix(CharBuffer buffer) {
        return readChars(buffer, 6);
    }

    protected int readEight(CharBuffer buffer) {
        return Integer.parseInt(readChars(buffer, 8).toString(), 16);
    }

    protected CharSequence readChars(CharBuffer buffer, int length) {
        if (buffer.remaining() < length) {
            throw new IllegalStateException("Buffer has '" + buffer.remaining() + "' bytes but '" + length + "' are needed.");
        }
        try {
            return buffer.subSequence(0, length);
        } finally {
            buffer.position(buffer.position() + length);
        }
    }

    protected String pad(CharSequence sequence, int length) {
        StringBuilder sequenceBuilder = new StringBuilder(sequence);
        while (sequenceBuilder.length() < length) {
            sequenceBuilder.insert(0, "0");
        }
        sequence = sequenceBuilder.toString();
        return sequence.toString();
    }

    protected int skip(ReadableByteChannel channel, int total) throws IOException {
        int skipped = difference(total, 3);
        ChannelWrapper.fill(channel, skipped);
        return skipped;
    }

    public int skip(WritableByteChannel channel, int total) throws IOException {
        int skipped = difference(total, 3);
        ChannelWrapper.empty(channel, ByteBuffer.allocate(skipped));
        return skipped;
    }

    public int read(ReadableByteChannel channel, int total) throws IOException {
        total += skip(channel, total);
        ByteBuffer descriptor = ChannelWrapper.fill(channel, CPIO_HEADER);
        CharBuffer buffer = charset.decode(descriptor);
        CharSequence magic = readSix(buffer);
        if (!MAGIC.equals(magic.toString())) {
            throw new IllegalStateException("Invalid magic number '" + magic + "' of length '" + magic.length() + "'.");
        }
        inode = readEight(buffer);
        int mode = readEight(buffer);
        permissions = mode & 07777;
        type = mode >>> 12;
        uid = readEight(buffer);
        gid = readEight(buffer);
        nlink = readEight(buffer);
        mtime = 1000L * readEight(buffer);
        filesize = readEight(buffer);
        devMajor = readEight(buffer);
        devMinor = readEight(buffer);
        rdevMajor = readEight(buffer);
        rdevMinor = readEight(buffer);
        int namesize = readEight(buffer);
        checksum = readEight(buffer);
        total += CPIO_HEADER;
        name = charset.decode(ChannelWrapper.fill(channel, namesize - 1)).toString();
        ChannelWrapper.fill(channel, 1);
        total += namesize;
        total += skip(channel, total);
        return total;
    }

    /**
     * Write the content for the CPIO header, including the name immediately following. The name data is rounded
     * to the nearest 2 byte boundary as CPIO requires by appending a null when needed.
     *
     * @param channel which channel to write on
     * @param total   current size of header?
     * @return total written and skipped
     * @throws IOException there was an IO error
     */
    public int write(WritableByteChannel channel, int total) throws IOException {
        ByteBuffer buffer = charset.encode(CharBuffer.wrap(name));
        int length = buffer.remaining() + 1;
        ByteBuffer descriptor = ByteBuffer.allocate(CPIO_HEADER);
        descriptor.put(writeSix(MAGIC));
        descriptor.put(writeEight(inode));
        descriptor.put(writeEight(getMode()));
        descriptor.put(writeEight(uid));
        descriptor.put(writeEight(gid));
        descriptor.put(writeEight(nlink));
        descriptor.put(writeEight((int) (mtime / 1000)));
        descriptor.put(writeEight(filesize));
        descriptor.put(writeEight(devMajor));
        descriptor.put(writeEight(devMinor));
        descriptor.put(writeEight(rdevMajor));
        descriptor.put(writeEight(rdevMinor));
        descriptor.put(writeEight(length));
        descriptor.put(writeEight(checksum));
        descriptor.flip();
        total += CPIO_HEADER + length;
        ChannelWrapper.empty(channel, descriptor);
        ChannelWrapper.empty(channel, buffer);
        ChannelWrapper.empty(channel, ByteBuffer.allocate(1));
        return total + skip(channel, total);
    }

    public String toString() {
        return "Inode: " + inode + "\n" +
                "Permission: " + Integer.toString(permissions, 8) + "\n" +
                "Type: " + type + "\n" +
                "UID: " + uid + "\n" +
                "GID: " + gid + "\n" +
                "Nlink: " + nlink + "\n" +
                "MTime: " + new Date(mtime) + "\n" +
                "FileSize: " + filesize + "\n" +
                "DevMinor: " + devMinor + "\n" +
                "DevMajor: " + devMajor + "\n" +
                "RDevMinor: " + rdevMinor + "\n" +
                "RDevMajor: " + rdevMajor + "\n" +
                "NameSize: " + (name.length() + 1) + "\n" +
                "Name: " + name + "\n";
    }
}
