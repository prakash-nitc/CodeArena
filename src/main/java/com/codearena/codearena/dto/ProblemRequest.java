package com.codearena.codearena.dto;

import com.codearena.codearena.model.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Incoming payload used to create or update a problem.
 *
 * <p>Phase 5 adds <strong>Bean Validation</strong> constraints. When the
 * controller annotates this with {@code @Valid}, Spring runs these checks
 * <em>before</em> the handler method body executes. If any fail, Spring throws
 * {@code MethodArgumentNotValidException}, which the global exception handler
 * turns into a {@code 400 Bad Request} listing the offending fields.
 *
 * <p>Validation answers "is this input well-formed?" — distinct from business
 * rules (e.g. "is this title already taken?"), which live in the service.
 *
 * <p>{@code difficulty} is intentionally left optional: the service defaults a
 * missing difficulty to {@code MEDIUM}, so we don't reject a null here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemRequest {

    @NotBlank(message = "title must not be blank")
    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;

    @NotBlank(message = "description must not be blank")
    @Size(max = 5000, message = "description must be at most 5000 characters")
    private String description;

    private Difficulty difficulty;

    @Size(max = 10, message = "a problem can have at most 10 tags")
    private List<@Size(max = 40, message = "each tag must be at most 40 characters") String> tags;
}
