package org.xbib.rpm.exception;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 */
public class InvalidDirectiveExceptionTest {
    @Test
    public void exception() {
        InvalidDirectiveException ex
                = new InvalidDirectiveException("directive");
        assertEquals("RPM directive 'directive' invalid", ex.getMessage());
    }
}
