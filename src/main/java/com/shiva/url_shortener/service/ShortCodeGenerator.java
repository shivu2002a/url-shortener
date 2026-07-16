package com.shiva.url_shortener.service;

/**
 * Generates short, URL-safe codes for new mappings.
 */
public interface ShortCodeGenerator {

    /**
     * Generates a single candidate short code. The code is not guaranteed to be unique;
     * callers must persist it under a UNIQUE constraint and retry on conflict.
     *
     * @return a freshly generated URL-safe code
     */
    String generate();
}
