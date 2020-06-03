package org.xbib.rpm.changelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.xbib.rpm.RpmBuilder;
import org.xbib.rpm.RpmReader;
import org.xbib.rpm.format.Format;
import org.xbib.rpm.header.EntryType;
import org.xbib.rpm.header.HeaderTag;
import org.xbib.rpm.header.IntegerList;
import org.xbib.rpm.header.StringList;
import org.xbib.rpm.header.entry.SpecEntry;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 *
 */
public class ChangelogTest {

    @Test
    public void testChangelog() throws Exception {
        RpmBuilder rpmBuilder = new RpmBuilder();
        rpmBuilder.addChangelog(getClass().getResource("changelog").openStream());
        Path path = Paths.get("build");
        rpmBuilder.build(path);
        RpmReader rpmReader = new RpmReader();
        Format format = rpmReader.readFormat(path.resolve(rpmBuilder.getPackageName()));
        assertDateEntryHeaderEqualsAt("Tue Feb 24 2015", format,
                HeaderTag.CHANGELOGTIME, 10, 0);
        assertHeaderEqualsAt("Thomas Jefferson", format,
                HeaderTag.CHANGELOGNAME, 10, 4);
        assertHeaderEqualsAt("- Initial rpm for this package", format,
                HeaderTag.CHANGELOGTEXT, 10, 9);
        String expectedMultiLineDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod \n" +
                "tempor incididunt ut labore et dolore magna aliqua";
        assertHeaderEqualsAt(expectedMultiLineDescription, format,
                HeaderTag.CHANGELOGTEXT, 10, 0);
    }

    private void assertDateEntryHeaderEqualsAt(String expected, Format format, EntryType entryType, int size, int pos) {
        assertNotNull(format, "null format");
        SpecEntry<?> entry = format.getHeader().getEntry(entryType);
        assertNotNull(entry, "Entry not found : " + entryType.getName());
        assertEquals(4, entry.getType(), "Entry type : " + entryType.getName());
        IntegerList values = (IntegerList) entry.getValues();
        assertNotNull(values, "null values");
        assertEquals(size, values.size(), "Entry size : " + entryType.getName());
        LocalDateTime localDate = LocalDateTime.ofEpochSecond(values.get(pos), 0, ZoneOffset.UTC);
        assertEquals(expected, ChangelogParser.CHANGELOG_FORMAT.format(localDate), "Entry value : " + entryType.getName());
    }

    private void assertHeaderEqualsAt(String expected, Format format, EntryType entryType, int size, int pos) {
        assertNotNull(format, "null format");
        SpecEntry<?> entry = format.getHeader().getEntry(entryType);
        assertNotNull(entry, "Entry not found : " + entryType.getName());
        assertEquals(8, entry.getType(), "Entry type : " + entryType.getName());
        StringList values = (StringList) entry.getValues();
        assertNotNull(values, "null values");
        assertEquals(size, values.size(), "Entry size : " + entryType.getName());
        assertEquals(expected, values.get(pos), "Entry value : " + entryType.getName());
    }
}
