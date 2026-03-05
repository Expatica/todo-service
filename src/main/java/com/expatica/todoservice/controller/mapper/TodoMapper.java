package com.expatica.todoservice.controller.mapper;

import com.expatica.todoservice.controller.dto.TodoResponse;
import com.expatica.todoservice.domain.Todo;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class TodoMapper {

    public TodoResponse toResponse(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getDescription(),
                todo.getDueAt(),
                todo.getCreatedAt(),
                todo.getCompletedAt(),
                todo.getStatus()
        );
    }

    public Page<TodoResponse> toResponsePage(Page<Todo> page) {
        return page.map(this::toResponse);
    }

}

