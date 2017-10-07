package org.xbib.rpm.exception;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 */
public class InvalidPathExceptionTest {

    @Test
    public void exception() {
        Exception cause = new Exception("cause");
        InvalidPathException ex  = new InvalidPathException("invalid/path", cause);
        assertEquals("Path invalid/path is invalid, causing exception", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
