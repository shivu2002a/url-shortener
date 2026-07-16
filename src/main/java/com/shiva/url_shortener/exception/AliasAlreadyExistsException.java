package com.shiva.url_shortener.exception;

/**
 * Thrown when a requested custom alias is already in use.
 */
public class AliasAlreadyExistsException extends RuntimeException {

    public AliasAlreadyExistsException(final String message) {
        super(message);
    }
}
