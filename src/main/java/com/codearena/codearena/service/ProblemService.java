package com.codearena.codearena.service;

import com.codearena.codearena.dto.ProblemRequest;
import com.codearena.codearena.dto.ProblemResponse;
import com.codearena.codearena.dto.ProblemStatsResponse;
import com.codearena.codearena.model.Difficulty;
import com.codearena.codearena.model.Problem;
import com.codearena.codearena.repository.ProblemRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Business logic for managing {@link Problem}s.
 *
 * <p>Phase 3 reshapes this class around two ideas:
 * <ol>
 *   <li><strong>It no longer stores anything itself.</strong> Storage now lives
 *       behind {@link ProblemRepository}; the service depends on that interface,
 *       so the in-memory store can be replaced by JPA in Phase 4 without
 *       touching this class.</li>
 *   <li><strong>It owns the real business rules:</strong> normalizing input
 *       (trimming text, cleaning up tags, defaulting difficulty), filtering and
 *       searching, and computing statistics. These are decisions about
 *       <em>what the data should mean</em> — exactly what a service layer is
 *       for, and exactly what does not belong in a controller or a repository.</li>
 * </ol>
 */
@Service
public class ProblemService {

    /** Difficulty applied when a request does not specify one. */
    private static final Difficulty DEFAULT_DIFFICULTY = Difficulty.MEDIUM;

    private final ProblemRepository problemRepository;

    public ProblemService(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    /**
     * Seeds a couple of example problems on startup so the API has data to
     * return out of the box. Runs once, after the bean is constructed.
     */
    @PostConstruct
    void seedSampleData() {
        create(new ProblemRequest(
                "Two Sum",
                "Given an array of integers and a target, return indices of the two numbers that add up to the target.",
                Difficulty.EASY,
                List.of("array", "hash-table")));
        create(new ProblemRequest(
                "Longest Substring Without Repeating Characters",
                "Given a string, find the length of the longest substring without repeating characters.",
                Difficulty.MEDIUM,
                List.of("string", "sliding-window")));
    }

    /** Returns all problems, ordered by id. */
    public List<ProblemResponse> findAll() {
        return findProblems(null, null);
    }

    /**
     * Returns problems matching the optional filters, ordered by id.
     *
     * @param difficulty if non-null, only problems of this difficulty are kept
     * @param search     if non-blank, only problems whose title or tags contain
     *                   this (case-insensitive) text are kept
     */
    public List<ProblemResponse> findProblems(Difficulty difficulty, String search) {
        String needle = (search == null) ? "" : search.trim().toLowerCase(Locale.ROOT);
        return problemRepository.findAll().stream()
                .filter(problem -> difficulty == null || problem.getDifficulty() == difficulty)
                .filter(problem -> needle.isEmpty() || matches(problem, needle))
                .sorted(Comparator.comparing(Problem::getId))
                .map(this::toResponse)
                .toList();
    }

    /** Looks up a single problem by id, if it exists. */
    public Optional<ProblemResponse> findById(Long id) {
        return problemRepository.findById(id).map(this::toResponse);
    }

    /** Creates a new problem from a request, applying normalization rules. */
    public ProblemResponse create(ProblemRequest request) {
        Problem problem = Problem.builder()
                .title(normalizeText(request.getTitle()))
                .description(normalizeText(request.getDescription()))
                .difficulty(resolveDifficulty(request.getDifficulty()))
                .tags(normalizeTags(request.getTags()))
                .createdAt(Instant.now())
                .build();
        return toResponse(problemRepository.save(problem));
    }

    /**
     * Updates an existing problem's mutable fields (with the same normalization
     * applied on the way in). Returns {@link Optional#empty()} if no problem has
     * the given id, so the controller can translate that into a 404. The id and
     * {@code createdAt} are preserved.
     */
    public Optional<ProblemResponse> update(Long id, ProblemRequest request) {
        return problemRepository.findById(id).map(existing -> {
            existing.setTitle(normalizeText(request.getTitle()));
            existing.setDescription(normalizeText(request.getDescription()));
            existing.setDifficulty(resolveDifficulty(request.getDifficulty()));
            existing.setTags(normalizeTags(request.getTags()));
            return toResponse(problemRepository.save(existing));
        });
    }

    /** Deletes a problem. Returns {@code true} if something was actually removed. */
    public boolean delete(Long id) {
        return problemRepository.deleteById(id);
    }

    /** Computes catalogue statistics: total count and a breakdown by difficulty. */
    public ProblemStatsResponse getStats() {
        List<Problem> all = problemRepository.findAll();

        // EnumMap pre-seeded with zeros so every difficulty always appears.
        Map<Difficulty, Long> countByDifficulty = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            countByDifficulty.put(difficulty, 0L);
        }
        for (Problem problem : all) {
            if (problem.getDifficulty() != null) {
                countByDifficulty.merge(problem.getDifficulty(), 1L, Long::sum);
            }
        }

        return ProblemStatsResponse.builder()
                .total(all.size())
                .countByDifficulty(countByDifficulty)
                .build();
    }

    // ------------------------------------------------------------------
    // Business rules / helpers
    // ------------------------------------------------------------------

    /** True if the problem's title or any tag contains the (lower-cased) needle. */
    private boolean matches(Problem problem, String needle) {
        if (problem.getTitle() != null
                && problem.getTitle().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        List<String> tags = problem.getTags();
        if (tags == null) {
            return false;
        }
        return tags.stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains(needle));
    }

    /** Trims surrounding whitespace; leaves {@code null} as-is (validation is Phase 5). */
    private String normalizeText(String text) {
        return text == null ? null : text.trim();
    }

    /** Falls back to {@link #DEFAULT_DIFFICULTY} when none is supplied. */
    private Difficulty resolveDifficulty(Difficulty difficulty) {
        return difficulty == null ? DEFAULT_DIFFICULTY : difficulty;
    }

    /**
     * Cleans up tags: trims each, lower-cases for consistency, drops blanks, and
     * removes duplicates while preserving order. {@code null} becomes an empty
     * list so downstream code never has to null-check.
     */
    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        Set<String> cleaned = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String trimmed = tag.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        return new ArrayList<>(cleaned);
    }

    /** Maps the internal domain model to the outgoing response DTO. */
    private ProblemResponse toResponse(Problem problem) {
        return ProblemResponse.builder()
                .id(problem.getId())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .difficulty(problem.getDifficulty())
                .tags(problem.getTags())
                .createdAt(problem.getCreatedAt())
                .build();
    }
}
