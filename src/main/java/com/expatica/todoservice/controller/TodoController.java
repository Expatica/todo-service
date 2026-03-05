package com.expatica.todoservice.controller;

import com.expatica.todoservice.controller.dto.CreateTodoRequest;
import com.expatica.todoservice.controller.dto.TodoPageResponse;
import com.expatica.todoservice.controller.dto.TodoResponse;
import com.expatica.todoservice.controller.dto.UpdateDescriptionRequest;
import com.expatica.todoservice.controller.mapper.TodoMapper;
import com.expatica.todoservice.domain.Todo;
import com.expatica.todoservice.domain.TodoStatus;
import com.expatica.todoservice.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

/**
 * REST controller for managing todo items.
 *
 * <p>Provides endpoints for:</p>
 * - Creating new todos
 * - Retrieving todos with filtering, pagination, and sorting
 * - Updating todo descriptions
 * - Marking todos as done or not done
 *
 * <p>All requests and responses use DTOs for data transfer.
 * Domain validation is enforced at both the controller and service layers.</p>
 */
@RestController
@RequestMapping("/todos")
@Tag(name = "Todos", description = "APIs for managing todo items")
public class TodoController {

    private final TodoService todoService;
    private final TodoMapper todoMapper;

    public TodoController(TodoService todoService, TodoMapper todoMapper) {
        this.todoService = todoService;
        this.todoMapper = todoMapper;
    }

