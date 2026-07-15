package com.shiva.url_shortener.exception;

/**
 * Thrown when an incoming URL fails validation.
 */
public class InvalidUrlException extends RuntimeException {

    public InvalidUrlException(final String message) {
        super(message);
    }
}
