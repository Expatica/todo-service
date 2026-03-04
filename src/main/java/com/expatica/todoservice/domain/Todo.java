package com.expatica.todoservice.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    private Instant dueDateTime;

    @CreationTimestamp
    private Instant createdAt;

    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    private TodoStatus status = TodoStatus.NOT_DONE;

    protected Todo() {
    }

    public Todo(String description, Instant dueDateTime) {
        this.description = description;
        this.dueDateTime = dueDateTime;
    }

    public void changeDescription() {
        ensureStatusAllowsModification();
        setDescription(description);
    }

    public void changeDueDate(Instant dueDateTime) {
        ensureStatusAllowsModification();
        setDueDateTime(dueDateTime);
    }

    public void markAsDone() {
        // Do nothing if being asked to mark a done item as done
        if (getStatus() == TodoStatus.DONE) return;

        setStatus(TodoStatus.DONE);
        setCompletedAt(Instant.now());
    }

    public void markAsNotDone() {
        TodoStatus status = (this.dueDateTime != null && this.dueDateTime.isBefore(Instant.now()))
                ? TodoStatus.NOT_DONE
                : TodoStatus.PAST_DUE;

        setStatus(status);
        setCompletedAt(null);
    }


    // Getters and Setters
    public UUID getId() {
        return id;
    }

    protected void setId(UUID id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    public Instant getDueDateTime() {
        return dueDateTime;
    }

    protected void setDueDateTime(Instant dueDateTime) {
        this.dueDateTime = dueDateTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    protected void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    protected void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public TodoStatus getStatus() {
        if (this.status == TodoStatus.NOT_DONE && this.dueDateTime != null && this.dueDateTime.isAfter(Instant.now())) {
            setStatus(TodoStatus.PAST_DUE);
        }
        return this.status;
    }

    protected void setStatus(TodoStatus status) {
        this.status = status;
    }

    private void ensureStatusAllowsModification() {
        TodoStatus currentStatus = getStatus();

        if (currentStatus == TodoStatus.DONE) {
            throw new TodoUnmodifiablePropertyException(this);
        }

        if (currentStatus == TodoStatus.PAST_DUE) {
            throw new TodoUnmodifiablePropertyException(this);
        }
    }

}
