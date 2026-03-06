package com.expatica.todoservice.domain;

public abstract class TodoDomainException extends RuntimeException {
    public TodoDomainException(String message) {
        super(message);
    }
}
