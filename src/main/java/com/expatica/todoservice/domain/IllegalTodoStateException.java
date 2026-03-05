package com.expatica.todoservice.domain;

public class IllegalTodoStateException extends TodoDomainException {
    public IllegalTodoStateException(String message) {
        super(message);
    }
}
