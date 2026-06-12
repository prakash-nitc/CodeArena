package com.codearena.codearena.repository;

/**
 * Interface-based projection for a leaderboard row.
 *
 * <p>Spring Data maps the aliased columns of the leaderboard query
 * ({@code ... as username}, {@code ... as solvedCount}) onto these getters,
 * returning a lightweight view instead of full entities. The API-facing
 * {@code LeaderboardEntry} DTO (which also carries a rank) is built from these
 * rows in the service.
 */
public interface LeaderboardRow {

    String getUsername();

    long getSolvedCount();
}
