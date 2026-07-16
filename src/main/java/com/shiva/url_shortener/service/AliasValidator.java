package com.shiva.url_shortener.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.shiva.url_shortener.exception.InvalidAliasException;

/**
 * Validates user-supplied custom aliases.
 *
 * <p>Aliases share the same URL-safe Base62 alphabet as generated codes and must be
 * between {@value #MIN_LENGTH} and {@value #MAX_LENGTH} characters. Keeping aliases in the
 * same alphabet and namespace as generated codes guarantees they can be looked up and
 * constrained identically.
 */
@Component
public class AliasValidator {

    /** Minimum alias length. */
    public static final int MIN_LENGTH = 3;

    /** Maximum alias length. */
    public static final int MAX_LENGTH = 32;

    private static final Pattern ALIAS_PATTERN =
            Pattern.compile("^[A-Za-z0-9]{" + MIN_LENGTH + "," + MAX_LENGTH + "}$");

    /**
     * Validates an alias.
     *
     * @param alias the requested alias
     * @throws InvalidAliasException if the alias is null, wrong length, or contains
     *                               characters outside the Base62 alphabet
     */
    public void validate(final String alias) {
        if (alias == null || !ALIAS_PATTERN.matcher(alias).matches()) {
            throw new InvalidAliasException(
                    "Alias must be " + MIN_LENGTH + "-" + MAX_LENGTH
                            + " characters using only letters and digits");
        }
    }
}
