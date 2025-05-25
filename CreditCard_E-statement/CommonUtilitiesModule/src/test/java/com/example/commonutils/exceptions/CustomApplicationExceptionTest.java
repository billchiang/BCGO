package com.example.commonutils.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomApplicationExceptionTest {

    @Test
    void testExceptionWithMessage() {
        String message = "Test application exception";
        CustomApplicationException ex = new CustomApplicationException(message);
        assertEquals(message, ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void testExceptionWithMessageAndCause() {
        String message = "Test application exception with cause";
        Throwable cause = new RuntimeException("Root cause");
        CustomApplicationException ex = new CustomApplicationException(message, cause);
        assertEquals(message, ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testTransientException() {
        String message = "Test transient exception";
        Throwable cause = new IOException("Temporary IO issue");
        TransientException ex = new TransientException(message, cause);
        assertEquals(message, ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertTrue(ex instanceof CustomApplicationException);
    }

    @Test
    void testPermanentException() {
        String message = "Test permanent exception";
        Throwable cause = new IllegalArgumentException("Invalid argument");
        PermanentException ex = new PermanentException(message, cause);
        assertEquals(message, ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertTrue(ex instanceof CustomApplicationException);
    }
}
