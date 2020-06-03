package org.xbib.rpm.changelog;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbib.rpm.RpmBuilder;
import org.xbib.rpm.exception.ChangelogParseException;
import org.xbib.rpm.exception.NoInitialAsteriskException;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.List;

/**
 *
 */
public class ChangelogHandlerTest {

    private RpmBuilder rpmBuilder;

    @BeforeEach
    public void setUp() {
        rpmBuilder = new RpmBuilder();
    }

    @Test
    public void testAddChangeLog() {
        try {
            rpmBuilder.addChangelog(Paths.get("non.existent.file"));
            fail("non-existent file throws FileNotFoundException: not thrown");
        } catch (IOException e) {
            assertTrue(e instanceof NoSuchFileException, "non-existent file exception");
        } catch (ChangelogParseException e) {
            fail("non-existent file throws FileNotFoundException: ChangelogParseException thrown instead");
        }
    }

    @Test
    public void testBadChangeLog() {
        try {
            rpmBuilder.addChangelog(getClass().getResource("bad.changelog").openStream());
            fail("bad Changelog file throws ChangelogParseException: not thrown");
        } catch (IOException e) {
            fail("bad Changelog file throws ChangelogParseException: IOException thrown instead");
        } catch (ChangelogParseException e) {
            assertTrue(e instanceof NoInitialAsteriskException, "bad Changelog file throws ChangelogParseException");
        }
    }

    /**
     * Test method for {@link org.xbib.rpm.changelog.ChangelogParser#parse(List)}.
     */
    @Test
    public void commentsIgnored() {
        try {
            rpmBuilder.addChangelog(getClass().getResource("changelog.with.comments").openStream());
        } catch (IOException e) {
            fail("comments_ignored: IOException thrown instead");
        } catch (ChangelogParseException e) {
            fail("comments_ignored: failed");
        }
    }
}
