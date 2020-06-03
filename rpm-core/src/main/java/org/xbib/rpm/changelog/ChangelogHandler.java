package org.xbib.rpm.changelog;

import org.xbib.rpm.exception.ChangelogParseException;
import org.xbib.rpm.header.Header;
import org.xbib.rpm.header.HeaderTag;
import org.xbib.rpm.header.IntegerList;
import org.xbib.rpm.header.StringList;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * This class manages the process of adding a Changelog to the header.
 */
public class ChangelogHandler {

    private final Header header;

    public ChangelogHandler(Header header) {
        this.header = header;
    }

    /**
     * Adds the specified changelog file to the archive.
     *
     * @param changelogFile the Changelog file to be added
     * @throws IOException             if the specified file cannot be read
     * @throws ChangelogParseException if the file violates the requirements of a Changelog
     */
    public void addChangeLog(Path changelogFile) throws IOException, ChangelogParseException {
        addChangeLog(Files.newBufferedReader(changelogFile));
    }

    /**
     * Adds the specified changelog contents to the archive.
     *
     * @param string the changelog contents to be added
     * @throws IOException             if the specified file cannot be read
     * @throws ChangelogParseException if the file violates the requirements of a Changelog
     */    public void addChangeLog(String string) throws IOException, ChangelogParseException {
        addChangeLog(new StringReader(string));
    }

    /**
     * Adds the specified changelod to the archive.
     *
     * @param reader the changelog reader to be added
     * @throws IOException             if the specified file cannot be read
     * @throws ChangelogParseException if the file violates the requirements of a Changelog
     */
    public void addChangeLog(Reader reader) throws IOException, ChangelogParseException {
        ChangelogParser parser = new ChangelogParser();
        List<ChangelogEntry> entries = parser.parse(reader);
        for (ChangelogEntry entry : entries) {
            addChangeLogEntry(entry);
        }
    }

    private void addChangeLogEntry(ChangelogEntry entry) {
        int epochSecs = (int) (entry.getChangeLogTime().toEpochMilli() / 1000L);
        header.addOrAppendEntry(HeaderTag.CHANGELOGTIME, IntegerList.of(epochSecs));
        header.addOrAppendEntry(HeaderTag.CHANGELOGNAME, StringList.of(entry.getUserMakingChange()));
        header.addOrAppendEntry(HeaderTag.CHANGELOGTEXT, StringList.of(entry.getDescription()));
    }
}
