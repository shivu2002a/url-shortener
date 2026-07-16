package com.shiva.url_shortener.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.shiva.url_shortener.exception.AliasAlreadyExistsException;
import com.shiva.url_shortener.exception.CodeGenerationException;
import com.shiva.url_shortener.exception.CodeNotFoundException;
import com.shiva.url_shortener.exception.InvalidAliasException;
import com.shiva.url_shortener.exception.InvalidUrlException;
import com.shiva.url_shortener.web.dto.ErrorResponse;

/**
 * Translates domain exceptions into HTTP responses with a consistent error body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation failures for URLs and aliases as {@code 400 Bad Request}.
     */
    @ExceptionHandler({InvalidUrlException.class, InvalidAliasException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(final RuntimeException ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles unknown short codes as {@code 404 Not Found}.
     */
    @ExceptionHandler(CodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(final CodeNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles alias conflicts as {@code 409 Conflict}.
     */
    @ExceptionHandler(AliasAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(final AliasAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles exhausted code generation as {@code 500 Internal Server Error}.
     */
    @ExceptionHandler(CodeGenerationException.class)
    public ResponseEntity<ErrorResponse> handleGenerationFailure(final CodeGenerationException ex) {
        log.error("Code generation exhausted: {}", ex.getMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> build(final HttpStatus status, final String message) {
        final ErrorResponse body =
                new ErrorResponse(status.value(), status.getReasonPhrase(), message);
        return ResponseEntity.status(status).body(body);
    }
}
