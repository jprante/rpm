package org.xbib.rpm.header.entry;

import org.xbib.rpm.header.EntryType;
import org.xbib.rpm.header.ShortList;

import java.nio.ByteBuffer;

/**
 *
 */
public class Int16SpecEntry extends AbstractSpecEntry<ShortList> {

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
        ShortList values = new ShortList();
        for (int i = 0; i < count; i++) {
            values.add(buffer.getShort());
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
