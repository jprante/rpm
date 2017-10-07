package org.xbib.rpm.header.entry;

import org.xbib.rpm.header.EntryType;

import java.nio.ByteBuffer;

/**
 *
 */
public class Int16SpecEntry extends AbstractSpecEntry<short[]> {

    @Override
    public int getOffset(int offset) {
        return (offset + 1) & ~1;
    }

    @Override
    public int getType() {
        return EntryType.INT16_ENTRY;
    }

    @Override
    public int size() {
        return count * (Short.SIZE / 8);
    }

    @Override
    public void read(ByteBuffer buffer) {
        short[] values = new short[count];
        for (int x = 0; x < count; x++) {
            values[x] = buffer.getShort();
        }
        setValues(values);
    }

    @Override
    public void write(ByteBuffer data) {
        for (short s : values) {
            data.putShort(s);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append("\n\t");
        for (short s : values) {
            builder.append(s & 0xFFFF).append(", ");
        }
        return builder.toString();
    }
}
