package com.codearena.codearena.repository;

import com.codearena.codearena.model.Problem;

import java.util.List;
import java.util.Optional;

/**
 * Persistence abstraction for {@link Problem}s.
 *
 * <p>This interface is the seam between the service layer (business logic) and
 * however data actually gets stored. In Phase 3 the only implementation is
 * {@link InMemoryProblemRepository}; in Phase 4 a Spring Data JPA repository will
 * take its place. Because the service depends on <em>this interface</em> rather
 * than a concrete store, that swap requires no changes to the business logic
 * above it.
 *
 * <p>The method names and semantics deliberately mirror Spring Data's
 * {@code CrudRepository} (notably {@code save} assigns an id on first insert),
 * so the eventual migration is close to a drop-in replacement.
 */
public interface ProblemRepository {

    /** Returns all stored problems. */
    List<Problem> findAll();

    /** Finds a problem by id, if present. */
    Optional<Problem> findById(Long id);

    /**
     * Inserts or updates a problem. If the problem's id is {@code null}, a new id
     * is assigned (insert); otherwise the existing entry is overwritten (update).
     *
     * @return the saved problem, including any newly assigned id
     */
    Problem save(Problem problem);

    /**
     * Deletes the problem with the given id.
     *
     * @return {@code true} if a problem was actually removed
     */
    boolean deleteById(Long id);

    /** Returns whether a problem with the given id exists. */
    boolean existsById(Long id);
}
