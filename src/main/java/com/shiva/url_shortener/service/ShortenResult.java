package com.shiva.url_shortener.service;

import com.shiva.url_shortener.entity.UrlMapping;

/**
 * Outcome of a shorten operation.
 *
 * @param mapping the resulting mapping (newly created or pre-existing)
 * @param created {@code true} if a new mapping was persisted, {@code false} if an existing
 *                mapping was returned through idempotent deduplication
 */
public record ShortenResult(UrlMapping mapping, boolean created) {
}
