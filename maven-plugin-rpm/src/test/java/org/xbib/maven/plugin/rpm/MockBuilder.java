package org.xbib.maven.plugin.rpm;

import org.xbib.rpm.RpmBuilder;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;

/**
 *
 */
public class MockBuilder extends RpmBuilder {

    @Override
    public void build(Path directory) throws IOException {
    }

    @Override
    public void build(SeekableByteChannel channel) throws IOException {
    }
}
