package com.expatica.todoservice.controller.dto;

import com.expatica.todoservice.domain.TodoStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a todo item.
 * Represents the complete state of a todo in JSON format.
 */
public record TodoResponse(
        UUID id,
        String description,
        Instant dueAt,
        Instant createdAt,
        Instant completedAt,
        TodoStatus status
) { }
