package com.codearena.codearena.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Core domain model for a coding problem on the platform.
 *
 * <p>In Phase 2 this is a plain Java object held in memory by
 * {@code ProblemService}. It is intentionally <em>not</em> a JPA entity yet —
 * database persistence is introduced in Phase 4, at which point this class (or
 * a dedicated entity) will gain {@code @Entity}/{@code @Id} annotations.
 *
 * <p>The Lombok annotations generate boilerplate at compile time:
 * <ul>
 *   <li>{@code @Data} → getters, setters, {@code equals}, {@code hashCode}, {@code toString}</li>
 *   <li>{@code @Builder} → a fluent builder ({@code Problem.builder()...build()})</li>
 *   <li>{@code @NoArgsConstructor} / {@code @AllArgsConstructor} → constructors</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Problem {

    /** Unique identifier, assigned by the service when the problem is created. */
    private Long id;

    /** Short human-readable title, e.g. "Two Sum". */
    private String title;

    /** Full problem statement / description. */
    private String description;

    /** How hard the problem is. */
    private Difficulty difficulty;

    /** Free-form labels for filtering/search, e.g. ["array", "hash-table"]. */
    private List<String> tags;

    /** Server-side creation timestamp. */
    private Instant createdAt;
}
