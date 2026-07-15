package com.shiva.url_shortener.entity;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Persistent mapping between a short code and the original long URL.
 *
 * <p>The {@code code} column carries a UNIQUE constraint so that generated codes and
 * custom aliases share a single namespace and can never collide once persisted. The
 * {@code urlHash} column is indexed to support fast idempotent lookups when the same URL
 * is shortened again.
 */
@Entity
@Table(
        name = "url_mapping",
        indexes = {
            @Index(name = "idx_url_mapping_url_hash", columnList = "url_hash")
        }
)
public class UrlMapping {

    /** Maximum supported length of an original URL. */
    public static final int MAX_URL_LENGTH = 2048;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "long_url", nullable = false, length = MAX_URL_LENGTH)
    private String longUrl;

    @Column(name = "url_hash", nullable = false, length = 64)
    private String urlHash;

    @Column(name = "custom", nullable = false, updatable = false)
    private boolean custom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Protected no-arg constructor required by JPA. */
    protected UrlMapping() {
    }

    /**
     * Creates a new mapping.
     *
     * @param code    the short code or custom alias, must not be {@code null}
     * @param longUrl the original URL, must not be {@code null}
     * @param urlHash a stable hash of the normalized URL used for deduplication lookups,
     *                must not be {@code null}
     * @param custom  {@code true} if {@code code} is a user-supplied custom alias,
     *                {@code false} if it was auto-generated
     */
    public UrlMapping(final String code, final String longUrl, final String urlHash,
            final boolean custom) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.longUrl = Objects.requireNonNull(longUrl, "longUrl must not be null");
        this.urlHash = Objects.requireNonNull(urlHash, "urlHash must not be null");
        this.custom = custom;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public String getUrlHash() {
        return urlHash;
    }

    public boolean isCustom() {
        return custom;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
