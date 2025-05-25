package com.example.commonutils.exceptions;

public class PermanentException extends CustomApplicationException {
    public PermanentException(String message, Throwable cause) {
        super(message, cause);
    }

    public PermanentException(String message) {
        super(message);
    }
}
