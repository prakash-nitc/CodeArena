package com.codearena.codearena.exception;

/**
 * Thrown when a submission is requested by an id that does not exist.
 *
 * <p>Mapped to {@code 404 Not Found} by the {@code GlobalExceptionHandler},
 * mirroring {@link ProblemNotFoundException}.
 */
public class SubmissionNotFoundException extends RuntimeException {

    public SubmissionNotFoundException(Long id) {
        super("Submission not found with id: " + id);
    }
}
