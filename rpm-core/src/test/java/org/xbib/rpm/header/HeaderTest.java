package org.xbib.rpm.header;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.xbib.rpm.header.entry.BinSpecEntry;
import org.xbib.rpm.header.entry.I18NStringSpecEntry;
import org.xbib.rpm.header.entry.Int16SpecEntry;
import org.xbib.rpm.header.entry.Int32SpecEntry;
import org.xbib.rpm.header.entry.Int64SpecEntry;
import org.xbib.rpm.header.entry.Int8SpecEntry;
import org.xbib.rpm.header.entry.SpecEntry;
import org.xbib.rpm.header.entry.StringListSpecEntry;
import org.xbib.rpm.header.entry.StringSpecEntry;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class HeaderTest {

    @Test
    public void testInt8Single() {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put((byte) 1);
        buffer.flip();
        SpecEntry<byte[]> entry = new Int8SpecEntry();
        entry.setCount(1);
        entry.read(buffer);
        assertEquals(1, entry.getValues()[0]);
        ByteBuffer data = ByteBuffer.allocate(1);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testInt8Multiple() {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        buffer.flip();
        SpecEntry<byte[]> entry = new Int8SpecEntry();
        entry.setCount(2);
        entry.read(buffer);
        assertEquals(1, entry.getValues()[0]);
        assertEquals(2, entry.getValues()[1]);
        ByteBuffer data = ByteBuffer.allocate(2);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testInt16Single() {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) 1);
        buffer.flip();
        SpecEntry<ShortList> entry = new Int16SpecEntry();
        entry.setCount(1);
        entry.read(buffer);
        assertEquals((short) 1, entry.getValues().get(0));
        ByteBuffer data = ByteBuffer.allocate(2);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testInt16Multiple() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putShort((short) 1);
        buffer.putShort((short) 2);
        buffer.flip();
        SpecEntry<ShortList> entry = new Int16SpecEntry();
        entry.setCount(2);
        entry.read(buffer);
        assertEquals((short) 1, entry.getValues().get(0));
        assertEquals((short) 2, entry.getValues().get(1));
        ByteBuffer data = ByteBuffer.allocate(4);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testInt32Single() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(1);
        buffer.flip();
        SpecEntry<IntegerList> entry = new Int32SpecEntry();
        entry.setCount(1);
        entry.read(buffer);
        assertEquals(1, entry.getValues().get(0));
        ByteBuffer data = ByteBuffer.allocate(4);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testInt32Multiple() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(1);
        buffer.putInt(2);
        buffer.flip();
        SpecEntry<IntegerList> entry = new Int32SpecEntry();
        entry.setCount(2);
        entry.read(buffer);
        assertEquals(1, entry.getValues().get(0));
        assertEquals(2, entry.getValues().get(1));
        ByteBuffer data = ByteBuffer.allocate(8);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testInt64Single() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(1);
        buffer.flip();
        SpecEntry<LongList> entry = new Int64SpecEntry();
        entry.setCount(1);
        entry.read(buffer);
        assertEquals(1, entry.getValues().get(0));
        ByteBuffer data = ByteBuffer.allocate(8);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testInt64Multiple() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(1);
        buffer.putLong(2);
        buffer.flip();
        SpecEntry<LongList> entry = new Int64SpecEntry();
        entry.setCount(2);
        entry.read(buffer);
        assertEquals(1, entry.getValues().get(0));
        assertEquals(2, entry.getValues().get(1));
        ByteBuffer data = ByteBuffer.allocate(16);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testStringSingle() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(StandardCharsets.US_ASCII.encode("1234567\000"));
        buffer.flip();
        SpecEntry<StringList> entry = new StringSpecEntry();
        entry.setCount(1);
        entry.read(buffer);
        assertEquals("1234567", entry.getValues().get(0));
        ByteBuffer data = ByteBuffer.allocate(8);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testStringMultiple() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(StandardCharsets.US_ASCII.encode("1234567\0007654321\000"));
        buffer.flip();
        SpecEntry<StringList> entry = new StringSpecEntry();
        entry.setCount(2);
        entry.read(buffer);
        assertEquals("1234567", entry.getValues().get(0));
        assertEquals("7654321", entry.getValues().get(1));
        ByteBuffer data = ByteBuffer.allocate(16);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testBinary() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put("12345678".getBytes());
        buffer.flip();
        SpecEntry<byte[]> entry = new BinSpecEntry();
        entry.setCount(8);
        entry.read(buffer);
        assertEquals(ByteBuffer.wrap("12345678".getBytes()), ByteBuffer.wrap(entry.getValues()));
        ByteBuffer data = ByteBuffer.allocate(8);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testStringArraySingle() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(StandardCharsets.US_ASCII.encode("1234567\000"));
        buffer.flip();
        SpecEntry<StringList> entry = new StringListSpecEntry();
        entry.setCount(1);
        entry.read(buffer);
        assertEquals("1234567", entry.getValues().get(0));
        ByteBuffer data = ByteBuffer.allocate(8);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testStringArrayMultiple() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(StandardCharsets.US_ASCII.encode("1234567\000"));
        buffer.put(StandardCharsets.US_ASCII.encode("7654321\000"));
        buffer.flip();
        SpecEntry<StringList> entry = new StringListSpecEntry();
        entry.setCount(2);
        entry.read(buffer);
        assertEquals("1234567", entry.getValues().get(0));
        assertEquals("7654321", entry.getValues().get(1));
        ByteBuffer data = ByteBuffer.allocate(16);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testI18NStringSingle() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(StandardCharsets.US_ASCII.encode("1234567\000"));
        buffer.flip();
        SpecEntry<StringList> entry = new I18NStringSpecEntry();
        entry.setCount(1);
        entry.read(buffer);
        assertEquals("1234567", entry.getValues().get(0));
        ByteBuffer data = ByteBuffer.allocate(8);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }

    @Test
    public void testI18NStringMultiple() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(StandardCharsets.US_ASCII.encode("1234567\000"));
        buffer.put(StandardCharsets.US_ASCII.encode("7654321\000"));
        buffer.flip();
        SpecEntry<StringList> entry = new I18NStringSpecEntry();
        entry.setCount(2);
        entry.read(buffer);
        assertEquals("1234567", entry.getValues().get(0));
        assertEquals("7654321", entry.getValues().get(1));
        ByteBuffer data = ByteBuffer.allocate(16);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertEquals(buffer, data);
    }
}
