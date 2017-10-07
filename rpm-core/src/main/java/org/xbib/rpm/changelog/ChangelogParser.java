package org.xbib.rpm.changelog;

import org.xbib.rpm.exception.ChangelogParseException;
import org.xbib.rpm.exception.DatesOutOfSequenceException;
import org.xbib.rpm.exception.IncompleteChangelogEntryException;
import org.xbib.rpm.exception.InvalidChangelogDateException;
import org.xbib.rpm.exception.NoInitialAsteriskException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *
 */
public class ChangelogParser {

    public static final DateTimeFormatter CHANGELOG_FORMAT =
            DateTimeFormatter.ofPattern("EEE MMM dd yyyy").withLocale(Locale.US).withZone(ZoneId.systemDefault());

    /**
     * @param lines an array of lines read from the Changelog file
     * @return a List of ChangeLogEntry objects
     * @throws DateTimeParseException  if date could not be parsed
     * @throws ChangelogParseException if any of the rules of a Changelog is violated by the input
     */
    public List<ChangelogEntry> parse(String[] lines) throws DateTimeParseException, ChangelogParseException {
        final int timeLen = 15;
        List<ChangelogEntry> result = new ArrayList<>();
        if (lines.length == 0) {
            return result;
        }
        ParsingState state = ParsingState.NEW;
        Instant lastTime = null;
        ChangelogEntry entry = new ChangelogEntry();
        String restOfLine = null;
        StringBuilder descr = new StringBuilder();
        int index = 0;
        String line = lines[index];
        lineloop:
        while (true) {
            switch (state) {
                case NEW:
                    if (line.startsWith("#")) {
                        if (++index < lines.length) {
                            line = lines[index];
                            continue;
                        } else {
                            return result;
                        }
                    } else if (!line.startsWith("*")) {
                        throw new NoInitialAsteriskException();
                    }
                    restOfLine = line.substring(1).trim();
                    state = ParsingState.TIME;
                    break;
                case TIME:
                    if (restOfLine.length() < timeLen) {
                        throw new InvalidChangelogDateException(restOfLine);
                    }
                    String timestr = restOfLine.substring(0, timeLen);
                    try {
                        Instant entryTime = LocalDate.parse(timestr, CHANGELOG_FORMAT)
                                .atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.UTC);
                        if (lastTime != null && lastTime.isBefore(entryTime)) {
                            throw new DatesOutOfSequenceException();
                        }
                        entry.setChangeLogTime(entryTime);
                        lastTime = entryTime;
                        state = ParsingState.NAME;
                    } catch (DateTimeParseException e) {
                        throw new InvalidChangelogDateException(e);
                    }
                    break;
                case NAME:
                    String name = restOfLine.substring(timeLen).trim();
                    if (name.length() > 0) {
                        entry.setUserMakingChange(name);
                    }
                    state = ParsingState.TEXT;
                    break;
                case TEXT:
                    index++;
                    if (index < lines.length) {
                        line = lines[index];
                        if (line.startsWith("*")) {
                            if (descr.length() > 1) {
                                entry.setDescription(descr.toString().substring(0, descr.length() - 1));
                            }
                            if (entry.isComplete()) {
                                result.add(entry);
                                entry = new ChangelogEntry();
                                descr = new StringBuilder();
                                state = ParsingState.NEW;
                            } else {
                                throw new IncompleteChangelogEntryException();
                            }
                        } else {
                            descr.append(line).append('\n');
                        }
                    } else {
                        break lineloop;
                    }
            }
        }
        if (descr.length() > 1) {
            entry.setDescription(descr.toString().substring(0, descr.length() - 1));
        }
        if (entry.isComplete()) {
            result.add(entry);
        } else {
            throw new IncompleteChangelogEntryException();
        }
        return result;
    }

    /**
     * @param stream stream read from the Changelog file
     * @return a List of ChangeLogEntry objects
     * @throws IOException             if the input stream cannot be read
     * @throws ChangelogParseException if any of the rules of a Changelog is
     *                                 violated by the input
     */
    public List<ChangelogEntry> parse(InputStream stream) throws IOException, ChangelogParseException {
        String line;
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    lines.add(line);
                }
            }
        }
        return parse(lines.toArray(new String[0]));
    }
}
