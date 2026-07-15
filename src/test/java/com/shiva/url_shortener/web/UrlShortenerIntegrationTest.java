package com.shiva.url_shortener.web;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiva.url_shortener.web.dto.ShortenRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end tests covering the shorten -> redirect round-trip, unknown codes, duplicate
 * URL deduplication, and custom-alias behaviour.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shortenThenRedirectRoundTripsWith301() throws Exception {
        final String longUrl = "https://example.com/some/long/path?ref=1";

        final MvcResult result = mockMvc.perform(shortenRequest(longUrl, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.longUrl", is(longUrl)))
                .andExpect(jsonPath("$.created", is(true)))
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.shortUrl").isNotEmpty())
                .andReturn();

        final String code = readJson(result, "code");

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", longUrl));
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        mockMvc.perform(get("/{code}", "doesNotExist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateUrlReturnsSameCodeIdempotently() throws Exception {
        final String longUrl = "https://dedup.example.com/page";

        final MvcResult first = mockMvc.perform(shortenRequest(longUrl, null))
                .andExpect(status().isCreated())
                .andReturn();
        final String firstCode = readJson(first, "code");

        mockMvc.perform(shortenRequest(longUrl, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created", is(false)))
                .andExpect(jsonPath("$.code", is(firstCode)));
    }

    @Test
    void customAliasIsHonoured() throws Exception {
        final String alias = "myCustomAlias";
        final String longUrl = "https://example.com/custom";

        mockMvc.perform(shortenRequest(longUrl, alias))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is(alias)))
                .andExpect(jsonPath("$.shortUrl", endsWith("/" + alias)));

        mockMvc.perform(get("/{code}", alias))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", longUrl));
    }

    @Test
    void takenAliasReturns409() throws Exception {
        final String alias = "conflictAlias";

        mockMvc.perform(shortenRequest("https://example.com/first", alias))
                .andExpect(status().isCreated());

        mockMvc.perform(shortenRequest("https://example.com/second", alias))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidUrlReturns400() throws Exception {
        mockMvc.perform(shortenRequest("ftp://example.com", null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidAliasReturns400() throws Exception {
        mockMvc.perform(shortenRequest("https://example.com/x", "bad alias!"))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            shortenRequest(final String url, final String alias) throws Exception {
        return post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ShortenRequest(url, alias)));
    }

    private String readJson(final MvcResult result, final String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(field).asText();
    }
}
