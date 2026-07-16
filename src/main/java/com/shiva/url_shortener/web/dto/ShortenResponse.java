package com.shiva.url_shortener.web.dto;

/**
 * Response body for {@code POST /shorten}.
 *
 * @param code     the short code or custom alias
 * @param shortUrl the fully-qualified short URL that redirects to {@code longUrl}
 * @param longUrl  the original URL (normalized)
 * @param created  {@code true} if a new mapping was created, {@code false} if an existing
 *                 mapping was returned via idempotent deduplication
 */
public record ShortenResponse(String code, String shortUrl, String longUrl, boolean created) {
}
