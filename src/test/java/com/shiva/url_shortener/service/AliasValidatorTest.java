package com.shiva.url_shortener.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.shiva.url_shortener.exception.InvalidAliasException;

class AliasValidatorTest {

    private final AliasValidator validator = new AliasValidator();

    @Test
    void acceptsValidBase62Alias() {
        assertThatCode(() -> validator.validate("myAlias123")).doesNotThrowAnyException();
    }

    @Test
    void rejectsNullAlias() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(InvalidAliasException.class);
    }

    @Test
    void rejectsTooShortAlias() {
        assertThatThrownBy(() -> validator.validate("ab"))
                .isInstanceOf(InvalidAliasException.class);
    }

    @Test
    void rejectsAliasWithIllegalCharacters() {
        assertThatThrownBy(() -> validator.validate("bad-alias"))
                .isInstanceOf(InvalidAliasException.class);
    }
}
