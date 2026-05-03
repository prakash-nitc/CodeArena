package com.codearena.codearena.service;

import com.codearena.codearena.dto.ProblemRequest;
import com.codearena.codearena.dto.ProblemResponse;
import com.codearena.codearena.model.Difficulty;
import com.codearena.codearena.model.Problem;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Business logic for managing {@link Problem}s.
 *
 * <p>In Phase 2 there is no database, so problems are kept in an in-memory
 * {@link ConcurrentHashMap}. This is deliberately swappable: when JPA arrives in
 * Phase 4, this class will delegate to a Spring Data repository instead, while
 * the controller above it stays unchanged. That is the whole point of a service
 * layer — it isolates the rest of the app from how data is actually stored.
 *
 * <p>{@code @Service} marks this as a Spring-managed singleton bean, so it can be
 * injected into the controller. Because a singleton is shared across all HTTP
 * threads, the store uses thread-safe types ({@code ConcurrentHashMap} +
 * {@code AtomicLong}).
 */
@Service
public class ProblemService {

    /** Thread-safe in-memory store keyed by problem id. */
    private final Map<Long, Problem> store = new ConcurrentHashMap<>();

    /** Generates monotonically increasing ids in a thread-safe way. */
    private final AtomicLong idSequence = new AtomicLong(0);

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

    /** Returns every problem currently stored. */
    public List<ProblemResponse> findAll() {
        return store.values().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Looks up a single problem by id, if it exists. */
    public Optional<ProblemResponse> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(this::toResponse);
    }

    /** Creates a new problem, assigning it a fresh id and creation timestamp. */
    public ProblemResponse create(ProblemRequest request) {
        long id = idSequence.incrementAndGet();
        Problem problem = Problem.builder()
                .id(id)
                .title(request.getTitle())
                .description(request.getDescription())
                .difficulty(request.getDifficulty())
                .tags(request.getTags())
                .createdAt(Instant.now())
                .build();
        store.put(id, problem);
        return toResponse(problem);
    }

    /**
     * Updates an existing problem's mutable fields. Returns
     * {@link Optional#empty()} if no problem has the given id, so the controller
     * can translate that into a 404.
     */
    public Optional<ProblemResponse> update(Long id, ProblemRequest request) {
        Problem existing = store.get(id);
        if (existing == null) {
            return Optional.empty();
        }
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setDifficulty(request.getDifficulty());
        existing.setTags(request.getTags());
        return Optional.of(toResponse(existing));
    }

    /** Deletes a problem. Returns {@code true} if something was actually removed. */
    public boolean delete(Long id) {
        return store.remove(id) != null;
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
