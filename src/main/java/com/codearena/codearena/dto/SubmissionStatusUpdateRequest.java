package com.codearena.codearena.dto;

import com.codearena.codearena.model.SubmissionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for the admin-only "judge" endpoint that moves a submission to a new
 * status (e.g. {@code ACCEPTED}/{@code REJECTED}). This stands in for the real
 * code execution engine (Phase 8).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionStatusUpdateRequest {

    @NotNull(message = "status is required")
    private SubmissionStatus status;
}
