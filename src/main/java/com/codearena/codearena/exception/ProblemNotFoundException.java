package com.codearena.codearena.exception;

/**
 * Thrown when a problem is requested by an id that does not exist.
 *
 * <p>It extends {@link RuntimeException} (an <em>unchecked</em> exception) so the
 * service can signal "not found" without forcing every caller to declare or
 * catch it. The {@code GlobalExceptionHandler} catches it centrally and maps it
 * to a {@code 404 Not Found} response — keeping the controller free of
 * not-found plumbing.
 */
public class ProblemNotFoundException extends RuntimeException {

    public ProblemNotFoundException(Long id) {
        super("Problem not found with id: " + id);
    }
}
