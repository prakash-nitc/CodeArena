package com.codearena.codearena.model;

/**
 * A user's role, which determines what they're allowed to do.
 *
 * <p>Spring Security expresses roles as authorities prefixed with {@code ROLE_}
 * (so {@code ADMIN} becomes the authority {@code ROLE_ADMIN}). Keeping the set
 * fixed as an enum mirrors {@link Difficulty} and keeps authorization checks
 * type-safe.
 */
public enum Role {
    /** Regular authenticated user: may create and update problems. */
    USER,

    /** Administrator: everything a user can do, plus deleting problems. */
    ADMIN
}
