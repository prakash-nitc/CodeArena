package com.codearena.codearena.service;

import com.codearena.codearena.dto.ProblemRequest;
import com.codearena.codearena.dto.ProblemResponse;
import com.codearena.codearena.dto.ProblemStatsResponse;
import com.codearena.codearena.model.Difficulty;
import com.codearena.codearena.repository.InMemoryProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link ProblemService} business logic.
 *
 * <p>No Spring context is started: we construct the service directly with a real
 * {@link InMemoryProblemRepository}. Because {@code @PostConstruct} seeding only
 * runs inside the Spring container, each test begins with an empty store, which
 * makes assertions on counts fully deterministic.
 */
class ProblemServiceTest {

    private ProblemService service;

    @BeforeEach
    void setUp() {
        service = new ProblemService(new InMemoryProblemRepository());
    }

    @Test
    void create_assignsIdAndTimestamp() {
        ProblemResponse created = service.create(
                new ProblemRequest("Two Sum", "desc", Difficulty.EASY, List.of("array")));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Two Sum");
    }

    @Test
    void create_normalizesTags_lowercasesTrimsAndDeduplicates() {
        ProblemResponse created = service.create(new ProblemRequest(
                "Graph", "desc", Difficulty.HARD,
                Arrays.asList("  Graph ", "graph", "BFS", "", "  ")));

        // "Graph"/"graph" collapse to one; blanks dropped; order preserved.
        assertThat(created.getTags()).containsExactly("graph", "bfs");
    }

    @Test
    void create_defaultsDifficultyToMedium_whenNull() {
        ProblemResponse created = service.create(
                new ProblemRequest("No difficulty", "desc", null, List.of()));

        assertThat(created.getDifficulty()).isEqualTo(Difficulty.MEDIUM);
    }

    @Test
    void create_trimsTitle() {
        ProblemResponse created = service.create(
                new ProblemRequest("   Padded Title   ", "desc", Difficulty.EASY, null));

        assertThat(created.getTitle()).isEqualTo("Padded Title");
        assertThat(created.getTags()).isEmpty();
    }

    @Test
    void findProblems_filtersByDifficulty() {
        service.create(new ProblemRequest("A", "d", Difficulty.EASY, null));
        service.create(new ProblemRequest("B", "d", Difficulty.HARD, null));
        service.create(new ProblemRequest("C", "d", Difficulty.EASY, null));

        List<ProblemResponse> easy = service.findProblems(Difficulty.EASY, null);

        assertThat(easy).hasSize(2);
        assertThat(easy).allMatch(p -> p.getDifficulty() == Difficulty.EASY);
    }

    @Test
    void findProblems_searchesTitleAndTagsCaseInsensitively() {
        service.create(new ProblemRequest("Binary Tree Paths", "d", Difficulty.MEDIUM, List.of("tree")));
        service.create(new ProblemRequest("Two Sum", "d", Difficulty.EASY, List.of("array", "hash-table")));

        assertThat(service.findProblems(null, "TREE")).hasSize(1);
        assertThat(service.findProblems(null, "hash")).hasSize(1);
        assertThat(service.findProblems(null, "nope")).isEmpty();
    }

    @Test
    void findProblems_combinesDifficultyAndSearch() {
        service.create(new ProblemRequest("Tree A", "d", Difficulty.EASY, List.of("tree")));
        service.create(new ProblemRequest("Tree B", "d", Difficulty.HARD, List.of("tree")));

        List<ProblemResponse> result = service.findProblems(Difficulty.HARD, "tree");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Tree B");
    }

    @Test
    void update_replacesFields_andPreservesId() {
        ProblemResponse created = service.create(
                new ProblemRequest("Old", "old", Difficulty.EASY, List.of("x")));

        Optional<ProblemResponse> updated = service.update(created.getId(),
                new ProblemRequest("New", "new", Difficulty.HARD, List.of("y")));

        assertThat(updated).isPresent();
        assertThat(updated.get().getId()).isEqualTo(created.getId());
        assertThat(updated.get().getTitle()).isEqualTo("New");
        assertThat(updated.get().getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(updated.get().getTags()).containsExactly("y");
    }

    @Test
    void update_returnsEmpty_whenMissing() {
        assertThat(service.update(404L, new ProblemRequest("x", "x", Difficulty.EASY, null)))
                .isEmpty();
    }

    @Test
    void delete_returnsTrueThenFalse() {
        ProblemResponse created = service.create(
                new ProblemRequest("Temp", "d", Difficulty.EASY, null));

        assertThat(service.delete(created.getId())).isTrue();
        assertThat(service.delete(created.getId())).isFalse();
    }

    @Test
    void getStats_countsTotalAndPerDifficulty() {
        service.create(new ProblemRequest("A", "d", Difficulty.EASY, null));
        service.create(new ProblemRequest("B", "d", Difficulty.EASY, null));
        service.create(new ProblemRequest("C", "d", Difficulty.HARD, null));

        ProblemStatsResponse stats = service.getStats();

        assertThat(stats.getTotal()).isEqualTo(3);
        assertThat(stats.getCountByDifficulty())
                .containsEntry(Difficulty.EASY, 2L)
                .containsEntry(Difficulty.MEDIUM, 0L)
                .containsEntry(Difficulty.HARD, 1L);
    }
}
