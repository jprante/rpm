package org.xbib.rpm;

import org.xbib.io.compress.bzip2.Bzip2InputStream;
import org.xbib.io.compress.xz.XZInputStream;
import org.xbib.rpm.format.Format;
import org.xbib.rpm.header.Header;
import org.xbib.rpm.header.HeaderTag;
import org.xbib.rpm.header.StringList;
import org.xbib.rpm.header.entry.SpecEntry;
import org.xbib.rpm.io.ChannelWrapper;
import org.xbib.rpm.io.ReadableChannelWrapper;
import org.xbib.rpm.payload.CompressionType;
import org.xbib.rpm.payload.CpioHeader;
import org.xbib.rpm.signature.SignatureTag;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * The RPM reader reads an archive and outputs useful information about its contents. The reader will
 * output the headers of the RPM format itself, as well as the individual headers for the particular
 * packaged content.
 * In addition, the reader will read through the payload and output information about each file
 * contained in the embedded CPIO payload.
 */
public class RpmReader {

    public RpmReader() {
    }

    public Format readFormat(Path path) throws IOException {
        return readFormat(Files.newInputStream((path)));
    }

    public Format readFormat(InputStream inputStream) throws IOException {
        try (InputStream thisInputStream = inputStream) {
            return readFormat(new ReadableChannelWrapper(Channels.newChannel(thisInputStream)), thisInputStream);
        }
    }

    public Format readFormat(ReadableChannelWrapper readableChannelWrapper, InputStream inputStream) throws IOException {
        Format format = readHeader(readableChannelWrapper);
        Header rpmHeader = format.getHeader();
        try (InputStream uncompressed = createUncompressedStream(rpmHeader, inputStream)) {
            readableChannelWrapper = new ReadableChannelWrapper(Channels.newChannel(uncompressed));
            CpioHeader header;
            int total = 0;
            do {
                header = new CpioHeader();
                total = header.read(readableChannelWrapper, total);
                final int skip = header.getFileSize();
                if (uncompressed.skip(skip) != skip) {
                    throw new RuntimeException();
                }
                total += header.getFileSize();
            } while (!header.isLast());
        }
        return format;
    }

    public RpmReaderResult read(Path path) throws IOException {
        return read(Files.newInputStream((path)));
    }

    public RpmReaderResult read(InputStream inputStream) throws IOException {
        try (InputStream thisInputStream = inputStream) {
            return read(new ReadableChannelWrapper(Channels.newChannel(thisInputStream)), thisInputStream);
        }
    }

    public RpmReaderResult read(ReadableChannelWrapper wrapper, InputStream inputStream) throws IOException {
        final Format format = readHeader(wrapper);
        final List<RpmReaderFile> list = new ArrayList<>();
        try (InputStream uncompressed = createUncompressedStream(format.getHeader(), inputStream)) {
            wrapper = new ReadableChannelWrapper(Channels.newChannel(uncompressed));
            CpioHeader header;
            int total = 0;
            do {
                header = new CpioHeader();
                total = header.read(wrapper, total);
                int fileSize = header.getFileSize();
                boolean includingContents = header.getType() == CpioHeader.FILE;
                if (!header.isLast()) {
                    ByteBuffer byteBuffer = includingContents ? ChannelWrapper.fill(wrapper, fileSize) : null;
                    list.add(new RpmReaderFile(header, byteBuffer));
                }
                if (!includingContents) {
                    if (uncompressed.skip(fileSize) != fileSize) {
                        throw new RuntimeException();
                    }
                }
                total += fileSize;
            } while (!header.isLast());
        }


        return new RpmReaderResult() {
            @Override
            public Format getFormat() {
                return format;
            }

            @Override
            public List<RpmReaderFile> getFiles() {
                return list;
            }
        };
    }

    public Format readHeader(Path path) throws IOException {
        return readHeader(Files.newInputStream((path)));
    }

    public Format readHeader(InputStream inputStream) throws IOException {
        try (InputStream thisInputStream = inputStream) {
            return readHeader(new ReadableChannelWrapper(Channels.newChannel(thisInputStream)));
        }
    }

    /**
     * Reads the headers of an RPM and returns a description of it and it's format.
     *
     * @param channelWrapper the channel wrapper to read input from
     * @return information describing the RPM file
     * @throws IOException if an error occurs reading the file
     */
    public Format readHeader(ReadableChannelWrapper channelWrapper) throws IOException {
        Format format = new Format();
        ChannelWrapper.Key<Integer> headerStartKey = channelWrapper.start();
        ChannelWrapper.Key<Integer> lead = channelWrapper.start();
        format.getLead().read(channelWrapper);
        ChannelWrapper.Key<Integer> signature = channelWrapper.start();
        int count = format.getSignatureHeader().read(channelWrapper);
        SpecEntry<?> sigEntry = format.getSignatureHeader().getEntry(SignatureTag.SIGNATURES);
        int expected = sigEntry == null ? 0 :
                ByteBuffer.wrap((byte[]) sigEntry.getValues(), 8, 4).getInt() / -16;
        Integer headerStartPos = channelWrapper.finish(headerStartKey);
        format.getHeader().setStartPos(headerStartPos);
        ChannelWrapper.Key<Integer> headerKey = channelWrapper.start();
        count = format.getHeader().read(channelWrapper);
        SpecEntry<?> immutableEntry = format.getHeader().getEntry(HeaderTag.HEADERIMMUTABLE);
        expected = immutableEntry == null ? 0 :
                ByteBuffer.wrap((byte[]) immutableEntry.getValues(), 8, 4).getInt() / -16;
        Integer headerLength = channelWrapper.finish(headerKey);
        format.getHeader().setEndPos(headerStartPos + headerLength);
        return format;
    }

    /**
     * Create the proper stream wrapper to handling the payload section based on the
     * payload compression header tag.
     *
     * @param header the header
     * @param inputStream  raw input stream of the rpm
     * @return the "proper" input stream
     * @throws IOException an IO error occurred
     */
    private static InputStream createUncompressedStream(Header header, InputStream inputStream) throws IOException {
        InputStream compressedInput = inputStream;
        SpecEntry<?> pcEntry = header.getEntry(HeaderTag.PAYLOADCOMPRESSOR);
        StringList pc = (StringList) pcEntry.getValues();
        CompressionType compressionType = CompressionType.valueOf(pc.get(0).toUpperCase());
        switch (compressionType) {
            case NONE:
                break;
            case GZIP:
                compressedInput = new GZIPInputStream(inputStream);
                break;
            case BZIP2:
                compressedInput = new Bzip2InputStream(inputStream);
                break;
            case XZ:
                compressedInput = new XZInputStream(inputStream);
                break;
        }
        return compressedInput;
    }
}
