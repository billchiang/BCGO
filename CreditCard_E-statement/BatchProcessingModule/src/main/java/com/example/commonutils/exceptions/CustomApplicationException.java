package com.example.commonutils.exceptions;

public class CustomApplicationException extends RuntimeException {
    public CustomApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CustomApplicationException(String message) {
        super(message);
    }
}
