package com.codearena.codearena.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response body returned for every handled error.
 *
 * <p>A consistent error shape is part of a good API contract: clients can rely
 * on always getting {@code status}, a human-readable {@code message}, and the
 * request {@code path}. For validation failures, {@code fieldErrors} maps each
 * rejected field to its message.
 *
 * <p>{@code @JsonInclude(NON_NULL)} omits {@code fieldErrors} from the JSON when
 * it isn't a validation error, keeping responses tidy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    /** When the error occurred (server time). */
    private Instant timestamp;

    /** HTTP status code, e.g. 400. */
    private int status;

    /** HTTP reason phrase, e.g. "Bad Request". */
    private String error;

    /** Human-readable explanation. */
    private String message;

    /** The request path that produced the error. */
    private String path;

    /** Field-by-field messages for validation failures (otherwise omitted). */
    private Map<String, String> fieldErrors;
}
