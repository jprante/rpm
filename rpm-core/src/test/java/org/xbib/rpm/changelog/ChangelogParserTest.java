package org.xbib.rpm.changelog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.xbib.rpm.exception.ChangelogParseException;
import org.xbib.rpm.exception.DatesOutOfSequenceException;
import org.xbib.rpm.exception.IncompleteChangelogEntryException;
import org.xbib.rpm.exception.InvalidChangelogDateException;
import org.xbib.rpm.exception.NoInitialAsteriskException;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class ChangelogParserTest {

    private ChangelogParser parser;

    private List<ChangelogEntry> changelogs;

    @Before
    public void setUp() throws Exception {
        parser = new ChangelogParser();
    }

    @Test
    public void testParsesCorrectlyFormattedChangelog() {
        String[] lines = {
                "* Tue Feb 24 2015 George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt",
                "* Tue Feb 10 2015 George Washington",
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
        };
        try {
            changelogs = parser.parse(lines);
            assertEquals("parses correctly formatted Changelog", 2, changelogs.size());
        } catch (ChangelogParseException e) {
            fail("parses correctly formatted Changelog");
        }
    }

    @Test
    public void commentsIgnored() {
        String[] lines = {
                "# ORDER MUST BE DESCENDING (most recent change at top)",
                "* Tue Feb 24 2015 George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt",
                "* Tue Feb 10 2015 George Washington",
                "# a random comment",
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
        };

        try {
            changelogs = parser.parse(lines);
            assertEquals("comments_ignored", 2, changelogs.size());
        } catch (ChangelogParseException e) {
            fail("comments_ignored: failed");
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.lang.String[])}.
     */
    @Test
    public void error_thrown_if_dates_out_of_order() {
        String[] lines = {
                "* Tue Feb 10 2015 George Washington",
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                "* Tue Feb 24 2015 George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt"
        };

        try {
            changelogs = parser.parse(lines);
            fail("error thrown if dates out of order");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof DatesOutOfSequenceException);
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.lang.String[])}.
     */
    @Test
    public void errorThrownOnWrongDateFormat() {
        // 2/24/2015 was a Tuesday
        String[] lines = {
                "* 02/24/2015 George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt"
        };

        try {
            changelogs = parser.parse(lines);
            fail("error thrown on wrong date format");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof InvalidChangelogDateException);
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.lang.String[])}.
     */
    @Test
    public void errorThrownOnIncorrectDayOfWeek() {
        // 2/24/2015 was a Tuesday
        String[] lines = {
                "* Wed Feb 24 2015 George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt"
        };

        try {
            changelogs = parser.parse(lines);
            fail("error thrown on incorrect day of week");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof InvalidChangelogDateException);
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.lang.String[])}.
     */
    @Test
    public void errorThrownOnNoDescription() {
        String[] lines = {
                "* Tue Feb 24 2015 George Washington",
                "* Tue Feb 10 2015 George Washington",
        };

        try {
            changelogs = parser.parse(lines);
            fail("error thrown on no description");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof IncompleteChangelogEntryException);
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.lang.String[])}.
     */
    @Test
    public void errorThrownOnNoInitialAsterisk() {
        String[] lines = {
                "Tue Feb 24 2015 George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt"
        };

        try {
            changelogs = parser.parse(lines);
            fail("error thrown on no initial asterisk");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof NoInitialAsteriskException);
        }
    }


    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.lang.String[])}.
     */
    @Test
    public void errorThrownOnNoUserName() {
        String[] lines = {
                "* Tue Feb 24 2015",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt"
        };

        try {
            changelogs = parser.parse(lines);
            fail("error thrown on no user name");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof IncompleteChangelogEntryException);
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.lang.String[])}.
     */
    @Test
    public void errorThrownOnNoUserNameOnFirstLine() {
        String[] lines = {
                "* Tue Feb 24 2015",
                "George Washington",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt"
        };

        try {
            changelogs = parser.parse(lines);
            fail("error thrown on no user name on first line");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof IncompleteChangelogEntryException);
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.io.InputStream)}.
     */
    @Test
    public void parsesFileCorrectly() {
        try {
            changelogs = parser.parse(getClass().getResourceAsStream("changelog"));
            assertEquals("parses file correctly", 10, changelogs.size());
        } catch (ChangelogParseException e) {
            fail("parses file correctly");
        } catch (IOException e) {
            fail("parses file correctly: " + e.getMessage());
        }

    }
}
