package com.expatica.todoservice.controller.dto;

import java.time.Instant;

/**
 * Request DTO for creating a new todo item.
 */
public record CreateTodoRequest(
        String description,
        Instant dueAt
) { }

