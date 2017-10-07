package org.xbib.rpm.exception;

/**
 * Unknown architecture exception.
 */
public class UnknownArchitectureException extends RpmException {

    private static final long serialVersionUID = -7624086302466294147L;

    /**
     * Constructor.
     *
     * @param unknownArchitecture Unknown architecture name
     */
    public UnknownArchitectureException(String unknownArchitecture) {
        super(String.format("Unknown architecture '%s'", unknownArchitecture));
    }

    /**
     * Constructor.
     *
     * @param unknownArchitecture Unknown architecture name
     * @param cause               Exception cause
     */
    public UnknownArchitectureException(String unknownArchitecture, Throwable cause) {
        super(String.format("Unknown architecture '%s'", unknownArchitecture), cause);
    }
}