    /**
     * Creates a new todo item.
     *
     * @param request the create todo request containing description and due date
     * @return the created todo item
     */
    @PostMapping
    @Operation(
            summary = "Create a new todo",
            description = "Creates a new todo item with the provided description and due date. Due date must be in the future."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Todo created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - description is blank, exceeds 255 characters, or due date is not in the future")
    })
    public ResponseEntity<TodoResponse> createTodo(@Valid @RequestBody CreateTodoRequest request) {
        Todo todo = todoService.createTodo(request.description(), request.dueAt());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(todoMapper.toResponse(todo));
    }

    /**
     * Retrieves a single todo by ID.
     *
     * @param id the todo ID
     * @return the todo item if found
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get a todo by ID",
            description = "Retrieves a single todo item by its unique identifier."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo found"),
            @ApiResponse(responseCode = "404", description = "Todo not found")
    })
    public ResponseEntity<TodoResponse> getTodo(
            @Parameter(description = "The unique identifier of the todo", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id
    ) {
        return todoService.getTodoById(id)
                .map(todo -> ResponseEntity.ok(todoMapper.toResponse(todo)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves todos with optional filtering, pagination, and sorting.
     *
     * @param status comma-separated list of statuses to filter by (default: not done)
     * @param page page number (0-indexed, default: 0)
     * @param size page size (default: 20)
     * @param sortBy field to sort by (default: dueAt)
     * @param direction sort direction ASC or DESC (default: ASC)
     * @param all if true, ignore pagination and return all results
     * @return paginated list of todos
     */
    @GetMapping
    @Operation(
            summary = "Get todos with filtering, pagination, and sorting",
            description = "Retrieves todos with optional filtering by status, pagination, and sorting. Status filter accepts comma-separated values."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todos retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters - invalid sort field or invalid status")
    })
    public ResponseEntity<TodoPageResponse> getTodos(
            @Parameter(description = "Comma-separated list of statuses (NOT_DONE, DONE, PAST_DUE)", example = "NOT_DONE,PAST_DUE")
            @RequestParam(required = false, defaultValue = "NOT_DONE") String status,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0")
            @Min(0) int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) int size,

            @Parameter(description = "Field to sort by (description, createdAt, dueAt, status)", example = "dueAt")
            @RequestParam(defaultValue = "dueAt") String sortBy,

            @Parameter(description = "Sort direction (ASC or DESC)")
            @RequestParam(defaultValue = "ASC") Sort.Direction direction,

            @Parameter(description = "If true, return all results ignoring pagination")
            @RequestParam(defaultValue = "false") boolean all
    ) {
        // Parse status filter
        Collection<TodoStatus> statuses = parseStatuses(status);

        // Handle "all" parameter
        Pageable pageable;
        if (all) {
            pageable = PageRequest.of(0, Integer.MAX_VALUE, direction, sortBy);
        } else {
            pageable = PageRequest.of(page, size, direction, sortBy);
        }

        Page<Todo> todosPage = todoService.getTodosByStatuses(statuses, pageable);
        Page<TodoResponse> responsePageable = todoMapper.toResponsePage(todosPage);

        return ResponseEntity.ok(TodoPageResponse.from(responsePageable));
    }

    /**
     * Updates the description of a todo.
     * Only allowed if the todo's status is NOT_DONE.
     *
     * @param id the todo ID
     * @param request the update request containing the new description
     * @return the updated todo
     */
    @PatchMapping("/{id}/description")
    @Operation(
            summary = "Update a todo's description",
            description = "Updates the description of a todo item. Only allowed if the todo's status is NOT_DONE."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Description updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - description is blank or exceeds 255 characters"),
            @ApiResponse(responseCode = "403", description = "Forbidden - todo is immutable or not in NOT_DONE status"),
            @ApiResponse(responseCode = "404", description = "Todo not found")
    })
    public ResponseEntity<TodoResponse> updateDescription(
            @Parameter(description = "The unique identifier of the todo", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDescriptionRequest request
    ) {
        Todo todo = todoService.updateDescription(id, request.description());
        return ResponseEntity.ok(todoMapper.toResponse(todo));
    }

    /**
     * Marks a todo as done.
     * Only allowed if the todo's status is NOT_DONE.
     *
     * @param id the todo ID
     * @return the updated todo with status DONE
     */
    @PatchMapping("/{id}/mark-done")
    @Operation(
            summary = "Mark a todo as done",
            description = "Marks a todo item as DONE. Only allowed if the todo's status is NOT_DONE."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo marked as done successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - todo is immutable or not in NOT_DONE status"),
            @ApiResponse(responseCode = "404", description = "Todo not found")
    })
    public ResponseEntity<TodoResponse> markAsDone(
            @Parameter(description = "The unique identifier of the todo", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id
    ) {
        Todo todo = todoService.markAsDone(id);
        return ResponseEntity.ok(todoMapper.toResponse(todo));
    }

    /**
     * Marks a todo as not done.
     * Only allowed if the todo's status is DONE and its due date is still in the future.
     *
     * @param id the todo ID
     * @return the updated todo with status NOT_DONE
     */
    @PatchMapping("/{id}/mark-not-done")
    @Operation(
            summary = "Mark a todo as not done",
            description = "Marks a todo item as NOT_DONE. Only allowed if the todo's status is DONE and its due date is still in the future."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo marked as not done successfully"),
            @ApiResponse(responseCode = "404", description = "Todo not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - todo is immutable, not in DONE status, or due date has passed")
    })
    public ResponseEntity<TodoResponse> markAsNotDone(
            @Parameter(description = "The unique identifier of the todo", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id
    ) {
        Todo todo = todoService.markAsNotDone(id);
        return ResponseEntity.ok(todoMapper.toResponse(todo));
    }

    /**
     * Parses a comma-separated list of status strings into a collection of TodoStatus enums.
     * If the input is null or empty, returns all statuses.
     *
     * @param statusString comma-separated list of status strings
     * @return collection of TodoStatus enums
     * @throws IllegalArgumentException if an invalid status is encountered
     */
    private Collection<TodoStatus> parseStatuses(String statusString) {
        if (statusString == null || statusString.isBlank()) {
            return Arrays.asList(TodoStatus.values());
        }

        return Arrays.stream(statusString.split(","))
                .map(String::trim)
                .map(TodoStatus::valueOf)
                .toList();
    }

}


