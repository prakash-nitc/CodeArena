package com.codearena.codearena.dto;

import com.codearena.codearena.model.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for submitting a solution to a problem.
 *
 * <p>The submitter and the target problem are <em>not</em> in the body: the
 * submitter is taken from the authenticated principal, and the problem from the
 * URL ({@code /api/problems/{problemId}/submissions}). The client only supplies
 * the language and the code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionRequest {

    @NotNull(message = "language is required")
    private Language language;

    @NotBlank(message = "sourceCode must not be blank")
    @Size(max = 50000, message = "sourceCode must be at most 50000 characters")
    private String sourceCode;
}
