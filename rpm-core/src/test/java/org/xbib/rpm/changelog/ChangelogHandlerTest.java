package org.xbib.rpm.changelog;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.xbib.rpm.RpmBuilder;
import org.xbib.rpm.exception.ChangelogParseException;
import org.xbib.rpm.exception.NoInitialAsteriskException;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

/**
 *
 */
public class ChangelogHandlerTest {

    private RpmBuilder rpmBuilder;

    @Before
    public void setUp() throws Exception {
        rpmBuilder = new RpmBuilder();
    }

    @Test
    public void testAddChangeLog() {
        try {
            rpmBuilder.addChangelog(Paths.get("non.existent.file"));
            fail("non-existent file throws FileNotFoundException: not thrown");
        } catch (IOException e) {
            assertTrue("non-existent file exception", e instanceof NoSuchFileException);
        } catch (ChangelogParseException e) {
            fail("non-existent file throws FileNotFoundException: ChangelogParseException thrown instead");
        }
    }

    @Test
    public void testBadChangeLog() {
        try {
            rpmBuilder.addChangelog(getClass().getResource("bad.changelog"));
            fail("bad Changelog file throws ChangelogParseException: not thrown");
        } catch (IOException e) {
            fail("bad Changelog file throws ChangelogParseException: IOException thrown instead");
        } catch (ChangelogParseException e) {
            assertTrue("bad Changelog file throws ChangelogParseException", e instanceof NoInitialAsteriskException);
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(java.lang.String[])}.
     */
    @Test
    public void commentsIgnored() {
        try {
            rpmBuilder.addChangelog(getClass().getResource("changelog.with.comments"));
        } catch (IOException e) {
            fail("comments_ignored: IOException thrown instead");
        } catch (ChangelogParseException e) {
            fail("comments_ignored: failed");
        }
    }
}
