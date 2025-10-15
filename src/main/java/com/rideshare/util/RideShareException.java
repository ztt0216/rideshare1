package com.rideshare.util;

public class RideShareException extends RuntimeException {
    public RideShareException(String message) {
        super(message);
    }

    public RideShareException(String message, Throwable cause) {
        super(message, cause);
    }
}
