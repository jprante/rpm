package org.xbib.rpm.header.entry;

import org.xbib.rpm.header.EntryType;

import java.nio.ByteBuffer;

/**
 *
 */
public class Int64SpecEntry extends AbstractSpecEntry<long[]> {

    @Override
    public int getOffset(int offset) {
        return (offset + 7) & ~7;
    }

    @Override
    public int getType() {
        return EntryType.INT64_ENTRY;
    }

    @Override
    public int size() {
        return count * (Long.SIZE / 8);
    }

    @Override
    public void read(ByteBuffer buffer) {
        long[] values = new long[count];
        for (int x = 0; x < count; x++) {
            values[x] = buffer.getLong();
        }
        setValues(values);
    }

    @Override
    public void write(ByteBuffer data) {
        for (long l : values) {
            data.putLong(l);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append("\n\t");
        for (long l : values) {
            builder.append(l).append(", ");
        }
        return builder.toString();
    }
}
