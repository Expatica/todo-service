package com.expatica.todoservice.exception;

import com.expatica.todoservice.domain.IllegalTodoStateException;
import com.expatica.todoservice.domain.ImmutableTodoException;
import com.expatica.todoservice.domain.InvalidTodoStatusTransitionException;
import com.expatica.todoservice.domain.TodoDomainException;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.List;

/**
 * Global exception handler for the application.
 *
 * <p>Centralizes exception handling across all controllers and provides
 * consistent error response format.</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles domain-level exceptions (ImmutableTodoException, InvalidTodoStatusTransitionException).
     * Returns 403 Forbidden status.
     */
    @ExceptionHandler({ImmutableTodoException.class, InvalidTodoStatusTransitionException.class, IllegalTodoStateException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleDomainException(
            TodoDomainException ex,
            WebRequest request
    ) {
        logger.warn("Domain exception occurred: {}", ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                Instant.now()
        );

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles validation errors for invalid request bodies.
     * Returns 400 Bad Request status.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        logger.warn("Validation exception occurred");

        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage)
                .toList();

        String message = String.join("; ", errors);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                Instant.now()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles IllegalArgumentException (typically from domain validation or invalid enum values).
     * Returns 400 Bad Request status.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request
    ) {
        logger.warn("Illegal argument exception occurred: {}", ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                Instant.now()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles NoSuchElementException when a todo is not found.
     * Returns 404 Not Found status.
     */
    @ExceptionHandler(java.util.NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(
            java.util.NoSuchElementException ex,
            WebRequest request
    ) {
        logger.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                Instant.now()
        );

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }


    /**
     * Handles any other unexpected exceptions.
     * Returns 500 Internal Server Error status.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex,
            WebRequest request
    ) {
        logger.error("Unexpected exception occurred", ex);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                Instant.now()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Standard error response format.
     */
    @Schema(description = "Standard error response")
    public record ErrorResponse(
            @Schema(description = "HTTP status code", example = "400")
            int status,

            @Schema(description = "Error message", example = "Description must not be blank")
            String message,

            @Schema(description = "Timestamp when the error occurred", example = "2026-03-04T10:00:00Z")
            Instant timestamp
    ) {
    }
}

