package com.expatica.todoservice.domain;

public class TodoDomainException extends RuntimeException {
    public TodoDomainException(String message) {
        super(message);
    }
}
