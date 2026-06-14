package com.codearena.codearena.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row of the leaderboard: a user, how many distinct problems they've had
 * accepted, and their 1-based rank. The rank is assigned by the service after
 * the database returns rows already ordered by {@code solvedCount}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {

    private int rank;

    private String username;

    private long solvedCount;
}
