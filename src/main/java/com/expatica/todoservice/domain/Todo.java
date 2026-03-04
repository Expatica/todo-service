package com.expatica.todoservice.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
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
            @NotBlank @Size(max = 255) String description,
            @NotNull @Future Instant dueAt
    ) {
        this.description = description;
        this.dueAt = dueAt;
    }

    public void changeDescription(@NotBlank @Size(max = 255) String description) {
        Todo.requireFutureDueDate(this);
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
        Todo.requireFutureDueDate(this);
        Todo.requireAllowedStatusTransition(this, TodoStatus.DONE);

        this.status = TodoStatus.DONE;
        this.completedAt = Instant.now();
    }

    public void markAsNotDone() {
        Todo.requireAllowedStatusTransition(this, TodoStatus.NOT_DONE);

        this.status = TodoStatus.NOT_DONE;
        this.completedAt = null;
    }

    private static void requireFutureDueDate(Todo todo) {
        if (todo.dueAt.isBefore(Instant.now())) {
            throw new ImmutableTodoException(todo.getId());
        }
    }

    private static void requireAllowedStatusTransition(Todo todo, TodoStatus toStatus) {
        if (!todo.getStatus().canTransitionTo(toStatus)) {
            throw new InvalidTodoStatusTransitionException(todo, toStatus);
        }
    }
}
