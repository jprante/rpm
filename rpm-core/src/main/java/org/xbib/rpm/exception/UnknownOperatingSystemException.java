package org.xbib.rpm.exception;

/**
 * Unknown operating system exception.
 */
public class UnknownOperatingSystemException extends RpmException {

    private static final long serialVersionUID = 1939077648201714453L;

    /**
     * Constructor.
     *
     * @param unknownOperatingSystem Unknown operating system
     */
    public UnknownOperatingSystemException(String unknownOperatingSystem) {
        super(String.format("Unknown operating system '%s'", unknownOperatingSystem));
    }

    /**
     * Constructor.
     *
     * @param unknownOperatingSystem Unknown operating system
     * @param cause                  Exception cause
     */
    public UnknownOperatingSystemException(String unknownOperatingSystem, Throwable cause) {
        super(String.format("Unknown operating system '%s'", unknownOperatingSystem), cause);
    }
}
