package org.xbib.rpm;

import org.xbib.rpm.payload.CpioHeader;
import java.nio.ByteBuffer;

public class RpmReaderFile {

    private final CpioHeader header;

    private final ByteBuffer contents;

    public RpmReaderFile(CpioHeader header, ByteBuffer contents) {
        this.header = header;
        this.contents = contents;
    }

    public CpioHeader getHeader() {
        return header;
    }

    public String getName() {
        return header.getName();
    }

    public int getType() {
        return header.getType();
    }

    public ByteBuffer getContents() {
        return contents;
    }
}
