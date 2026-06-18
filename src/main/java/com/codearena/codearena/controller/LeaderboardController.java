package com.codearena.codearena.controller;

import com.codearena.codearena.dto.LeaderboardEntry;
import com.codearena.codearena.service.SubmissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only leaderboard endpoint.
 *
 * <p>Ranks users by the number of distinct problems they've had accepted. It's a
 * public view (anyone can see the standings), computed on demand from submission
 * data via an aggregation query.
 */
@RestController
public class LeaderboardController {

    private final SubmissionService submissionService;

    public LeaderboardController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /** {@code GET /api/leaderboard} — users ranked by distinct problems accepted. */
    @GetMapping("/api/leaderboard")
    public List<LeaderboardEntry> leaderboard() {
        return submissionService.leaderboard();
    }
}
