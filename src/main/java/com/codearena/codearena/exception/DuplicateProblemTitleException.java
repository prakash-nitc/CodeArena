package com.codearena.codearena.exception;

/**
 * Thrown when creating or updating a problem would result in a duplicate title.
 *
 * <p>This represents a violated <em>business rule</em> (titles must be unique,
 * case-insensitively), as opposed to malformed input. The
 * {@code GlobalExceptionHandler} maps it to {@code 409 Conflict} — the status
 * that means "the request is valid but conflicts with the current state of the
 * resource".
 */
public class DuplicateProblemTitleException extends RuntimeException {

    public DuplicateProblemTitleException(String title) {
        super("A problem with the title '" + title + "' already exists");
    }
}
