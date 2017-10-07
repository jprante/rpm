package org.xbib.rpm.exception;

/**
 * This exception is thrown when the date portion of a change log can not be parsed.
 */
public class InvalidChangelogDateException extends ChangelogParseException {

    private static final long serialVersionUID = 2684845962721950707L;

    public InvalidChangelogDateException(Exception e) {
        super(e);
    }

    public InvalidChangelogDateException(String message) {
        super(message);
    }

}
