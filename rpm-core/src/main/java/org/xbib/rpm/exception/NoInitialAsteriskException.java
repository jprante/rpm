package org.xbib.rpm.exception;

/**
 * This exception is when a change log entry does not begin with an asterisk.
 */
public class NoInitialAsteriskException extends ChangelogParseException {

    private static final long serialVersionUID = -3822813453286191743L;

    public NoInitialAsteriskException() {
        super();
    }
}
