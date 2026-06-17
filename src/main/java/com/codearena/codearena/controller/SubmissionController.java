package com.codearena.codearena.controller;

import com.codearena.codearena.dto.SubmissionRequest;
import com.codearena.codearena.dto.SubmissionResponse;
import com.codearena.codearena.dto.SubmissionStatusUpdateRequest;
import com.codearena.codearena.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST endpoints for submissions.
 *
 * <p>The current user comes from the {@link Authentication} injected by Spring
 * Security (populated from the JWT). {@code authentication.getName()} is the
 * username, which the service uses to set the submitter and to enforce ownership.
 * The controller stays thin — it passes the username through and lets the service
 * decide what's allowed.
 */
@RestController
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /** {@code POST /api/problems/{problemId}/submissions} — submit a solution (201). */
    @PostMapping("/api/problems/{problemId}/submissions")
    public ResponseEntity<SubmissionResponse> submit(@PathVariable Long problemId,
                                                     @Valid @RequestBody SubmissionRequest request,
                                                     Authentication authentication) {
        SubmissionResponse created = submissionService.create(problemId, request, authentication.getName());
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/submissions/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** {@code GET /api/problems/{problemId}/submissions} — admins see all; users see their own. */
    @GetMapping("/api/problems/{problemId}/submissions")
    public List<SubmissionResponse> listForProblem(@PathVariable Long problemId, Authentication authentication) {
        return submissionService.listForProblem(problemId, authentication.getName());
    }

    /** {@code GET /api/submissions/me} — the current user's submissions. */
    @GetMapping("/api/submissions/me")
    public List<SubmissionResponse> listMine(Authentication authentication) {
        return submissionService.listMine(authentication.getName());
    }

    /** {@code GET /api/submissions/{id}} — one submission (owner or admin only; else 403). */
    @GetMapping("/api/submissions/{id}")
    public SubmissionResponse getOne(@PathVariable Long id, Authentication authentication) {
        return submissionService.getById(id, authentication.getName());
    }

    /** {@code PUT /api/submissions/{id}/status} — admin "judge" the submission. */
    @PutMapping("/api/submissions/{id}/status")
    public SubmissionResponse updateStatus(@PathVariable Long id,
                                           @Valid @RequestBody SubmissionStatusUpdateRequest request) {
        return submissionService.updateStatus(id, request.getStatus());
    }
}
