package org.xbib.rpm.exception;

/**
 * Invalid RPM directive exception.
 */
public class InvalidDirectiveException extends RpmException {

    private static final long serialVersionUID = -7183149225882563869L;

    /**
     * Constructor.
     *
     * @param invalidDirective Invalid directive name
     */
    public InvalidDirectiveException(String invalidDirective) {
        super(String.format("RPM directive '%s' invalid", invalidDirective));
    }
}
