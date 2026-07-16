package com.shiva.url_shortener.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.shiva.url_shortener.entity.UrlMapping;
import com.shiva.url_shortener.exception.AliasAlreadyExistsException;
import com.shiva.url_shortener.exception.CodeGenerationException;
import com.shiva.url_shortener.exception.CodeNotFoundException;
import com.shiva.url_shortener.repository.UrlMappingRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates URL shortening and resolution.
 *
 * <p>Design decisions:
 * <ul>
 *   <li><b>Collision safety</b> — auto-generated codes are persisted under a UNIQUE
 *       constraint. On the rare insert conflict the code is regenerated, up to
 *       {@value #MAX_GENERATION_ATTEMPTS} times, so a collision can never be persisted.</li>
 *   <li><b>Duplicate URLs</b> — a plain shorten request (no alias) is idempotent: if the
 *       same normalized URL already has an auto-generated code, that code is returned
 *       instead of minting a new one.</li>
 *   <li><b>Custom aliases</b> — always create a new mapping, so one URL may have several
 *       codes. A taken alias is rejected rather than silently replaced.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);

    /** Maximum number of code regeneration attempts before giving up. */
    private static final int MAX_GENERATION_ATTEMPTS = 5;

    @NonNull
    private final UrlMappingRepository repository;
    @NonNull
    private final ShortCodeGenerator codeGenerator;
    @NonNull
    private final UrlValidator urlValidator;
    @NonNull
    private final AliasValidator aliasValidator;
    @NonNull
    private final UrlHasher urlHasher;

    /**
     * Shortens a URL, optionally under a custom alias.
     *
     * @param rawUrl the URL to shorten
     * @param alias  an optional custom alias; ignored when null or blank
     * @return the shorten outcome, including whether a new mapping was created
     * @throws com.shiva.url_shortener.exception.InvalidUrlException   if the URL is invalid
     * @throws com.shiva.url_shortener.exception.InvalidAliasException if the alias is invalid
     * @throws AliasAlreadyExistsException                            if the alias is taken
     * @throws CodeGenerationException                                if no unique code could
     *                                                                be generated
     */
    @Transactional
    public ShortenResult shorten(final String rawUrl, final String alias) {
        final String normalizedUrl = urlValidator.validateAndNormalize(rawUrl);
        final String urlHash = urlHasher.hash(normalizedUrl);

        if (StringUtils.hasText(alias)) {
            return createWithAlias(alias, normalizedUrl, urlHash);
        }
        return createWithGeneratedCode(normalizedUrl, urlHash);
    }

    /**
     * Resolves a short code to its original URL.
     *
     * @param code the short code or custom alias
     * @return the original (normalized) URL
     * @throws CodeNotFoundException if no mapping exists for the code
     */
    @Transactional(readOnly = true)
    public String resolve(final String code) {
        return repository.findByCode(code)
                .map(UrlMapping::getLongUrl)
                .orElseThrow(() -> {
                    log.debug("Resolution failed: no mapping for code={}", code);
                    return new CodeNotFoundException("No mapping for code: " + code);
                });
    }

    private ShortenResult createWithAlias(
            final String alias, final String normalizedUrl, final String urlHash) {
        aliasValidator.validate(alias);
        if (repository.existsByCode(alias)) {
            log.warn("Custom alias conflict: alias={} is already in use", alias);
            throw new AliasAlreadyExistsException("Alias already in use: " + alias);
        }
        try {
            final UrlMapping saved =
                    repository.saveAndFlush(new UrlMapping(alias, normalizedUrl, urlHash, true));
            log.info("Created mapping with custom alias: code={}", alias);
            return new ShortenResult(saved, true);
        } catch (final DataIntegrityViolationException e) {
            log.warn("Concurrent alias conflict: alias={}", alias);
            throw new AliasAlreadyExistsException("Alias already in use: " + alias);
        }
    }

    private ShortenResult createWithGeneratedCode(
            final String normalizedUrl, final String urlHash) {
        final Optional<UrlMapping> existing =
                repository.findFirstByUrlHashAndCustomFalseOrderByIdAsc(urlHash)
                        .filter(mapping -> mapping.getLongUrl().equals(normalizedUrl));
        if (existing.isPresent()) {
            log.debug("Deduplication hit: returning existing code={} for urlHash={}",
                    existing.get().getCode(), urlHash);
            return new ShortenResult(existing.get(), false);
        }

        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            final String code = codeGenerator.generate();
            try {
                final UrlMapping saved = repository.saveAndFlush(
                        new UrlMapping(code, normalizedUrl, urlHash, false));
                log.info("Created mapping: code={}, attempt={}", code, attempt);
                return new ShortenResult(saved, true);
            } catch (final DataIntegrityViolationException e) {
                log.warn("Code collision on attempt {}/{}: code={}", attempt,
                        MAX_GENERATION_ATTEMPTS, code);
            }
        }
        log.error("Exhausted {} code generation attempts for urlHash={}", MAX_GENERATION_ATTEMPTS,
                urlHash);
        throw new CodeGenerationException(
                "Failed to generate a unique code after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }
}
