package com.expatica.todoservice.controller.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Request DTO for creating a new todo item.
 */
public record CreateTodoRequest(
        @NotBlank(message = "Description must not be blank")
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotNull(message = "Due date must not be null")
        @Future(message = "Due date must be in the future")
        Instant dueAt
) { }

