package org.xbib.rpm.exception;

/**
 * This exception is thrown when parsing of the changelog file results in an incomplete ChangeLogEntry.
 */
public class IncompleteChangelogEntryException extends ChangelogParseException {

    private static final long serialVersionUID = -2868781181942971960L;

    public IncompleteChangelogEntryException() {
        super();
    }

}
