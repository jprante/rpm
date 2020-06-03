package org.xbib.rpm.changelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbib.rpm.exception.ChangelogParseException;
import org.xbib.rpm.exception.DatesOutOfSequenceException;
import org.xbib.rpm.exception.IncompleteChangelogEntryException;
import org.xbib.rpm.exception.InvalidChangelogDateException;
import org.xbib.rpm.exception.NoInitialAsteriskException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class ChangelogParserTest {

    private ChangelogParser parser;

    private List<ChangelogEntry> changelogs;

    @BeforeEach
    public void setUp()  {
        parser = new ChangelogParser();
    }

    @Test
    public void testParsesCorrectlyFormattedChangelog() {
        List<String> lines = Arrays.asList(
                "* Tue Feb 24 2015 George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt",
                "* Tue Feb 10 2015 George Washington",
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.");
        try {
            changelogs = parser.parse(lines);
            assertEquals(2, changelogs.size(), "parses correctly formatted Changelog");
        } catch (ChangelogParseException e) {
            fail("parses correctly formatted Changelog");
        }
    }

    @Test
    public void commentsIgnored() {
        List<String> lines = Arrays.asList(
                "# ORDER MUST BE DESCENDING (most recent change at top)",
                "* Tue Feb 24 2015 George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt",
                "* Tue Feb 10 2015 George Washington",
                "# a random comment",
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.");
        try {
            changelogs = parser.parse(lines);
            assertEquals(2, changelogs.size(), "comments_ignored");
        } catch (ChangelogParseException e) {
            fail("comments_ignored: failed");
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.util.List)}.
     */
    @Test
    public void error_thrown_if_dates_out_of_order() {
        List<String> lines = Arrays.asList(
                "* Tue Feb 10 2015 George Washington",
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                "* Tue Feb 24 2015 George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt");
        try {
            changelogs = parser.parse(lines);
            fail("error thrown if dates out of order");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof DatesOutOfSequenceException);
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.util.List)}.
     */
    @Test
    public void errorThrownOnWrongDateFormat() {
        // 2/24/2015 was a Tuesday
        List<String> lines = Arrays.asList(
                "* 02/24/2015 George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt");
        try {
            changelogs = parser.parse(lines);
            fail("error thrown on wrong date format");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof InvalidChangelogDateException);
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.util.List)}.
     */
    @Test
    public void errorThrownOnIncorrectDayOfWeek() {
        // 2/24/2015 was a Tuesday
        List<String> lines = Arrays.asList(
                "* Wed Feb 24 2015 George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt");
        try {
            changelogs = parser.parse(lines);
            fail("error thrown on incorrect day of week");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof InvalidChangelogDateException);
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.util.List)}.
     */
    @Test
    public void errorThrownOnNoDescription() {
        List<String> lines = Arrays.asList(
                "* Tue Feb 24 2015 George Washington",
                "* Tue Feb 10 2015 George Washington");
        try {
            changelogs = parser.parse(lines);
            fail("error thrown on no description");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof IncompleteChangelogEntryException);
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(List)}.
     */
    @Test
    public void errorThrownOnNoInitialAsterisk() {
        List<String> lines = Arrays.asList(
                "Tue Feb 24 2015 George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt");
        try {
            changelogs = parser.parse(lines);
            fail("error thrown on no initial asterisk");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof NoInitialAsteriskException);
        }
    }


    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(List)}.
     */
    @Test
    public void errorThrownOnNoUserName() {
        List<String> lines = Arrays.asList(
                "* Tue Feb 24 2015",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt");
        try {
            changelogs = parser.parse(lines);
            fail("error thrown on no user name");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof IncompleteChangelogEntryException);
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(List)}.
     */
    @Test
    public void errorThrownOnNoUserNameOnFirstLine() {
        List<String> lines = Arrays.asList(
                "* Tue Feb 24 2015",
                "George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt");
        try {
            changelogs = parser.parse(lines);
            fail("error thrown on no user name on first line");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof IncompleteChangelogEntryException);
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.io.InputStream, java.nio.charset.Charset)}.
     */
    @Test
    public void parsesFileCorrectly() {
        try {
            changelogs = parser.parse(getClass().getResourceAsStream("changelog"), StandardCharsets.UTF_8);
            assertEquals(10, changelogs.size(), "parses file correctly");
        } catch (ChangelogParseException e) {
            fail("parses file correctly");
        } catch (IOException e) {
            fail("parses file correctly: " + e.getMessage());
        }
    }
}
