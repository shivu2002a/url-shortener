package com.shiva.url_shortener.web.dto;

/**
 * Request body for {@code POST /shorten}.
 *
 * @param url   the long URL to shorten (required)
 * @param alias an optional custom alias; when present the service creates a mapping using
 *              this alias instead of an auto-generated code
 */
public record ShortenRequest(String url, String alias) {
}
