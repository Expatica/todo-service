package com.expatica.todoservice.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a todo item's description.
 */
public record UpdateDescriptionRequest(
        @NotBlank(message = "Description must not be blank")
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) { }
