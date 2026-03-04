package com.expatica.todoservice.domain;

public class InvalidTodoStatusTransitionException extends TodoDomainException {

    public InvalidTodoStatusTransitionException(Todo todo, TodoStatus toStatus) {
        super(String.format("Todo %s is not allowed to transition from %s to %s", todo.getId(), todo.getStatus(), toStatus));
    }

}
