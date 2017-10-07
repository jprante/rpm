package org.xbib.rpm.changelog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.xbib.rpm.RpmBuilder;
import org.xbib.rpm.RpmReader;
import org.xbib.rpm.format.Format;
import org.xbib.rpm.header.EntryType;
import org.xbib.rpm.header.HeaderTag;
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
        rpmBuilder.addChangelog(getClass().getResource("changelog"));
        Path path = Paths.get("build");
        rpmBuilder.build(path);
        RpmReader rpmReader = new RpmReader();
        Format format = rpmReader.read(path.resolve(rpmBuilder.getPackageName()));
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
        assertNotNull("null format", format);
        SpecEntry<?> entry = format.getHeader().getEntry(entryType);
        assertNotNull("Entry not found : " + entryType.getName(), entry);
        assertEquals("Entry type : " + entryType.getName(), 4, entry.getType());
        Integer[] values = (Integer[]) entry.getValues();
        assertNotNull("null values", values);
        assertEquals("Entry size : " + entryType.getName(), size, values.length);
        LocalDateTime localDate = LocalDateTime.ofEpochSecond(values[pos], 0, ZoneOffset.UTC);
        assertEquals("Entry value : " + entryType.getName(), expected, ChangelogParser.CHANGELOG_FORMAT.format(localDate));
    }

    private void assertHeaderEqualsAt(String expected, Format format, EntryType entryType, int size, int pos) {
        assertNotNull("null format", format);
        SpecEntry<?> entry = format.getHeader().getEntry(entryType);
        assertNotNull("Entry not found : " + entryType.getName(), entry);
        assertEquals("Entry type : " + entryType.getName(), 8, entry.getType());
        String[] values = (String[]) entry.getValues();
        assertNotNull("null values", values);
        assertEquals("Entry size : " + entryType.getName(), size, values.length);
        assertEquals("Entry value : " + entryType.getName(), expected, values[pos]);
    }
}
