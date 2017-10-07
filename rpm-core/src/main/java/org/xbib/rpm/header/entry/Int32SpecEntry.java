package org.xbib.rpm.header.entry;

import org.xbib.rpm.header.EntryType;

import java.nio.ByteBuffer;

/**
 *
 */
public class Int32SpecEntry extends AbstractSpecEntry<Integer[]> {

    @Override
    public int getOffset(int offset) {
        return (offset + 3) & ~3;
    }

    @Override
    public int getType() {
        return EntryType.INT32_ENTRY;
    }

    @Override
    public int size() {
        return count * (Integer.SIZE / 8);
    }

    @Override
    public void read(ByteBuffer buffer) {
        Integer[] values = new Integer[count];
        for (int x = 0; x < count; x++) {
            values[x] = buffer.getInt();
        }
        setValues(values);
    }

    @Override
    public void write(ByteBuffer buffer) {
        for (int x = 0; x < count; x++) {
            Integer i = values[x];
            buffer.putInt(i);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append("\n\t");
        if (values != null) {
            for (Integer i : values) {
                builder.append(i);
                if (values.length > 1) {
                    builder.append(", ");
                }
            }
        }
        return builder.toString();
    }
}
