package com.expatica.todoservice.service.exception;

import java.util.UUID;

public class TodoNotFoundException extends RuntimeException {

    public TodoNotFoundException(UUID uuid) {
        super(String.format("Todo with id '%s' not found", uuid));
    }

}
