package com.shiva.url_shortener.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.shiva.url_shortener.config.ShortCodeProperties;

/**
 * A {@link ShortCodeGenerator} strategy that produces random Base62 codes.
 *
 * <p>The alphabet {@code [A-Za-z0-9]} is URL-safe, so codes never require percent-encoding.
 * With a keyspace of 62^n (where {@code n} is the configured length) the probability of two
 * independent calls producing the same code is negligible; the definitive guarantee comes
 * from the UNIQUE constraint on the persisted code, so a rare duplicate simply fails to
 * insert and the caller retries. Uniqueness is therefore enforced by the datastore, not by
 * assuming randomness never repeats.
 *
 * <p>{@link SecureRandom} is used so codes are not predictable or enumerable. The length is
 * supplied via {@link ShortCodeProperties}, keeping this strategy configurable.
 */
@Component
public class Base62ShortCodeGenerator implements ShortCodeGenerator {

    /** URL-safe Base62 alphabet. */
    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private final SecureRandom random = new SecureRandom();
    private final int codeLength;

    /**
     * Creates a generator using the configured code length.
     *
     * @param properties short-code configuration
     */
    public Base62ShortCodeGenerator(final ShortCodeProperties properties) {
        this.codeLength = properties.length();
    }

    @Override
    public String generate() {
        final StringBuilder builder = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            builder.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }
}
