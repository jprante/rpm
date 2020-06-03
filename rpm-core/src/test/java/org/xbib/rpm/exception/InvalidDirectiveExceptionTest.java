package org.xbib.rpm.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

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
