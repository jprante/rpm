package org.xbib.rpm.changelog;

import org.xbib.rpm.exception.ChangelogParseException;
import org.xbib.rpm.header.Header;
import org.xbib.rpm.header.HeaderTag;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
     * Adds the specified  file to the archive.
     *
     * @param changelogFile the Changelog file to be added
     * @throws IOException             if the specified file cannot be read
     * @throws ChangelogParseException if the file violates the requirements of a Changelog
     */
    public void addChangeLog(Path changelogFile) throws IOException, ChangelogParseException {
        addChangeLog(Files.newInputStream(changelogFile));
    }

    /**
     * Adds the specified  file to the archive.
     *
     * @param changelogFile the Changelog URL to be added
     * @throws IOException             if the specified file cannot be read
     * @throws ChangelogParseException if the file violates the requirements of a Changelog
     */
    public void addChangeLog(URL changelogFile) throws IOException, ChangelogParseException {
        addChangeLog(changelogFile.openStream());
    }

    /**
     * Adds the specified file to the archive.
     *
     * @param changelogStream the changelog stream to be added
     * @throws IOException             if the specified file cannot be read
     * @throws ChangelogParseException if the file violates the requirements of a Changelog
     */
    public void addChangeLog(InputStream changelogStream) throws IOException, ChangelogParseException {
        try (InputStream changelog = changelogStream) {
            ChangelogParser parser = new ChangelogParser();
            List<ChangelogEntry> entries = parser.parse(changelog);
            for (ChangelogEntry entry : entries) {
                addChangeLogEntry(entry);
            }
        }
    }

    private void addChangeLogEntry(ChangelogEntry entry) {
        Long epochSecs = entry.getChangeLogTime().toEpochMilli() / 1000L;
        header.addOrAppendEntry(HeaderTag.CHANGELOGTIME, new Integer[]{epochSecs.intValue()});
        header.addOrAppendEntry(HeaderTag.CHANGELOGNAME, new String[]{entry.getUserMakingChange()});
        header.addOrAppendEntry(HeaderTag.CHANGELOGTEXT, new String[]{entry.getDescription()});
    }
}
