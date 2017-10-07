package org.xbib.gradle.plugin.rpm

import groovy.transform.Canonical
import org.xbib.io.compress.bzip2.Bzip2InputStream
import org.xbib.io.compress.xz.XZInputStream
import org.xbib.rpm.header.Header
import org.xbib.rpm.header.HeaderTag
import org.xbib.rpm.header.entry.SpecEntry
import org.xbib.rpm.io.ChannelWrapper
import org.xbib.rpm.io.ReadableChannelWrapper
import org.xbib.rpm.format.Format
import org.xbib.rpm.payload.CompressionType
import org.xbib.rpm.payload.CpioHeader
import org.spockframework.util.Nullable

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.Channels
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.zip.GZIPInputStream

import static org.xbib.rpm.header.HeaderTag.HEADERIMMUTABLE
import static org.xbib.rpm.signature.SignatureTag.SIGNATURES
import static org.junit.Assert.assertEquals

/**
 *
 */
class RpmReader {

    @Canonical
    static class ReaderResult {
        Format format
        List<ReaderFile> files
    }

    @Canonical
    static class ReaderFile {
        @Delegate
        CpioHeader header

        @Nullable
        ByteBuffer contents

        String asString() {
            if (contents == null ) {
                return null
            }
            Charset charset = StandardCharsets.UTF_8
            CharBuffer buffer = charset.decode(contents)
            return buffer.toString()
        }
    }

    static ReaderResult read(Path path) throws Exception {
        ReaderResult readerResult = null
        path.withInputStream { InputStream inputStream ->
            readerResult = read(inputStream)
        }
        readerResult
    }

    static ReaderResult read(InputStream inputStream, boolean includeContents = true) {
        ReadableChannelWrapper wrapper = new ReadableChannelWrapper(Channels.newChannel(inputStream))
        Format format = readHeader(wrapper)
        InputStream uncompressed = createUncompressedStream(format.getHeader(), inputStream)
        wrapper = new ReadableChannelWrapper(Channels.newChannel(uncompressed))
        CpioHeader header = null
        def files = []
        int total = 0
        while (header == null || !header.isLast()) {
            header = new CpioHeader()
            total = header.read(wrapper, total)
            final int fileSize = header.getFileSize()
            boolean includingContents = includeContents && header.type == CpioHeader.FILE
            if (!header.isLast()) {
                ByteBuffer descriptor = includingContents ? ChannelWrapper.fill(wrapper, fileSize) : null
                files += new ReaderFile(header, descriptor)
            }
            if (!includingContents) {
                assertEquals(fileSize, uncompressed.skip(fileSize))
            }
            total += fileSize
        }
        return new ReaderResult(format,files)
    }

    static InputStream createUncompressedStream(Header header, InputStream inputStream) {
        InputStream compressedInput = inputStream
        SpecEntry<?> pcEntry = header.getEntry(HeaderTag.PAYLOADCOMPRESSOR)
        String[] pc = (String[]) pcEntry.getValues()
        CompressionType compressionType = CompressionType.valueOf(pc[0].toUpperCase())
        switch (compressionType) {
            case CompressionType.NONE:
                break
            case CompressionType.GZIP:
                compressedInput = new GZIPInputStream(inputStream)
                break
            case CompressionType.BZIP2:
                compressedInput = new Bzip2InputStream(inputStream)
                break
            case CompressionType.XZ:
                compressedInput = new XZInputStream(inputStream)
                break
        }
        compressedInput
    }

    static Format readHeader(ReadableChannelWrapper wrapper) throws Exception {
        Format format = new Format()
        format.getLead().read(wrapper)
        int count = format.signatureHeader.read(wrapper)
        int expected = ByteBuffer.wrap(format.signatureHeader.getEntry(SIGNATURES).values, 8, 4).getInt() / -16
        assertEquals(expected, count)
        count = format.getHeader().read(wrapper)
        expected = ByteBuffer.wrap(format.getHeader().getEntry(HEADERIMMUTABLE).values, 8, 4).getInt() / -16
        assertEquals(expected, count)
        return format
    }

    def static getHeaderEntry(ReaderResult res, tag) {
        def header = res.format.header
        header.getEntry(tag.code)
    }

    def static getHeaderEntryString(ReaderResult res, tag) {
        getHeaderEntry(res, tag)?.values?.join('')
    }
}
