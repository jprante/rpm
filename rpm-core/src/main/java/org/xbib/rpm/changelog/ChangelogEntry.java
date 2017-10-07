package org.xbib.rpm.changelog;

import java.time.Instant;

/**
 * This class defines a Plain Old Java Object encapsulating one entry in a Changelog. For example:
 * Wed Nov 08 2006 George Washington
 * - Add the foo feature
 * Add the bar feature
 */
public class ChangelogEntry {
    /**
     * The date portion of the Changelog entry.
     * In the above Example: Wed Nov 08 2006
     */
    private Instant changeLogTime;
    /**
     * The "user" or "name" portion of the Changelog entry.
     * In the above Example: George Washington
     * in other words, the rest of the first line of the entry,
     * not counting the date portion
     */
    private String userMakingChange;
    /**
     * Freeform text on the second line and beyond of the Changelog entry.
     * In the above Example:
     * - Add the foo feature
     * Add the bar feature
     * Terminates with a line beginning with an asterisk, which defines a new Changelog entry.
     */
    private String description;

    public ChangelogEntry() {
    }

    public boolean isComplete() {
        return changeLogTime != null && userMakingChange != null && description != null;
    }

    public Instant getChangeLogTime() {
        return changeLogTime;
    }

    public void setChangeLogTime(Instant changeLogTime) {
        this.changeLogTime = changeLogTime;
    }

    public String getUserMakingChange() {
        return userMakingChange;
    }

    public void setUserMakingChange(String userMakingChange) {
        this.userMakingChange = userMakingChange;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
