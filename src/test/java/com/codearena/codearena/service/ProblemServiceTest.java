package com.codearena.codearena.service;

import com.codearena.codearena.dto.ProblemRequest;
import com.codearena.codearena.dto.ProblemResponse;
import com.codearena.codearena.dto.ProblemStatsResponse;
import com.codearena.codearena.exception.DuplicateProblemTitleException;
import com.codearena.codearena.exception.ProblemNotFoundException;
import com.codearena.codearena.model.Difficulty;
import com.codearena.codearena.model.Problem;
import com.codearena.codearena.repository.ProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Pure unit tests for {@link ProblemService} business logic.
 *
 * <p>No Spring context and no database: the {@link ProblemRepository} is a
 * Mockito mock backed by a plain {@link Map}, so {@code save}/{@code findById}/
 * etc. behave like a tiny in-memory store. This keeps the tests fast and
 * focused on the service's logic (normalization, filtering, stats) while
 * treating persistence as a collaborator. Each test starts with an empty store,
 * which makes count assertions deterministic.
 */
@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock
    private ProblemRepository repository;

    private ProblemService service;

    private Map<Long, Problem> store;
    private AtomicLong sequence;

    @BeforeEach
    void setUp() {
        store = new LinkedHashMap<>();
        sequence = new AtomicLong(0);

        // save(): assign an id on insert, then store the problem.
        lenient().when(repository.save(any(Problem.class))).thenAnswer(invocation -> {
            Problem problem = invocation.getArgument(0);
            if (problem.getId() == null) {
                problem.setId(sequence.incrementAndGet());
            }
            store.put(problem.getId(), problem);
            return problem;
        });
        lenient().when(repository.findAll())
                .thenAnswer(invocation -> new ArrayList<>(store.values()));
        lenient().when(repository.findById(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.<Long>getArgument(0))));
        lenient().when(repository.findByDifficulty(any())).thenAnswer(invocation -> {
            Difficulty difficulty = invocation.getArgument(0);
            return store.values().stream()
                    .filter(problem -> problem.getDifficulty() == difficulty)
                    .toList();
        });
        lenient().when(repository.existsById(anyLong()))
                .thenAnswer(invocation -> store.containsKey(invocation.<Long>getArgument(0)));
        lenient().when(repository.existsByTitleIgnoreCase(anyString())).thenAnswer(invocation -> {
            String title = invocation.getArgument(0);
            return store.values().stream()
                    .anyMatch(p -> p.getTitle() != null && p.getTitle().equalsIgnoreCase(title));
        });
        lenient().when(repository.existsByTitleIgnoreCaseAndIdNot(anyString(), anyLong())).thenAnswer(invocation -> {
            String title = invocation.getArgument(0);
            Long id = invocation.getArgument(1);
            return store.values().stream()
                    .anyMatch(p -> p.getTitle() != null
                            && p.getTitle().equalsIgnoreCase(title)
                            && !p.getId().equals(id));
        });
        lenient().doAnswer(invocation -> {
            store.remove(invocation.<Long>getArgument(0));
            return null;
        }).when(repository).deleteById(anyLong());

        service = new ProblemService(repository);
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
    void getById_returnsProblem() {
        ProblemResponse created = service.create(
                new ProblemRequest("Lookup", "d", Difficulty.EASY, null));

        assertThat(service.getById(created.getId()).getTitle()).isEqualTo("Lookup");
    }

    @Test
    void getById_throws_whenMissing() {
        assertThatThrownBy(() -> service.getById(404L))
                .isInstanceOf(ProblemNotFoundException.class);
    }

    @Test
    void create_throwsDuplicate_whenTitleAlreadyExists() {
        service.create(new ProblemRequest("Two Sum", "d", Difficulty.EASY, null));

        // Same title, different case -> still a duplicate.
        assertThatThrownBy(() -> service.create(
                new ProblemRequest("two sum", "d", Difficulty.HARD, null)))
                .isInstanceOf(DuplicateProblemTitleException.class);
    }

    @Test
    void update_replacesFields_andPreservesId() {
        ProblemResponse created = service.create(
                new ProblemRequest("Old", "old", Difficulty.EASY, List.of("x")));

        ProblemResponse updated = service.update(created.getId(),
                new ProblemRequest("New", "new", Difficulty.HARD, List.of("y")));

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getTitle()).isEqualTo("New");
        assertThat(updated.getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(updated.getTags()).containsExactly("y");
    }

    @Test
    void update_throws_whenMissing() {
        assertThatThrownBy(() -> service.update(404L,
                new ProblemRequest("x", "x", Difficulty.EASY, null)))
                .isInstanceOf(ProblemNotFoundException.class);
    }

    @Test
    void delete_succeedsThenThrows_whenDeletedAgain() {
        ProblemResponse created = service.create(
                new ProblemRequest("Temp", "d", Difficulty.EASY, null));

        service.delete(created.getId());
        assertThatThrownBy(() -> service.delete(created.getId()))
                .isInstanceOf(ProblemNotFoundException.class);
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
