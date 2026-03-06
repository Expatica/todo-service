package com.expatica.todoservice.domain.exception;

import java.util.UUID;

public class ImmutableTodoException extends TodoDomainException {

    public ImmutableTodoException(UUID id) {
        super(String.format("Todo, %s, can no longer be modified because the due date has passed.", id));
    }
}
