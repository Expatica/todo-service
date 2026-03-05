package com.expatica.todoservice.controller;

import com.expatica.todoservice.controller.dto.CreateTodoRequest;
import com.expatica.todoservice.controller.dto.TodoResponse;
import com.expatica.todoservice.controller.mapper.TodoMapper;
import com.expatica.todoservice.domain.Todo;
import com.expatica.todoservice.service.TodoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<TodoResponse> createTodo(@RequestBody CreateTodoRequest request) {
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

}


