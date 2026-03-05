package com.expatica.todoservice.controller.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Response DTO for paginated todo items.
 */
public record TodoPageResponse(
        List<TodoResponse> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {
    /**
     * Creates a TodoPageResponse from a Page of TodoResponse objects.
     */
    public static TodoPageResponse from(Page<TodoResponse> page) {
        return new TodoPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}

