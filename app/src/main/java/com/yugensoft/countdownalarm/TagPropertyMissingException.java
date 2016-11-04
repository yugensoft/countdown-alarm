package com.yugensoft.countdownalarm;

public class TagPropertyMissingException extends Exception {
    public TagPropertyMissingException() {

    }

    public TagPropertyMissingException(String message) {
        super (message);
    }

    public TagPropertyMissingException(Throwable cause) {
        super (cause);
    }

    public TagPropertyMissingException(String message, Throwable cause) {
        super (message, cause);
    }
}