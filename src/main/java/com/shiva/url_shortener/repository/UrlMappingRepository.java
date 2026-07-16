package com.shiva.url_shortener.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shiva.url_shortener.entity.UrlMapping;

/**
 * Data-access operations for {@link UrlMapping} entities.
 */
@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    /**
     * Finds a mapping by its short code or custom alias.
     *
     * @param code the code to look up
     * @return the matching mapping, if present
     */
    Optional<UrlMapping> findByCode(String code);

    /**
     * Checks whether a code is already in use.
     *
     * @param code the code to check
     * @return {@code true} if a mapping with this code exists
     */
    boolean existsByCode(String code);

    /**
     * Finds the first auto-generated mapping for a given URL hash, used for idempotent
     * deduplication of repeated shorten requests. Custom aliases are excluded so that
     * deduplication only ever returns auto-generated codes.
     *
     * @param urlHash the hash of the normalized URL
     * @return the earliest auto-generated mapping for this URL, if any
     */
    Optional<UrlMapping> findFirstByUrlHashAndCustomFalseOrderByIdAsc(String urlHash);
}
