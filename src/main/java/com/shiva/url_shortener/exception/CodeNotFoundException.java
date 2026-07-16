package com.shiva.url_shortener.exception;

/**
 * Thrown when no mapping exists for a requested short code.
 */
public class CodeNotFoundException extends RuntimeException {

    public CodeNotFoundException(final String message) {
        super(message);
    }
}
