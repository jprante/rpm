package org.xbib.rpm.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class SigningKeyNotFoundExceptionTest {

    @Test
    public void testException() {
        SigningKeyNotFoundException exception = new SigningKeyNotFoundException("keyfile");
        assertEquals("Signing key keyfile could not be found", exception.getMessage());
    }
}
