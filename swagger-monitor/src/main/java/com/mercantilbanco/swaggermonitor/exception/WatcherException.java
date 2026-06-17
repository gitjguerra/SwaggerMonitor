package com.mercantilbanco.swaggermonitor.exception;

public class WatcherException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WatcherException(String message) {
        super(message);
    }

    public WatcherException(String message, Throwable cause) {
        super(message, cause);
    }
}