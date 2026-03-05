package com.expatica.todoservice.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 255)
    private String description;

    private Instant dueAt;

    @CreationTimestamp
    private Instant createdAt;

    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    private TodoStatus status = TodoStatus.NOT_DONE;

    protected Todo() {
    }

    public Todo(
            String description,
            Instant dueAt
    ) {
        // Validate description
        Todo.requireNonBlankDescriptionUnder255Characters(description);

        // Validate dueAt
        Todo.requireFutureDueDate(dueAt);

        this.description = description;
        this.dueAt = dueAt.truncatedTo(ChronoUnit.MINUTES);
    }

    public void changeDescription(String description) {
        Todo.requireNonBlankDescriptionUnder255Characters(description);
        Todo.requireFutureDueDateToModify(this);
        Todo.requireNotDoneStatus(this);
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public TodoStatus getStatus() {
        if (this.status == TodoStatus.NOT_DONE && this.dueAt.isBefore(Instant.now())) {
            this.status = TodoStatus.PAST_DUE;
        }
        return this.status;
    }

    public void markAsDone() {
        Todo.requireFutureDueDateToModify(this);
        Todo.requireAllowedStatusTransition(this, TodoStatus.DONE);

        this.status = TodoStatus.DONE;
        this.completedAt = Instant.now();
    }

    public void markAsNotDone() {
        Todo.requireFutureDueDateToModify(this);
        Todo.requireAllowedStatusTransition(this, TodoStatus.NOT_DONE);

        this.status = TodoStatus.NOT_DONE;
        this.completedAt = null;
    }


    private static void requireAllowedStatusTransition(Todo todo, TodoStatus toStatus) {
        if (!todo.getStatus().canTransitionTo(toStatus)) {
            throw new InvalidTodoStatusTransitionException(todo, toStatus);
        }
    }

    private static void requireFutureDueDate(Instant dueAt) {
        if (dueAt == null) {
            throw new IllegalArgumentException("Due date must not be null");
        }

        if (dueAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Due date must be in the future");
        }
    }

    private static void requireFutureDueDateToModify(Todo todo) {
        if (todo.dueAt.isBefore(Instant.now())) {
            throw new ImmutableTodoException(todo.getId());
        }
    }

    private static void requireNonBlankDescriptionUnder255Characters(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description must not be blank");
        }
        if (description.length() > 255) {
            throw new IllegalArgumentException("Description must not exceed 255 characters");
        }
    }

    private static void requireNotDoneStatus(Todo todo) {
        if (todo.getStatus() != TodoStatus.NOT_DONE) {
            throw new IllegalTodoStateException("Description cannot be modified after the todo is DONE.");
        }
    }
}
