package com.expatica.todoservice.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Request DTO for creating a new todo item.
 */
@Schema(description = "Request to create a new todo item")
public record CreateTodoRequest(
        @NotBlank(message = "Description must not be blank")
        @Size(max = 255, message = "Description must not exceed 255 characters")
        @Schema(description = "Description of the todo item", example = "Buy milk")
        String description,

        @NotNull(message = "Due date must not be null")
        @Future(message = "Due date must be in the future")
        @Schema(description = "Due date for the todo item in ISO 8601 format", example = "2050-01-01T12:00:00Z")
        Instant dueAt
) { }

