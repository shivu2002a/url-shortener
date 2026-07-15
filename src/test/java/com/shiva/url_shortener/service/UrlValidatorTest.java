package com.shiva.url_shortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.shiva.url_shortener.entity.UrlMapping;
import com.shiva.url_shortener.exception.InvalidUrlException;

class UrlValidatorTest {

    private final UrlValidator validator = new UrlValidator();

    @Test
    void acceptsAndPreservesValidHttpsUrl() {
        final String url = "https://example.com/path?q=1#frag";
        assertThat(validator.validateAndNormalize(url)).isEqualTo(url);
    }

    @Test
    void lowercasesSchemeAndHost() {
        assertThat(validator.validateAndNormalize("HTTPS://Example.COM/Path"))
                .isEqualTo("https://example.com/Path");
    }

    @Test
    void stripsDefaultPorts() {
        assertThat(validator.validateAndNormalize("http://example.com:80/a"))
                .isEqualTo("http://example.com/a");
        assertThat(validator.validateAndNormalize("https://example.com:443/a"))
                .isEqualTo("https://example.com/a");
    }

    @Test
    void keepsNonDefaultPort() {
        assertThat(validator.validateAndNormalize("http://example.com:8080/a"))
                .isEqualTo("http://example.com:8080/a");
    }

    @Test
    void dropsTrailingSlashOnEmptyPath() {
        assertThat(validator.validateAndNormalize("https://example.com/"))
                .isEqualTo("https://example.com");
    }

    @Test
    void rejectsBlankUrl() {
        assertThatThrownBy(() -> validator.validateAndNormalize("  "))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void rejectsUnsupportedScheme() {
        assertThatThrownBy(() -> validator.validateAndNormalize("ftp://example.com"))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void rejectsMissingHost() {
        assertThatThrownBy(() -> validator.validateAndNormalize("http:///path"))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void rejectsUrlExceedingMaxLength() {
        final String tooLong = "https://example.com/" + "a".repeat(UrlMapping.MAX_URL_LENGTH);
        assertThatThrownBy(() -> validator.validateAndNormalize(tooLong))
                .isInstanceOf(InvalidUrlException.class);
    }
}
