package com.codearena.codearena.exception;

import com.codearena.codearena.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central place that turns exceptions into clean HTTP error responses.
 *
 * <p>{@code @RestControllerAdvice} registers these handlers across every
 * controller, so individual controllers never build error responses themselves —
 * they just throw (or let Spring throw), and the right {@link ApiError} comes
 * out with the correct status. Spring picks the handler whose exception type is
 * the closest match.
 *
 * <p>Mappings:
 * <ul>
 *   <li>{@link ProblemNotFoundException} → 404 Not Found</li>
 *   <li>{@link DuplicateProblemTitleException} → 409 Conflict</li>
 *   <li>{@link MethodArgumentNotValidException} (failed {@code @Valid}) → 400 with field errors</li>
 *   <li>{@link MethodArgumentTypeMismatchException} (e.g. {@code /problems/abc} or a bad enum query param) → 400</li>
 *   <li>{@link HttpMessageNotReadableException} (malformed JSON / invalid enum in body) → 400</li>
 *   <li>any other {@link Exception} → 500 (last-resort fallback)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProblemNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ProblemNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateProblemTitleException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateProblemTitleException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // Keep the first message per field for a stable, readable response.
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Parameter '" + ex.getName() + "' has an invalid value: '" + ex.getValue() + "'";
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        // Last-resort fallback. In a larger app you'd also handle framework
        // exceptions (405, 415, ...) explicitly rather than letting them fall here.
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest request, Map<String, String> fieldErrors) {
        ApiError body = ApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
