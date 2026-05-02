package com.codearena.codearena.dto;

import com.codearena.codearena.model.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Outgoing representation of a problem returned to clients.
 *
 * <p>Separating the response DTO from the {@link com.codearena.codearena.model.Problem}
 * domain model means the API contract can stay stable even if the internal
 * model changes, and lets us decide precisely which fields are exposed. Here we
 * expose everything, but server-managed fields such as {@code id} and
 * {@code createdAt} are included as read-only output.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemResponse {

    private Long id;

    private String title;

    private String description;

    private Difficulty difficulty;

    private List<String> tags;

    private Instant createdAt;
}
