package com.codearena.codearena.exception;

/**
 * Thrown when registering a username that is already taken.
 *
 * <p>Mapped to {@code 409 Conflict} by the {@code GlobalExceptionHandler} — the
 * request is well-formed but conflicts with existing state, mirroring how
 * {@link DuplicateProblemTitleException} is handled.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("Username '" + username + "' is already taken");
    }
}
