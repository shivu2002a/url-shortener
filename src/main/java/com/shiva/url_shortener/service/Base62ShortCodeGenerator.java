package com.shiva.url_shortener.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Generates random {@value #CODE_LENGTH}-character Base62 codes.
 *
 * <p>The alphabet {@code [A-Za-z0-9]} is URL-safe, so codes never require percent-encoding.
 * With a keyspace of 62^{@value #CODE_LENGTH} (~3.5 trillion) the probability of two
 * independent calls producing the same code is negligible; the definitive guarantee comes
 * from the UNIQUE constraint on the persisted code, so a rare duplicate simply fails to
 * insert and the caller retries. Uniqueness is therefore enforced by the datastore, not by
 * assuming randomness never repeats.
 *
 * <p>{@link SecureRandom} is used so codes are not predictable or enumerable.
 */
@Component
public class Base62ShortCodeGenerator implements ShortCodeGenerator {

    /** URL-safe Base62 alphabet. */
    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    /** Number of characters in a generated code. */
    private static final int CODE_LENGTH = 7;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        final StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }
}
