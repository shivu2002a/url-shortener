package com.shiva.url_shortener.exception;

/**
 * Thrown when a requested custom alias does not satisfy the alias format rules.
 */
public class InvalidAliasException extends RuntimeException {

    public InvalidAliasException(final String message) {
        super(message);
    }
}
