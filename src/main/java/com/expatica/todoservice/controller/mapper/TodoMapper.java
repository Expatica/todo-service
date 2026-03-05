package com.expatica.todoservice.controller.mapper;

import com.expatica.todoservice.controller.dto.TodoResponse;
import com.expatica.todoservice.domain.Todo;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Todo domain objects to DTOs.
 * Provides centralized logic for object transformation.
 */
@Component
public class TodoMapper {

    /**
     * Converts a Todo domain object to a TodoResponse DTO.
     *
     * @param todo the Todo domain object
     * @return the corresponding TodoResponse DTO
     */
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

    /**
     * Converts a Page of Todo objects to a Page of TodoResponse DTOs.
     *
     * @param page the Page of Todo objects
     * @return a new Page with TodoResponse DTOs
     */
    public Page<TodoResponse> toResponsePage(Page<Todo> page) {
        return page.map(this::toResponse);
    }

}

