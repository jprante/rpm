package org.xbib.rpm.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class UnknownOperatingSystemExceptionTest {

    @Test
    public void exception() {
        UnknownOperatingSystemException ex
                = new UnknownOperatingSystemException("unknown");
        assertEquals("Unknown operating system 'unknown'", ex.getMessage());
    }

    @Test
    public void exceptionWithCause() {
        Exception cause = new Exception("cause");
        UnknownOperatingSystemException ex
                = new UnknownOperatingSystemException("unknown", cause);
        assertEquals("Unknown operating system 'unknown'", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
