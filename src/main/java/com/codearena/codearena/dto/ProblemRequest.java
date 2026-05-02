package com.codearena.codearena.dto;

import com.codearena.codearena.model.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Incoming payload used to create or update a problem.
 *
 * <p>This is a <em>DTO</em> (Data Transfer Object): it describes exactly what
 * the client is allowed to send, which is deliberately a subset of the domain
 * model. Notice there is no {@code id} or {@code createdAt} here — those are
 * controlled by the server, never by the caller. Keeping a separate request
 * type (instead of accepting the {@code Problem} model directly) prevents
 * clients from overwriting server-managed fields.
 *
 * <p>Bean Validation annotations (e.g. {@code @NotBlank}) are added in Phase 5;
 * for now the shape is intentionally plain.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemRequest {

    private String title;

    private String description;

    private Difficulty difficulty;

    private List<String> tags;
}
