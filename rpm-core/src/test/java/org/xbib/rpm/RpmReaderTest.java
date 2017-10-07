package org.xbib.rpm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.xbib.rpm.format.Format;

import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class RpmReaderTest {

    @Test
    public void readNoArchRPMTest() throws Exception {
        RpmReader rpmReader = new RpmReader();
        rpmReader.read(Paths.get("src/test/resources/rpm-1-1.0-1.noarch.rpm"));
    }

    @Test
    public void readSomeArchTest() throws Exception {
        RpmReader rpmReader = new RpmReader();
        rpmReader.read(Paths.get("src/test/resources/rpm-3-1.0-1.somearch.rpm"));
    }

    @Test
    public void setHeaderStartAndEndPosition() throws Exception {
        Format format = new RpmReader().readHeader(getClass().getResourceAsStream("/rpm-1-1.0-1.noarch.rpm"));
        assertEquals(280, format.getHeader().getStartPos());
        assertEquals(4760, format.getHeader().getEndPos());
    }

    @Test
    public void fileModesHeaderIsCorrect() throws Exception {
        Format format = new RpmReader().readHeader(getClass().getResourceAsStream("/rpm-1-1.0-1.noarch.rpm"));
        String rpmDescription = format.toString();
        Matcher matcher = Pattern.compile(".*filemodes\\[[^\\]]*\\]\\n[^0-9-]*([^\\n]*).*", Pattern.DOTALL)
                .matcher(rpmDescription);
        if (matcher.matches()) {
            String[] fileModesFromString = matcher.group(1).split(", ");
            String[] expectedFileModes = {"33188", "41471", "16877", "33261", "33261", "33261", "33261", "41453",
                    "33261", "16877", "33188", "33188", "16877", "33188", "41471"};
            assertArrayEquals(expectedFileModes, fileModesFromString);
        } else {
            fail("no match: " + rpmDescription);
        }
    }
}
