package com.expatica.todoservice.domain.exception;

public class IllegalTodoStateException extends TodoDomainException {
    public IllegalTodoStateException(String message) {
        super(message);
    }
}
