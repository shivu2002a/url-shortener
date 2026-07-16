package com.shiva.url_shortener.service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.shiva.url_shortener.config.ShortCodeProperties;

class Base62ShortCodeGeneratorTest {

    private static final Pattern BASE62_SEVEN = Pattern.compile("^[A-Za-z0-9]{7}$");

    private final ShortCodeGenerator generator =
            new Base62ShortCodeGenerator(new ShortCodeProperties(7));

    @Test
    void generatesSevenCharacterBase62Codes() {
        for (int i = 0; i < 1_000; i++) {
            final String code = generator.generate();
            assertThat(code).hasSize(7);
            assertThat(BASE62_SEVEN.matcher(code).matches())
                    .as("code %s should be 7 URL-safe Base62 characters", code)
                    .isTrue();
        }
    }

    @Test
    void producesNoDuplicatesAcrossManyGenerations() {
        final int iterations = 100_000;
        final Set<String> seen = new HashSet<>(iterations * 2);

        int collisions = 0;
        for (int i = 0; i < iterations; i++) {
            if (!seen.add(generator.generate())) {
                collisions++;
            }
        }

        assertThat(collisions)
                .as("expected no collisions across %d generations in a 62^7 keyspace", iterations)
                .isZero();
    }
}
