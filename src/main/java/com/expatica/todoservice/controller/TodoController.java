package com.expatica.todoservice.controller;

import com.expatica.todoservice.controller.dto.CreateTodoRequest;
import com.expatica.todoservice.controller.dto.TodoPageResponse;
import com.expatica.todoservice.controller.dto.TodoResponse;
import com.expatica.todoservice.controller.dto.UpdateDescriptionRequest;
import com.expatica.todoservice.controller.mapper.TodoMapper;
import com.expatica.todoservice.domain.Todo;
import com.expatica.todoservice.domain.TodoStatus;
import com.expatica.todoservice.service.TodoService;
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

@RestController
@RequestMapping("/todos")
public class TodoController {

    private final TodoService todoService;
    private final TodoMapper todoMapper;

    public TodoController(TodoService todoService, TodoMapper todoMapper) {
        this.todoService = todoService;
        this.todoMapper = todoMapper;
    }

    // curl -X POST 'http://localhost:8080/todos' -H "Content-Type: application/json" -d '{"description":"My Task", "dueAt": "2026-03-10T12:00:00Z"}'
    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(@Valid @RequestBody CreateTodoRequest request) {
        Todo todo = todoService.createTodo(request.description(), request.dueAt());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(todoMapper.toResponse(todo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TodoResponse> getTodo(
            @PathVariable UUID id
    ) {
        return todoService.getTodoById(id)
                .map(todo -> ResponseEntity.ok(todoMapper.toResponse(todo)))
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping
    public ResponseEntity<TodoPageResponse> getTodos(
            @RequestParam(required = false)
            String status,

            @RequestParam(defaultValue = "0")
            @Min(0) int page,

            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) int size,

            @RequestParam(defaultValue = "dueAt")
            String sortBy,

            @RequestParam(defaultValue = "ASC")
            Sort.Direction direction,

            @RequestParam(defaultValue = "false")
            boolean all
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

    @PatchMapping("/{id}/description")
    public ResponseEntity<TodoResponse> updateDescription(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDescriptionRequest request
    ) {
        Todo todo = todoService.updateDescription(id, request.description());
        return ResponseEntity.ok(todoMapper.toResponse(todo));
    }

    @PatchMapping("/{id}/mark-done")
    public ResponseEntity<TodoResponse> markAsDone(
            @PathVariable UUID id
    ) {
        Todo todo = todoService.markAsDone(id);
        return ResponseEntity.ok(todoMapper.toResponse(todo));
    }

    @PatchMapping("/{id}/mark-not-done")
    public ResponseEntity<TodoResponse> markAsNotDone(
            @PathVariable UUID id
    ) {
        Todo todo = todoService.markAsNotDone(id);
        return ResponseEntity.ok(todoMapper.toResponse(todo));
    }

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


