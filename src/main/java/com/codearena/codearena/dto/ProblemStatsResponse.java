package com.codearena.codearena.dto;

import com.codearena.codearena.model.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Aggregated statistics about the problem catalogue.
 *
 * <p>This is a <em>derived</em> view — it isn't stored anywhere; the service
 * computes it on demand from the existing problems. It illustrates a piece of
 * pure business logic that belongs in the service layer rather than in a
 * controller or the repository.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemStatsResponse {

    /** Total number of problems. */
    private long total;

    /** How many problems fall into each difficulty bucket. */
    private Map<Difficulty, Long> countByDifficulty;
}
