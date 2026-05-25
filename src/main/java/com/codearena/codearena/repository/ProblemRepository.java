package com.codearena.codearena.repository;

import com.codearena.codearena.model.Difficulty;
import com.codearena.codearena.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Problem}s.
 *
 * <p>This is the Phase 4 replacement for the hand-written in-memory repository.
 * By extending {@link JpaRepository}, we inherit a full set of persistence
 * operations for free — {@code findAll}, {@code findById}, {@code save},
 * {@code deleteById}, {@code existsById}, {@code count}, paging, sorting, and
 * more — implemented by Spring Data at runtime. We write no SQL and no
 * implementation class.
 *
 * <p>Because the Phase 3 service was written against a repository abstraction,
 * this swap is almost transparent to it: the method names line up, and only the
 * delete signature differs (Spring Data's {@code deleteById} returns {@code void}).
 *
 * <p>The {@code findBy.../existsBy...} methods are <em>derived query methods</em>:
 * Spring Data parses the method name and generates the query automatically (no
 * SQL, no {@code @Query}).
 */
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    /** Returns all problems with the given difficulty (query derived from the name). */
    List<Problem> findByDifficulty(Difficulty difficulty);

    /** Whether any problem already has this title (case-insensitive). Used for create. */
    boolean existsByTitleIgnoreCase(String title);

    /**
     * Whether a <em>different</em> problem (not {@code id}) already has this title
     * (case-insensitive). Used on update so a problem can keep its own title.
     */
    boolean existsByTitleIgnoreCaseAndIdNot(String title, Long id);
}
