package org.xbib.rpm.exception;

/**
 * Signing key file not found exception.
 */
public class SigningKeyNotFoundException extends RpmException {

    private static final long serialVersionUID = -8186767059791996113L;

    /**
     * Constructor.
     *
     * @param signingKey Signing key
     */
    public SigningKeyNotFoundException(String signingKey) {
        super(String.format("Signing key %s could not be found", signingKey));
    }
}
