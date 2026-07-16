package com.shiva.url_shortener.exception;

/**
 * Thrown when a unique short code could not be generated within the allowed number of
 * attempts. In practice this should never happen given the size of the code keyspace;
 * it exists so the failure is explicit rather than silent.
 */
public class CodeGenerationException extends RuntimeException {

    public CodeGenerationException(final String message) {
        super(message);
    }
}
