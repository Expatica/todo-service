package com.expatica.todoservice.controller.dto;

import com.expatica.todoservice.domain.TodoStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a todo item.
 * Represents the complete state of a todo in JSON format.
 */
@Schema(description = "Response containing a todo item")
public record TodoResponse(
        @Schema(description = "Unique identifier of the todo item", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Description of the todo item", example = "Buy milk")
        String description,

        @Schema(description = "Due date for the todo item in ISO 8601 format", example = "2026-03-10T12:00:00Z")
        Instant dueAt,

        @Schema(description = "Creation timestamp in ISO 8601 format", example = "2026-03-04T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Completion timestamp in ISO 8601 format, null if not completed", example = "2026-03-05T14:30:00Z")
        Instant completedAt,

        @Schema(description = "Current status of the todo item", example = "NOT_DONE")
        TodoStatus status
) {
}

