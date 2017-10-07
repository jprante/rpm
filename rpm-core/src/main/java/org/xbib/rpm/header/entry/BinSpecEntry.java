package org.xbib.rpm.header.entry;

import org.xbib.rpm.header.EntryType;

import java.nio.ByteBuffer;

/**
 *
 */
public class BinSpecEntry extends AbstractSpecEntry<byte[]> {

    @Override
    public int getType() {
        return EntryType.BIN_ENTRY;
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public void read(ByteBuffer buffer) {
        byte[] values = new byte[count];
        buffer.get(values);
        setValues(values);
    }

    @Override
    public void write(ByteBuffer data) {
        data.put(values);
    }
}
