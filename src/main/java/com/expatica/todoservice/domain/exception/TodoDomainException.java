package com.expatica.todoservice.domain.exception;

public abstract class TodoDomainException extends RuntimeException {
    public TodoDomainException(String message) {
        super(message);
    }
}
