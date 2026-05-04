package com.codearena.codearena.controller;

import com.codearena.codearena.dto.ProblemRequest;
import com.codearena.codearena.dto.ProblemResponse;
import com.codearena.codearena.service.ProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST endpoints for managing coding problems.
 *
 * <p>This is the heart of Phase 2. A few annotations do the heavy lifting:
 * <ul>
 *   <li>{@code @RestController} = {@code @Controller} + {@code @ResponseBody}, so
 *       every method's return value is serialized straight to the HTTP response
 *       body (as JSON, via Jackson) instead of being treated as a view name.</li>
 *   <li>{@code @RequestMapping("/api/problems")} sets the base path shared by all
 *       methods.</li>
 *   <li>{@code @GetMapping}, {@code @PostMapping}, etc. map HTTP verbs to methods.</li>
 * </ul>
 *
 * <p>The controller stays "thin": it only translates between HTTP and Java and
 * delegates all real work to {@link ProblemService}. {@link ResponseEntity} is
 * used where we need explicit control over the status code (201, 204, 404).
 */
@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    /**
     * Constructor injection: Spring sees the single constructor and supplies the
     * {@code ProblemService} bean automatically. This is preferred over field
     * injection because it makes the dependency explicit and allows the field to
     * be {@code final}.
     */
    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    /** {@code GET /api/problems} — list all problems. */
    @GetMapping
    public List<ProblemResponse> getAllProblems() {
        return problemService.findAll();
    }

    /** {@code GET /api/problems/{id}} — fetch one problem, or 404 if missing. */
    @GetMapping("/{id}")
    public ResponseEntity<ProblemResponse> getProblemById(@PathVariable Long id) {
        return problemService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * {@code POST /api/problems} — create a problem.
     *
     * <p>Returns {@code 201 Created} together with a {@code Location} header
     * pointing at the new resource, which is the REST convention for creation.
     */
    @PostMapping
    public ResponseEntity<ProblemResponse> createProblem(@RequestBody ProblemRequest request) {
        ProblemResponse created = problemService.create(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** {@code PUT /api/problems/{id}} — replace a problem, or 404 if missing. */
    @PutMapping("/{id}")
    public ResponseEntity<ProblemResponse> updateProblem(@PathVariable Long id,
                                                         @RequestBody ProblemRequest request) {
        return problemService.update(id, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** {@code DELETE /api/problems/{id}} — remove a problem; 204 on success, 404 if missing. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProblem(@PathVariable Long id) {
        if (problemService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
