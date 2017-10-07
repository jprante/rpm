package org.xbib.rpm.exception;

/**
 * Invalid path.
 */
public class InvalidPathException extends RpmException {

    private static final long serialVersionUID = 8635626016855635561L;

    /**
     *
     * @param invalidPath Invalid path
     * @param cause       Exception cause
     */
    public InvalidPathException(String invalidPath, Throwable cause) {
        super(String.format("Path %s is invalid, causing exception", invalidPath), cause);
    }
}
