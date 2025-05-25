package com.example.commonutils.exceptions;

public class TransientException extends CustomApplicationException {
    public TransientException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransientException(String message) {
        super(message);
    }
}
