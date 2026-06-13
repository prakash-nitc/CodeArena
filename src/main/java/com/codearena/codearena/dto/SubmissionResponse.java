package com.codearena.codearena.dto;

import com.codearena.codearena.model.Language;
import com.codearena.codearena.model.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Outgoing representation of a submission.
 *
 * <p>Flattens the {@code problem} and {@code user} relationships into a few
 * useful fields ({@code problemId}, {@code problemTitle}, {@code username}) so
 * the API never exposes the entities directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {

    private Long id;

    private Long problemId;

    private String problemTitle;

    private String username;

    private Language language;

    private SubmissionStatus status;

    private String sourceCode;

    private Instant createdAt;
}
