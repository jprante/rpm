package org.xbib.rpm.exception;

/**
 * Exceptions thrown by the RPM builder or reader.
 */
public class RpmException extends Exception {

    private static final long serialVersionUID = -7205164781605944414L;

    public RpmException() {
        super();
    }

    public RpmException(Throwable t) {
        super(t);
    }

    public RpmException(String message) {
        super(message);
    }

    public RpmException(String message, Throwable cause) {
        super(message, cause);
    }

}
