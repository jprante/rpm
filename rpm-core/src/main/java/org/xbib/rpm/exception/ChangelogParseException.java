package org.xbib.rpm.exception;

/**
 * Exceptions thrown by the ChangeLogParser.
 */
public abstract class ChangelogParseException extends RpmException {

    private static final long serialVersionUID = 3874782829969316647L;

    public ChangelogParseException() {
        super();
    }

    public ChangelogParseException(Exception e) {
        super(e);
    }

    public ChangelogParseException(String message) {
        super(message);
    }
}
