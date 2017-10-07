package org.xbib.rpm.header.entry;

import org.xbib.rpm.header.EntryType;

import java.nio.ByteBuffer;

/**
 * @param <T> the type parameter
 */
public interface SpecEntry<T> {

    void setSize(int size);

    void setCount(int count);

    void incCount(int count);

    void setOffset(int offset);

    T getValues();

    void setValues(T values);

    EntryType getEntryType();

    void setEntryType(EntryType entryType);

    int getType();

    int getOffset(int offset);

    int size();

    boolean ready();

    void read(ByteBuffer buffer);

    void write(ByteBuffer buffer);

    void index(ByteBuffer buffer, int position);
}
