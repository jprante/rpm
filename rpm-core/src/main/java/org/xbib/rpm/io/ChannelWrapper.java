package org.xbib.rpm.io;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps an IO channel so that bytes may be observed during transmission. Wrappers around IO channels are
 * used for a variety of purposes, including counting byte output for use in generating headers, calculating
 * a signature across output bytes, and digesting output bytes using a one-way secure hash.
 */
public abstract class ChannelWrapper {

    final Map<Key<?>, Consumer<?>> consumers = new HashMap<>();

    /**
     * Initializes a byte counter on this channel.
     *
     * @return reference to the new key added to the consumers
     */
    public Key<Integer> startCount() {
        Key<Integer> object = new Key<>();
        consumers.put(object, new Consumer<Integer>() {
            int count;

            @Override
            public void consume(ByteBuffer buffer) {
                int c = buffer.remaining();
                count += c;
            }

            @Override
            public Integer finish() {
                return count;
            }
        });
        return object;
    }

    /**
     * Add a new consumer to this channel.
     *
     * @param consumer the channel consumer
     * @return reference to the new key added to the consumers
     */
    public Key<byte[]> startCount(Consumer<byte[]> consumer) {
        Key<byte[]> object = new Key<>();
        consumers.put(object, consumer);
        return object;
    }

    @SuppressWarnings("unchecked")
    public <T> T finish(Key<T> object) {
        return (T) consumers.remove(object).finish();
    }

    public void close() throws IOException {
        if (!consumers.isEmpty()) {
            throw new IOException("there are '" + consumers.size() + "' unfinished consumers");
        }
    }

    /**
     * Creates a new buffer and fills it with bytes from the
     * provided channel. The amount of data to read is specified
     * in the arguments.
     *
     * @param in   the channel to read from
     * @param size the number of bytes to read into a new buffer
     * @return a new buffer containing the bytes read
     * @throws IOException if an IO error occurs
     */
    public static ByteBuffer fill(ReadableByteChannel in, int size) throws IOException {
        return fill(in, ByteBuffer.allocate(size));
    }

    /**
     * Fills the provided buffer it with bytes from the
     * provided channel. The amount of data to read is
     * dependant on the available space in the provided
     * buffer.
     *
     * @param in     the channel to read from
     * @param buffer the buffer to read into
     * @return the provided buffer
     * @throws IOException if an IO error occurs
     */
    public static ByteBuffer fill(ReadableByteChannel in, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (in.read(buffer) == -1) {
                throw new BufferUnderflowException();
            }
        }
        buffer.flip();
        return buffer;
    }

    /**
     * Empties the contents of the given buffer into the
     * writable channel provided. The buffer will be copied
     * to the channel in it's entirety.
     *
     * @param out    the channel to write to
     * @param buffer the buffer to write out to the channel
     * @throws IOException if an IO error occurs
     */
    public static void empty(WritableByteChannel out, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            out.write(buffer);
        }
    }

    /**
     * Interface describing an object that consumes data from a byte buffer.
     *
     * @param <T> the consumer type
     */
    public interface Consumer<T> {

        /**
         * Consume some input from the given buffer.
         *
         * @param buffer the buffer to consume
         */
        void consume(ByteBuffer buffer);

        /**
         * Complete and optionally return a value to the holder of the key.
         *
         * @return reference to the object
         */
        T finish();
    }

    /**
     * @param <T> key type for stream processing
     */
    public static class Key<T> {
    }

}
