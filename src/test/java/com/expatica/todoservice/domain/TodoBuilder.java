package com.expatica.todoservice.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Builder for creating Todo instances with specific states that cannot be reached
 * through the public constructor of the Todo class.
 *
 * This builder allows creating Todo objects in various states including:
 * - PAST_DUE todos (with due dates in the past)
 * - DONE todos (with completion dates)
 * - Todos with custom IDs
 * - Todos with custom creation timestamps
 */
public class TodoBuilder {
    private UUID id;
    private String description;
    private Instant dueAt;
    private Instant createdAt;
    private Instant completedAt;
    private TodoStatus status;

    /**
     * Creates a new TodoBuilder with required description and dueAt.
     * @param description the todo description
     * @param dueAt the due date/time
     */
    public TodoBuilder(String description, Instant dueAt) {
        this.description = description;
        this.dueAt = dueAt;
    }

    /**
     * Sets the ID for the todo.
     * @param id the UUID to assign
     * @return this builder instance for method chaining
     */
    public TodoBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the creation timestamp for the todo.
     * @param createdAt the creation instant
     * @return this builder instance for method chaining
     */
    public TodoBuilder withCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * Sets the completion timestamp for the todo.
     * @param completedAt the completion instant
     * @return this builder instance for method chaining
     */
    public TodoBuilder withCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
        return this;
    }

    /**
     * Sets the status for the todo.
     * @param status the TodoStatus
     * @return this builder instance for method chaining
     */
    public TodoBuilder withStatus(TodoStatus status) {
        this.status = status;
        return this;
    }

    /**
     * Builds and returns the Todo instance with the configured state.
     * Uses reflection to set private fields that cannot be set through normal constructors.
     * @return a new Todo instance with the specified state
     */
    public Todo build() {
        try {
            // Create instance using protected no-arg constructor
            Todo todo = new Todo();

            // Set id if provided, otherwise it will be generated
            if (id != null) {
                setFieldValue(todo, "id", id);
            }

            // Set all other fields
            setFieldValue(todo, "description", description);
            setFieldValue(todo, "dueAt", dueAt);
            setFieldValue(todo, "status", status != null ? status : TodoStatus.NOT_DONE);

            if (createdAt != null) {
                setFieldValue(todo, "createdAt", createdAt);
            }

            if (completedAt != null) {
                setFieldValue(todo, "completedAt", completedAt);
            }

            return todo;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to build Todo instance", e);
        }
    }

    /**
     * Helper method to set private field values using reflection.
     */
    private void setFieldValue(Todo todo, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        var field = Todo.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(todo, value);
    }
}
