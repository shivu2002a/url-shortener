package com.shiva.url_shortener.web;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.shiva.url_shortener.entity.UrlMapping;
import com.shiva.url_shortener.service.ShortenResult;
import com.shiva.url_shortener.service.UrlShortenerService;
import com.shiva.url_shortener.web.dto.ShortenRequest;
import com.shiva.url_shortener.web.dto.ShortenResponse;

import lombok.RequiredArgsConstructor;

/**
 * REST endpoints for shortening URLs and redirecting short codes.
 */
@RestController
@RequiredArgsConstructor
public class UrlShortenerController {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerController.class);

    private final UrlShortenerService service;

    /**
     * Shortens a URL. Returns {@code 201 Created} for a new mapping or {@code 200 OK} when
     * an existing mapping is returned via idempotent deduplication.
     *
     * @param request the shorten request
     * @return the shorten response
     */
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@RequestBody final ShortenRequest request) {
        log.debug("POST /shorten - url={}, alias={}", request.url(), request.alias());
        final ShortenResult result = service.shorten(request.url(), request.alias());
        final UrlMapping mapping = result.mapping();

        final String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(mapping.getCode())
                .toUriString();

        final ShortenResponse body = new ShortenResponse(
                mapping.getCode(), shortUrl, mapping.getLongUrl(), result.created());
        final HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        log.debug("POST /shorten - responding {} code={}", status.value(), mapping.getCode());
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Redirects a short code to its original URL with a {@code 301 Moved Permanently},
     * or returns {@code 404 Not Found} for an unknown code.
     *
     * @param code the short code or custom alias
     * @return a 301 redirect to the original URL
     */
    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable final String code) {
        log.debug("GET /{} - resolving", code);
        final String longUrl = service.resolve(code);
        log.debug("GET /{} - redirecting to {}", code, longUrl);
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(longUrl))
                .build();
    }
}
