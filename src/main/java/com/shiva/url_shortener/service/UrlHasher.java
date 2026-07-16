package com.shiva.url_shortener.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Produces a stable hash of a normalized URL for indexed deduplication lookups.
 *
 * <p>SHA-256 is used to keep the indexed column fixed-width regardless of URL length.
 * Callers still compare the stored {@code longUrl} for exact equality after a hash match,
 * so a (practically impossible) hash collision cannot cause an incorrect dedup.
 */
@Component
public class UrlHasher {

    private static final String ALGORITHM = "SHA-256";

    /**
     * Computes the hex-encoded SHA-256 hash of the given normalized URL.
     *
     * @param normalizedUrl the normalized URL
     * @return a 64-character lowercase hex string
     */
    public String hash(final String normalizedUrl) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            final byte[] bytes = digest.digest(normalizedUrl.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (final NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present on every JVM; this is unreachable.
            throw new IllegalStateException(ALGORITHM + " algorithm not available", e);
        }
    }
}
