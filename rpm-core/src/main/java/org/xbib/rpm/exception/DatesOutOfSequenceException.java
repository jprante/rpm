package org.xbib.rpm.exception;

/**
 * This exception is thrown when Changelog entries are not in descending order by date.
 */

public class DatesOutOfSequenceException extends ChangelogParseException {

    private static final long serialVersionUID = -4917148052703360535L;

    public DatesOutOfSequenceException() {
        super();
    }

}
