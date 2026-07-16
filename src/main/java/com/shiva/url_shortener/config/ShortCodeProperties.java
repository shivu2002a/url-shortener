package com.shiva.url_shortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Externalized configuration for short-code generation, bound from the {@code app.shortcode}
 * property namespace.
 *
 * <p>Externalizing the length keeps the {@link com.shiva.url_shortener.service.ShortCodeGenerator}
 * strategy tunable without code changes and makes the keyspace an explicit, documented
 * decision rather than a buried constant.
 *
 * @param length number of characters in a generated code; bounded to keep codes both
 *               URL-friendly and large enough to avoid frequent collisions
 */
@ConfigurationProperties(prefix = "app.shortcode")
public record ShortCodeProperties(@DefaultValue("7") int length) {

    /** Smallest permitted code length. */
    public static final int MIN_LENGTH = 4;

    /** Largest permitted code length. */
    public static final int MAX_LENGTH = 32;

    /**
     * Validates the bound values on construction so misconfiguration fails fast at startup.
     */
    public ShortCodeProperties {
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "app.shortcode.length must be between " + MIN_LENGTH + " and " + MAX_LENGTH
                            + " but was " + length);
        }
    }
}
