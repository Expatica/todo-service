package com.expatica.todoservice.service;

import com.expatica.todoservice.domain.Todo;
import com.expatica.todoservice.domain.TodoStatus;
import com.expatica.todoservice.repository.TodoRepository;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * TodoService provides transactional boundaries for todo management operations.
 *
 * <p>This service encapsulates all business logic for: <br/>
 * - Creating new todos <br/>
 * - Querying todos with filtering, pagination, and sorting <br/>
 * - Updating todo descriptions <br/>
 * - Marking todos as done or not done <br/>
 * - Transitioning todos to PAST_DUE status <br/>
 *  </p>
 *
 * All operations maintain transactional consistency and enforce domain rules
 * defined in the Todo aggregate.
 */
@Service
@Validated
public class TodoService {

    private final TodoRepository todoRepository;
    private static final String TODO_NOT_FOUND_MESSAGE_TEMPLATE = "Todo not found: %s";

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    /**
     * Creates a new todo with the given description and due date.
     *
     * <p>Validates:</p>
     * - description is not blank and max 255 characters<br/>
     * - dueAt is not null and must be in the future
     *
     * @param description the todo description (not blank, max 255 chars)
     * @param dueAt the due date (must be in the future)
     * @return the created todo
     * @throws IllegalArgumentException if description is blank or future validation fails
     */
    @Transactional
    public Todo createTodo(@NotBlank @Size(min=1, max=255) String description, @NotNull @Future Instant dueAt) {
        Todo todo = new Todo(description, dueAt);
        return todoRepository.saveAndFlush(todo);
    }

    /**
     * Retrieves a single todo by its ID.
     *
     * @param id the todo ID
     * @return the todo if found, or empty Optional
     */
    @Transactional(readOnly = true)
    public Optional<Todo> getTodoById(@NotNull UUID id) {
        return todoRepository.findById(id);
    }

    /**
     * Retrieves all todos with the given statuses, with pagination and sorting.
     *
     * @param statuses collection of TodoStatus values to filter by
     * @param pageable pagination and sorting configuration
     * @return a page of matching todos
     */
    @Transactional(readOnly = true)
    public Page<Todo> getTodosByStatuses(Collection<TodoStatus> statuses, Pageable pageable) {
        return todoRepository.findByStatusIn(statuses, pageable);
    }

    /**
     * Updates the description of a todo.
     *
     * <p>Only allowed if the todo's status is NOT_DONE (i.e., due date hasn't passed).
     * PAST_DUE todos are immutable and will throw an exception.
     * DONE todos cannot have their description changed.</p>
     *
     * <p>Validates:</p>
     * - description is not blank and max 255 characters
     *
     * @param id the todo ID
     * @param newDescription the new description
     * @return the updated todo
     * @throws java.util.NoSuchElementException if todo not found
     * @throws com.expatica.todoservice.domain.ImmutableTodoException if todo is immutable
     * @throws com.expatica.todoservice.domain.InvalidTodoStatusTransitionException if status does not allow update
     * @throws IllegalArgumentException if description validation fails
     */
    @Transactional
    public Todo updateDescription(@NotNull UUID id, @NotBlank @Size(min=1, max=255) String newDescription) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(String.format(TODO_NOT_FOUND_MESSAGE_TEMPLATE, id)));

        todo.changeDescription(newDescription);
        return todoRepository.save(todo);
    }

    /**
     * Marks a todo as DONE.
     *
     * <p>Only allowed if the todo's status is NOT_DONE (i.e., due date hasn't passed).
     * PAST_DUE and DONE todos cannot be marked as done again.</p>
     *
     * @param id the todo ID
     * @return the updated todo with status DONE
     * @throws java.util.NoSuchElementException if todo not found
     * @throws com.expatica.todoservice.domain.ImmutableTodoException if todo is immutable
     * @throws com.expatica.todoservice.domain.InvalidTodoStatusTransitionException if status does not allow transition
     */
    @Transactional
    public Todo markAsDone(@NotNull UUID id) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(String.format(TODO_NOT_FOUND_MESSAGE_TEMPLATE, id)));

        todo.markAsDone();
        return todoRepository.save(todo);
    }

    /**
     * Marks a todo as NOT_DONE.
     *
     * <p>Only allowed if the todo's status is DONE and its due date is still in the future.
     * If the due date has passed, the operation is rejected to prevent reopening expired tasks.</p>
     *
     * @param id the todo ID
     * @return the updated todo with status NOT_DONE
     * @throws java.util.NoSuchElementException if todo not found
     * @throws com.expatica.todoservice.domain.ImmutableTodoException if due date has passed
     * @throws com.expatica.todoservice.domain.InvalidTodoStatusTransitionException if status is not DONE
     */
    @Transactional
    public Todo markAsNotDone(@NotNull UUID id) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException(String.format(TODO_NOT_FOUND_MESSAGE_TEMPLATE, id)));

        todo.markAsNotDone();
        return todoRepository.save(todo);
    }

    /**
     * Finds all NOT_DONE todos with a due date in the past (for scheduling).
     *
     * <p>This method is used by the scheduler to identify todos that should transition
     * to PAST_DUE status.</p>
     *
     * @param now the current instant in UTC
     * @return collection of todos that should transition to PAST_DUE
     */
    @Transactional(readOnly = true)
    public Collection<Todo> findNotDoneWithPastDueDate(@NotNull Instant now) {
        return todoRepository.findNotDoneWithPastDueDate(now);
    }

    /**
     * Bulk updates todos from NOT_DONE to PAST_DUE status.
     *
     * <p>This is the active scheduled transition that keeps todos in sync with
     * the current time. Only todos with status NOT_DONE and dueAt < now will
     * be updated.</p>
     *
     * @param now the current instant in UTC
     * @return the number of todos updated
     */
    @Transactional
    public int transitionToPastDue(@NotNull Instant now) {
        return todoRepository.updateNotDoneToPastDue(now);
    }
}

