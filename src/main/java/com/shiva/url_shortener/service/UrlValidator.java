package com.shiva.url_shortener.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.shiva.url_shortener.entity.UrlMapping;
import com.shiva.url_shortener.exception.InvalidUrlException;

/**
 * Validates and normalizes incoming URLs.
 *
 * <p>Validation is intentionally strict: only absolute {@code http}/{@code https} URLs with
 * a host are accepted, and the raw length is capped at {@link UrlMapping#MAX_URL_LENGTH}.
 *
 * <p>Normalization produces a canonical form used both for storage and for the
 * deduplication hash. It lowercases the scheme and host, strips the default port for the
 * scheme, and removes a trailing slash from an otherwise empty path. Path, query, and
 * fragment are preserved as-is because they can be case- and character-sensitive.
 */
@Component
public class UrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final int HTTP_DEFAULT_PORT = 80;
    private static final int HTTPS_DEFAULT_PORT = 443;

    /**
     * Validates the raw URL and returns its normalized canonical form.
     *
     * @param rawUrl the URL supplied by the client
     * @return the normalized URL
     * @throws InvalidUrlException if the URL is blank, too long, malformed, or uses an
     *                             unsupported scheme
     */
    public String validateAndNormalize(final String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            throw new InvalidUrlException("URL must not be blank");
        }
        final String trimmed = rawUrl.trim();
        if (trimmed.length() > UrlMapping.MAX_URL_LENGTH) {
            throw new InvalidUrlException(
                    "URL exceeds maximum length of " + UrlMapping.MAX_URL_LENGTH + " characters");
        }

        final URI uri = parse(trimmed);
        final String scheme = requireScheme(uri);
        final String host = requireHost(uri);

        return normalize(scheme, host, uri);
    }

    private URI parse(final String value) {
        try {
            return new URI(value);
        } catch (final URISyntaxException e) {
            throw new InvalidUrlException("URL is malformed: " + e.getReason());
        }
    }

    private String requireScheme(final URI uri) {
        final String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new InvalidUrlException("URL scheme must be http or https");
        }
        return scheme.toLowerCase(Locale.ROOT);
    }

    private String requireHost(final URI uri) {
        final String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            throw new InvalidUrlException("URL must include a host");
        }
        return host.toLowerCase(Locale.ROOT);
    }

    private String normalize(final String scheme, final String host, final URI uri) {
        final StringBuilder normalized = new StringBuilder(scheme).append("://").append(host);

        final int port = uri.getPort();
        if (isNonDefaultPort(scheme, port)) {
            normalized.append(':').append(port);
        }

        final String path = uri.getRawPath();
        if (StringUtils.hasText(path) && !"/".equals(path)) {
            normalized.append(path);
        }

        if (uri.getRawQuery() != null) {
            normalized.append('?').append(uri.getRawQuery());
        }
        if (uri.getRawFragment() != null) {
            normalized.append('#').append(uri.getRawFragment());
        }
        return normalized.toString();
    }

    private boolean isNonDefaultPort(final String scheme, final int port) {
        if (port == -1) {
            return false;
        }
        if ("http".equals(scheme) && port == HTTP_DEFAULT_PORT) {
            return false;
        }
        return !("https".equals(scheme) && port == HTTPS_DEFAULT_PORT);
    }
}
