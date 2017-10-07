package org.xbib.rpm.header;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.xbib.rpm.header.entry.BinSpecEntry;
import org.xbib.rpm.header.entry.I18NStringSpecEntry;
import org.xbib.rpm.header.entry.Int16SpecEntry;
import org.xbib.rpm.header.entry.Int32SpecEntry;
import org.xbib.rpm.header.entry.Int64SpecEntry;
import org.xbib.rpm.header.entry.Int8SpecEntry;
import org.xbib.rpm.header.entry.SpecEntry;
import org.xbib.rpm.header.entry.StringArraySpecEntry;
import org.xbib.rpm.header.entry.StringSpecEntry;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 *
 */
public class HeaderTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testInt8Single() throws Exception {
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
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInt8Multiple() throws Exception {
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
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInt16Single() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) 1);
        buffer.flip();
        SpecEntry<short[]> entry = new Int16SpecEntry();
        entry.setCount(1);
        entry.read(buffer);
        assertEquals(1, entry.getValues()[0]);
        ByteBuffer data = ByteBuffer.allocate(2);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInt16Multiple() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putShort((short) 1);
        buffer.putShort((short) 2);
        buffer.flip();
        SpecEntry<short[]> entry = new Int16SpecEntry();
        entry.setCount(2);
        entry.read(buffer);
        assertEquals(1, entry.getValues()[0]);
        assertEquals(2, entry.getValues()[1]);
        ByteBuffer data = ByteBuffer.allocate(4);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInt32Single() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(1);
        buffer.flip();
        SpecEntry<Integer[]> entry = new Int32SpecEntry();
        entry.setCount(1);
        entry.read(buffer);
        assertEquals(1, entry.getValues()[0].intValue());
        ByteBuffer data = ByteBuffer.allocate(4);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInt32Multiple() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(1);
        buffer.putInt(2);
        buffer.flip();
        SpecEntry<Integer[]> entry = new Int32SpecEntry();
        entry.setCount(2);
        entry.read(buffer);
        assertEquals(1, entry.getValues()[0].intValue());
        assertEquals(2, entry.getValues()[1].intValue());
        ByteBuffer data = ByteBuffer.allocate(8);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInt64Single() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(1);
        buffer.flip();
        SpecEntry<long[]> entry = new Int64SpecEntry();
        entry.setCount(1);
        entry.read(buffer);
        assertEquals(1, entry.getValues()[0]);
        ByteBuffer data = ByteBuffer.allocate(8);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInt64Multiple() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(1);
        buffer.putLong(2);
        buffer.flip();
        SpecEntry<long[]> entry = new Int64SpecEntry();
        entry.setCount(2);
        entry.read(buffer);
        assertEquals(1, entry.getValues()[0]);
        assertEquals(2, entry.getValues()[1]);
        ByteBuffer data = ByteBuffer.allocate(16);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStringSingle() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(Charset.forName("US-ASCII").encode("1234567\000"));
        buffer.flip();
        SpecEntry<String[]> entry = new StringSpecEntry();
        entry.setCount(1);
        entry.read(buffer);
        assertEquals("1234567", entry.getValues()[0]);
        ByteBuffer data = ByteBuffer.allocate(8);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStringMultiple() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(Charset.forName("US-ASCII").encode("1234567\0007654321\000"));
        buffer.flip();
        SpecEntry<String[]> entry = new StringSpecEntry();
        entry.setCount(2);
        entry.read(buffer);
        assertEquals("1234567", entry.getValues()[0]);
        assertEquals("7654321", entry.getValues()[1]);
        ByteBuffer data = ByteBuffer.allocate(16);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBinary() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put("12345678".getBytes());
        buffer.flip();
        SpecEntry<byte[]> entry = new BinSpecEntry();
        entry.setCount(8);
        entry.read(buffer);
        assertTrue(ByteBuffer.wrap("12345678".getBytes()).equals(ByteBuffer.wrap(entry.getValues())));
        ByteBuffer data = ByteBuffer.allocate(8);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStringArraySingle() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(Charset.forName("US-ASCII").encode("1234567\000"));
        buffer.flip();
        SpecEntry<String[]> entry = new StringArraySpecEntry();
        entry.setCount(1);
        entry.read(buffer);
        assertEquals("1234567", entry.getValues()[0]);
        ByteBuffer data = ByteBuffer.allocate(8);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStringArrayMultiple() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(Charset.forName("US-ASCII").encode("1234567\000"));
        buffer.put(Charset.forName("US-ASCII").encode("7654321\000"));
        buffer.flip();
        SpecEntry<String[]> entry = new StringArraySpecEntry();
        entry.setCount(2);
        entry.read(buffer);
        assertEquals("1234567", entry.getValues()[0]);
        assertEquals("7654321", entry.getValues()[1]);
        ByteBuffer data = ByteBuffer.allocate(16);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testI18NStringSingle() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(Charset.forName("US-ASCII").encode("1234567\000"));
        buffer.flip();
        SpecEntry<String[]> entry = new I18NStringSpecEntry();
        entry.setCount(1);
        entry.read(buffer);
        assertEquals("1234567", entry.getValues()[0]);
        ByteBuffer data = ByteBuffer.allocate(8);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertTrue(buffer.equals(data));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testI18NStringMultiple() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(Charset.forName("US-ASCII").encode("1234567\000"));
        buffer.put(Charset.forName("US-ASCII").encode("7654321\000"));
        buffer.flip();
        SpecEntry<String[]> entry = new I18NStringSpecEntry();
        entry.setCount(2);
        entry.read(buffer);
        assertEquals("1234567", entry.getValues()[0]);
        assertEquals("7654321", entry.getValues()[1]);
        ByteBuffer data = ByteBuffer.allocate(16);
        entry.write(data);
        data.flip();
        buffer.flip();
        assertTrue(buffer.equals(data));
    }
}
