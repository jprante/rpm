package org.xbib.rpm;

import org.xbib.rpm.format.Format;
import java.util.List;

public interface RpmReaderResult {

    Format getFormat();

    List<RpmReaderFile> getFiles();
}
