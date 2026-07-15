package com.shiva.url_shortener.service;

import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.shiva.url_shortener.entity.UrlMapping;
import com.shiva.url_shortener.exception.AliasAlreadyExistsException;
import com.shiva.url_shortener.exception.CodeGenerationException;
import com.shiva.url_shortener.exception.CodeNotFoundException;
import com.shiva.url_shortener.repository.UrlMappingRepository;

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
public class UrlShortenerService {

    /** Maximum number of code regeneration attempts before giving up. */
    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final UrlMappingRepository repository;
    private final ShortCodeGenerator codeGenerator;
    private final UrlValidator urlValidator;
    private final AliasValidator aliasValidator;
    private final UrlHasher urlHasher;

    public UrlShortenerService(
            final UrlMappingRepository repository,
            final ShortCodeGenerator codeGenerator,
            final UrlValidator urlValidator,
            final AliasValidator aliasValidator,
            final UrlHasher urlHasher) {
        this.repository = Objects.requireNonNull(repository);
        this.codeGenerator = Objects.requireNonNull(codeGenerator);
        this.urlValidator = Objects.requireNonNull(urlValidator);
        this.aliasValidator = Objects.requireNonNull(aliasValidator);
        this.urlHasher = Objects.requireNonNull(urlHasher);
    }

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
                .orElseThrow(() -> new CodeNotFoundException("No mapping for code: " + code));
    }

    private ShortenResult createWithAlias(
            final String alias, final String normalizedUrl, final String urlHash) {
        aliasValidator.validate(alias);
        if (repository.existsByCode(alias)) {
            throw new AliasAlreadyExistsException("Alias already in use: " + alias);
        }
        try {
            final UrlMapping saved =
                    repository.saveAndFlush(new UrlMapping(alias, normalizedUrl, urlHash, true));
            return new ShortenResult(saved, true);
        } catch (final DataIntegrityViolationException e) {
            // Lost a race with a concurrent request for the same alias.
            throw new AliasAlreadyExistsException("Alias already in use: " + alias);
        }
    }

    private ShortenResult createWithGeneratedCode(
            final String normalizedUrl, final String urlHash) {
        final Optional<UrlMapping> existing =
                repository.findFirstByUrlHashAndCustomFalseOrderByIdAsc(urlHash)
                        .filter(mapping -> mapping.getLongUrl().equals(normalizedUrl));
        if (existing.isPresent()) {
            return new ShortenResult(existing.get(), false);
        }

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            final String code = codeGenerator.generate();
            try {
                final UrlMapping saved = repository.saveAndFlush(
                        new UrlMapping(code, normalizedUrl, urlHash, false));
                return new ShortenResult(saved, true);
            } catch (final DataIntegrityViolationException e) {
                // Generated code collided with an existing one; try again.
            }
        }
        throw new CodeGenerationException(
                "Failed to generate a unique code after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }
}
