package com.shiva.url_shortener.web.dto;

/**
 * Standard error payload returned for failed requests.
 *
 * @param status  the HTTP status code
 * @param error   a short machine-readable error label
 * @param message a human-readable description
 */
public record ErrorResponse(int status, String error, String message) {
}
