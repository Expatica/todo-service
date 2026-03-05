package com.expatica.todoservice.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Response DTO for paginated todo items.
 */
@Schema(description = "Paginated response containing todo items")
public record TodoPageResponse(
        @Schema(description = "List of todo items in this page")
        List<TodoResponse> content,

        @Schema(description = "Current page number (0-indexed)")
        int pageNumber,

        @Schema(description = "Number of items in this page")
        int pageSize,

        @Schema(description = "Total number of items across all pages")
        long totalElements,

        @Schema(description = "Total number of pages")
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

