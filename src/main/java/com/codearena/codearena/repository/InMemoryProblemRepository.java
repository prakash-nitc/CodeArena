package com.codearena.codearena.repository;

import com.codearena.codearena.model.Problem;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory {@link ProblemRepository} backed by a {@link ConcurrentHashMap}.
 *
 * <p>This is the Phase 3 stand-in for a real database. It owns the storage and
 * id generation that previously lived inside {@code ProblemService} — moving
 * them here is the whole point of the repository pattern: the service no longer
 * knows or cares <em>how</em> problems are stored.
 *
 * <p>{@code @Repository} registers it as a Spring bean and marks it as a
 * persistence component (which also enables Spring's data-access exception
 * translation once a real datastore is wired in). Being a shared singleton, it
 * uses thread-safe primitives ({@code ConcurrentHashMap} + {@code AtomicLong}).
 */
@Repository
public class InMemoryProblemRepository implements ProblemRepository {

    private final Map<Long, Problem> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(0);

    @Override
    public List<Problem> findAll() {
        // Defensive copy so callers can't mutate the backing store directly.
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<Problem> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Problem save(Problem problem) {
        if (problem.getId() == null) {
            problem.setId(idSequence.incrementAndGet());
        }
        store.put(problem.getId(), problem);
        return problem;
    }

    @Override
    public boolean deleteById(Long id) {
        return store.remove(id) != null;
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }
}
