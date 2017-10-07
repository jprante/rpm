package org.xbib.rpm.header.entry;

import org.xbib.rpm.header.EntryType;

import java.nio.ByteBuffer;

/**
 *
 */
public class Int8SpecEntry extends AbstractSpecEntry<byte[]> {

    @Override
    public int getType() {
        return EntryType.INT8_ENTRY;
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public void read(ByteBuffer buffer) {
        byte[] values = new byte[count];
        for (int x = 0; x < count; x++) {
            values[x] = buffer.get();
        }
        setValues(values);
    }

    @Override
    public void write(ByteBuffer data) {
        for (byte b : values) {
            data.put(b);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append("\n\t");
        for (byte b : values) {
            builder.append(b).append(", ");
        }
        return builder.toString();
    }
}
