package com.expatica.todoservice.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

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