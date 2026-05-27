package com.codearena.codearena.controller;

import com.codearena.codearena.dto.ProblemRequest;
import com.codearena.codearena.dto.ProblemResponse;
import com.codearena.codearena.dto.ProblemStatsResponse;
import com.codearena.codearena.model.Difficulty;
import com.codearena.codearena.service.ProblemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST endpoints for managing coding problems.
 *
 * <p>After Phase 5 the controller is even thinner. It no longer deals with
 * "not found" itself: the service throws {@link com.codearena.codearena.exception.ProblemNotFoundException}
 * (and {@link com.codearena.codearena.exception.DuplicateProblemTitleException}),
 * and the {@code GlobalExceptionHandler} converts those into 404/409 responses.
 * The {@code @Valid} annotation makes Spring validate request bodies before the
 * method runs, producing a 400 with field errors when input is malformed.
 */
@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    /**
     * {@code GET /api/problems} — list problems, optionally filtered.
     *
     * <p>Both query parameters are optional and combine (logical AND):
     * {@code ?difficulty=EASY} and {@code ?search=tree} (title/tag contains,
     * case-insensitive). With no parameters it returns every problem.
     */
    @GetMapping
    public List<ProblemResponse> getAllProblems(
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String search) {
        return problemService.findProblems(difficulty, search);
    }

    /** {@code GET /api/problems/stats} — catalogue statistics (total + per-difficulty counts). */
    @GetMapping("/stats")
    public ProblemStatsResponse getStats() {
        return problemService.getStats();
    }

    /** {@code GET /api/problems/{id}} — fetch one problem (404 if it doesn't exist). */
    @GetMapping("/{id}")
    public ProblemResponse getProblemById(@PathVariable Long id) {
        return problemService.getById(id);
    }

    /**
     * {@code POST /api/problems} — create a problem.
     *
     * <p>Returns {@code 201 Created} with a {@code Location} header. Invalid input
     * → 400; a duplicate title → 409.
     */
    @PostMapping
    public ResponseEntity<ProblemResponse> createProblem(@Valid @RequestBody ProblemRequest request) {
        ProblemResponse created = problemService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** {@code PUT /api/problems/{id}} — replace a problem (404 if missing, 409 on title clash). */
    @PutMapping("/{id}")
    public ProblemResponse updateProblem(@PathVariable Long id,
                                         @Valid @RequestBody ProblemRequest request) {
        return problemService.update(id, request);
    }

    /** {@code DELETE /api/problems/{id}} — remove a problem; 204 on success, 404 if missing. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProblem(@PathVariable Long id) {
        problemService.delete(id);
    }
}
