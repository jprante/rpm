package org.xbib.rpm.header.entry;

import org.xbib.rpm.header.EntryType;
import org.xbib.rpm.header.StringList;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class StringSpecEntry extends AbstractSpecEntry<StringList> {

    @Override
    public int getType() {
        return EntryType.STRING_ENTRY;
    }

    @Override
    public int size() {
        if (size != 0) {
            return size;
        }
        for (String s : values) {
            size += StandardCharsets.UTF_8.encode(s).remaining() + 1;
        }
        return size;
    }

    @Override
    public void read(ByteBuffer buffer) {
        StringList values = new StringList();
        for (int i = 0; i < count; i++) {
            int length = 0;
            while (buffer.get(buffer.position() + length) != 0) {
                length++;
            }
            ByteBuffer slice = buffer.slice();
            buffer.position(buffer.position() + length + 1);
            slice.limit(length);
            values.add(StandardCharsets.UTF_8.decode(slice).toString());
        }
        setValues(values);
    }

    @Override
    public void write(ByteBuffer data) {
        for (String s : values) {
            data.put(StandardCharsets.UTF_8.encode(s)).put((byte) 0);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        if (values != null) {
            for (String s : values) {
                builder.append("\n\t");
                builder.append(s);
            }
        }
        return builder.toString();
    }
}
