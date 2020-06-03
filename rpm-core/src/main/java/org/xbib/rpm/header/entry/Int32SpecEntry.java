package org.xbib.rpm.header.entry;

import org.xbib.rpm.header.EntryType;
import org.xbib.rpm.header.IntegerList;

import java.nio.ByteBuffer;

/**
 *
 */
public class Int32SpecEntry extends AbstractSpecEntry<IntegerList> {

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
        IntegerList values = new IntegerList();
        for (int x = 0; x < count; x++) {
            values.add(buffer.getInt());
        }
        setValues(values);
    }

    @Override
    public void write(ByteBuffer buffer) {
        for (int i = 0; i < count; i++) {
            Integer integer = values.get(i);
            if (integer == null) {
                throw new NullPointerException();
            }
            buffer.putInt(integer);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append("\n\t");
        if (values != null) {
            for (Integer i : values) {
                builder.append(i);
                if (values.size() > 1) {
                    builder.append(", ");
                }
            }
        }
        return builder.toString();
    }
}
