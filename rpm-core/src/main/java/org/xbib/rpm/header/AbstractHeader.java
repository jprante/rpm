package org.xbib.rpm.header;

import org.xbib.rpm.header.entry.BinSpecEntry;
import org.xbib.rpm.header.entry.I18NStringSpecEntry;
import org.xbib.rpm.header.entry.Int16SpecEntry;
import org.xbib.rpm.header.entry.Int32SpecEntry;
import org.xbib.rpm.header.entry.Int64SpecEntry;
import org.xbib.rpm.header.entry.Int8SpecEntry;
import org.xbib.rpm.header.entry.SpecEntry;
import org.xbib.rpm.header.entry.StringListSpecEntry;
import org.xbib.rpm.header.entry.StringSpecEntry;
import org.xbib.rpm.io.ChannelWrapper;
import org.xbib.rpm.lead.Lead;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public abstract class AbstractHeader {

    private static final int HEADER_SIZE = 16;

    private static final int ENTRY_SIZE = 16;

    private static final int MAGIC_WORD = 0x8EADE801;

    protected final Map<Integer, EntryType> tags = new HashMap<>();

    private final Map<Integer, SpecEntry<?>> entries = new TreeMap<>();

    private final Map<SpecEntry<?>, Integer> pending = new LinkedHashMap<>();

    private int startPos;

    private int endPos;

    protected abstract boolean pad();

    /**
     * Reads the entire header contents for this channel and returns the number of entries found.
     *
     * @param in the ReadableByteChannel to read
     * @return the number read
     * @throws IOException there was an IO error
     */
    public int read(ReadableByteChannel in) throws IOException {
        ByteBuffer header = ChannelWrapper.fill(in, HEADER_SIZE);
        int magic = header.getInt();
        if (magic == 0) {
            header.compact();
            ChannelWrapper.fill(in, header);
            magic = header.getInt();
        }
        if (MAGIC_WORD != magic) {
            throw new IOException("check expected " + Integer.toHexString(0xff & MAGIC_WORD) +
                    ", found " + Integer.toHexString(0xff & magic));
        }
        header.getInt();
        ByteBuffer index = ChannelWrapper.fill(in, header.getInt() * ENTRY_SIZE);
        int total = header.getInt();
        int pad = pad() ? ((total + 7) & ~7) - total : 0;
        ByteBuffer data = ChannelWrapper.fill(in, total + pad);
        int count = 0;
        while (index.remaining() >= ENTRY_SIZE) {
            readEntry(index.getInt(), index.getInt(), index.getInt(), index.getInt(), data);
            count++;
        }
        return count;
    }

    /**
     * Writes this header section to the provided file at the current position and returns the
     * required padding.  The caller is responsible for adding the padding immediately after
     * this data.
     *
     * @param out the WritableByteChannel to output to
     * @return the number written
     * @throws IOException there was an IO error
     */
    public int write(WritableByteChannel out) throws IOException {
        ByteBuffer header = getHeader();
        ByteBuffer index = getIndex();
        ByteBuffer data = getData(index);
        data.flip();
        int pad = pad() ? ((data.remaining() + 7) & ~7) - data.remaining() : 0;
        header.putInt(data.remaining());
        ChannelWrapper.empty(out, header.flip());
        ChannelWrapper.empty(out, index.flip());
        ChannelWrapper.empty(out, data);
        return pad;
    }

    public Map<Integer, SpecEntry<?>> getEntries() {
        return entries;
    }

    public int count() {
        return entries.size();
    }

    public SpecEntry<?> getEntry(EntryType entryType) {
        return getEntry(entryType.getCode());
    }

    public SpecEntry<?> getEntry(int code) {
        return entries.get(code);
    }

    @SuppressWarnings("unchecked")
    public void createEntry(EntryType entryType, CharSequence value) {
        if (value == null) {
            throw new IllegalArgumentException("unable to accept null value for entry " +
                    entryType.getName() + " " + entryType.getType());
        }
        SpecEntry<StringList> entry = (SpecEntry<StringList>) makeEntry(entryType, 1);
        entry.setValues(StringList.of(value.toString()));
    }

    @SuppressWarnings("unchecked")
    public void createEntry(EntryType entryType, Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("unable to accept null value for entry " +
                    entryType.getName() + " " + entryType.getType());
        }
        SpecEntry<IntegerList> entry = (SpecEntry<IntegerList>) makeEntry(entryType, 1);
        entry.setValues(IntegerList.of(value));
    }

    public void writePending(SeekableByteChannel channel) {
        for (Map.Entry<SpecEntry<?>, Integer> entry : pending.entrySet()) {
            try {
                ByteBuffer data = ByteBuffer.allocate(entry.getKey().size());
                entry.getKey().write(data);
                channel.position(Lead.LEAD_SIZE + HEADER_SIZE + count() * ENTRY_SIZE + entry.getValue());
                ChannelWrapper.empty(channel, data.flip());
            } catch (Exception e) {
                throw new RuntimeException("Error writing pending entry '" + entry.getKey().getEntryType() + "'.", e);
            }
        }
    }

    /**
     * This is the main entry point through which entries are created from the builder code for
     * types other than String.
     *
     * @param <T>    type parameter
     * @param entryType    the Tag identifying the type of header this is bound for
     * @param values the values to be stored in the entry.
     * @throws ClassCastException - if the type of values is not compatible with the type
     *                            required by tag
     */
    @SuppressWarnings({"unchecked", "raw"})
    public <T> void createEntry(EntryType entryType, T values) {
        int len = 1;
        if (values.getClass().isArray()) {
            len = Array.getLength(values);
        }
        if (Collection.class.isAssignableFrom(values.getClass())) {
            len = ((Collection) values).size();
        }
        SpecEntry<T> entry = (SpecEntry<T>) makeEntry(entryType, len);
        if (entryType instanceof HeaderTag) {
            Class<?> cl = ((HeaderTag) entryType).getTypeClass();
            if (cl.isAssignableFrom(values.getClass())) {
                entry.setValues(values);
            } else {
                throw new ClassCastException("cl = " + cl.getName() + " values = " + values.getClass().getName());
            }
        } else {
            entry.setValues(values);
        }
    }

    /**
     * This is the main entry point through which entries are created or appended to
     * from the builder or from places like the ChangelogHandler.  This is useful for
     * header types which may have multiple components of each tag, as changelogs do.
     *
     * @param <T> type parameter
     * @param entryType  the Tag identifying the type of header this is bound for
     * @param values the values to be stored in or appended to the entry.
     * @throws ClassCastException - if the type of values is not compatible with the
     *                            type required by tag
     */
    @SuppressWarnings({"unchecked", "raw"})
    public <T> void addOrAppendEntry(EntryType entryType, T values) {
        SpecEntry<T> entry = (SpecEntry<T>) addOrAppendEntry(entryType,
                values.getClass().isArray() ? Array.getLength(values) : 1);
        T existingValues = entry.getValues();
        if (existingValues == null) {
            entry.setValues(values);
        } else if (existingValues instanceof Collection) {
            Collection<T> collection = (Collection<T>) existingValues;
            if (values instanceof Collection) {
                Collection<T> valuesCollection = (Collection<T>) values;
                collection.addAll(valuesCollection);
            } else {
                collection.add(values);
            }
            entry.setValues((T) collection);
            /*int oldSize = java.lang.reflect.Array.getLength(existingValues);
            int newSize = values.getClass().isArray() ? Array.getLength(values) : 1;
            Class<?> elementType = existingValues.getClass().getComponentType();
            T newValues = (T) Array.newInstance(elementType, oldSize + newSize);
            System.arraycopy(existingValues, 0, newValues, 0, oldSize);
            System.arraycopy(values, 0, newValues, oldSize, newSize);
            entry.setValues(newValues);*/
        }
    }

    /**
     * Adds a pending entry to this header.  This entry will have the correctly sized buffer allocated, but
     * will not be written until the caller writes a value and then invokes {@link #writePending} on this
     * object.
     *
     * @param entryType   the tag
     * @param count the count
     * @return the entry added
     */
    public SpecEntry<?> addEntry(EntryType entryType, int count) {
        return makeEntry(entryType, count);
    }

    public int getEndPos() {
        return endPos;
    }

    public void setEndPos(int endPos) {
        this.endPos = endPos;
    }

    public int getStartPos() {
        return startPos;
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    /**
     * Memory maps the portion of the destination file that will contain the header structure
     * header and advances the file channels position.  The resulting buffer will be prefilled with
     * the necesssary magic data and the correct index count, but will require an integer value to
     * be written with the total data section size once data writing is complete.
     * This method must be invoked before mapping the index or data sections.
     *
     * @return a buffer containing the header
     */
    protected ByteBuffer getHeader() {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
        buffer.putInt(MAGIC_WORD);
        buffer.putInt(0);
        buffer.putInt(count());
        return buffer;
    }

    /**
     * Memory maps the portion of the destination file that will contain the index structure
     * header and advances the file channels position.  The resulting buffer will be ready for
     * writing of the entry indexes.
     * This method must be invoked before mapping the data section, but after mapping the header.
     *
     * @return a buffer containing the header
     * @throws IOException there was an IO error
     */
    private ByteBuffer getIndex() throws IOException {
        return ByteBuffer.allocate(count() * ENTRY_SIZE);
    }

    /**
     * Writes the data section of the file, starting at the current position which must be immediately
     * after the header section.  Each entry writes its corresponding index into the provided index buffer
     * and then writes its data to the file channel.
     *
     * @param index ByteBuffer of the index
     * @return the total number of bytes written to the data section of the file.
     */
    private ByteBuffer getData(final ByteBuffer index) {
        int offset = 0;
        List<ByteBuffer> buffers = new ArrayList<>();
        Iterator<Integer> i = entries.keySet().iterator();
        index.position(16);
        SpecEntry<?> first = entries.get(i.next());
        SpecEntry<?> entry = null;
        try {
            while (i.hasNext()) {
                entry = entries.get(i.next());
                offset = writeData(buffers, index, entry, offset);
            }
            index.position(0);
            offset = writeData(buffers, index, first, offset);
            index.position(index.limit());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error while writing '" + entry + "'.", e);
        }
        ByteBuffer data = ByteBuffer.allocate(offset);
        for (ByteBuffer buffer : buffers) {
            data.put(buffer);
        }
        return data;
    }

    private int writeData(Collection<ByteBuffer> buffers, ByteBuffer index, SpecEntry<?> entry, int offset) {
        int shift = entry.getOffset(offset) - offset;
        if (shift > 0) {
            buffers.add(ByteBuffer.allocate(shift));
        }
        offset += shift;
        int size = entry.size();
        if (size == 0) {
            throw new IllegalStateException("entry size is 0");
        }
        ByteBuffer buffer = ByteBuffer.allocate(size);
        entry.index(index, offset);
        if (entry.ready()) {
            entry.write(buffer);
            buffer.flip();
        } else {
            pending.put(entry, offset);
        }
        buffers.add(buffer);
        return offset + size;
    }

    private void readEntry(int tag, int type, int offset, int count, ByteBuffer data) {
        SpecEntry<?> entry = makeEntry(type);
        entry.setEntryType(Tags.from(tag));
        entry.setCount(count);
        entries.put(tag, entry);
        ByteBuffer buffer = data.duplicate();
        buffer.position(offset);
        entry.read(buffer);
        entry.setOffset(offset);
    }

    private SpecEntry<?> makeEntry(EntryType entryType, int count) {
        SpecEntry<?> entry = makeEntry(entryType.getType());
        entry.setEntryType(entryType);
        entry.setCount(count);
        entries.put(entryType.getCode(), entry);
        return entry;
    }

    private SpecEntry<?> addOrAppendEntry(EntryType entryType, int count) {
        SpecEntry<?> entry = entries.get(entryType.getCode());
        if (entry == null) {
            entry = makeEntry(entryType.getType());
            entry.setEntryType(entryType);
            entry.setCount(count);
        } else {
            entry.incCount(count);
        }
        entries.put(entryType.getCode(), entry);
        return entry;
    }

    private SpecEntry<?> makeEntry(int type) {
        switch (type) {
            case EntryType.INT8_ENTRY:
                return new Int8SpecEntry();
            case EntryType.INT16_ENTRY:
                return new Int16SpecEntry();
            case EntryType.INT32_ENTRY:
                return new Int32SpecEntry();
            case EntryType.INT64_ENTRY:
                return new Int64SpecEntry();
            case EntryType.STRING_ENTRY:
                return new StringSpecEntry();
            case EntryType.BIN_ENTRY:
                return new BinSpecEntry();
            case EntryType.STRING_LIST_ENTRY:
                return new StringListSpecEntry();
            case EntryType.I18NSTRING_ENTRY:
                return new I18NStringSpecEntry();
            default:
                throw new IllegalStateException("Unknown entry type '" + type + "'.");
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Header(").append(getClass()).append(")").append("\n");
        int count = 0;
        for (Map.Entry<Integer, SpecEntry<?>> entry : entries.entrySet()) {
            builder.append(count++).append(": ").append(entry.getValue()).append("\n");
        }
        return builder.toString();
    }
}
