package org.xbib.rpm.header.entry;

import org.xbib.rpm.header.EntryType;
import org.xbib.rpm.header.Tags;

import java.nio.ByteBuffer;

/**
 * @param <T> the type parameter
 */
public abstract class AbstractSpecEntry<T> implements SpecEntry<T> {

    protected int size;

    protected EntryType entryType;

    protected int count;

    protected int offset;

    protected T values;

    public EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incCount(int count) {
        this.count += count;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public T getValues() {
        return values;
    }

    public void setValues(T values) {
        this.values = values;
    }

    public int getOffset(int offset) {
        return offset;
    }

    public boolean ready() {
        return values != null;
    }

    public abstract int getType();

    public void typeCheck() {
    }

    /**
     * Returns the size this entry will need in the provided data buffer to write
     * it's contents, corrected for any trailing zeros to fill to a boundary.
     */
    public abstract int size();

    /**
     * Reads this entries value from the provided buffer using the set count.
     */
    public abstract void read(ByteBuffer buffer);

    /**
     * Writes this entries index to the index buffer and its values to the output
     * channel provided.
     */
    public abstract void write(ByteBuffer data);

    /**
     * Writes the index entry into the provided buffer at the current position.
     */
    public void index(ByteBuffer index, int position) {
        index.putInt(entryType.getCode()).putInt(getType()).putInt(position).putInt(count);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (Tags.tags().containsKey(entryType.getCode())) {
            builder.append(Tags.tags().get(entryType.getCode()).getName());
        } else {
            builder.append(super.toString());
        }
        builder.append("[tag=").append(entryType)
                .append(",type=").append(getType())
                .append(",count=").append(count)
                .append(",size=").append(size())
                .append(",offset=").append(offset)
                .append("]");
        return builder.toString();
    }
}
