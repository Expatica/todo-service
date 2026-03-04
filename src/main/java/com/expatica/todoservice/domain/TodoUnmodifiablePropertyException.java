package com.expatica.todoservice.domain;

/**
 * Thrown to indicate that the {@code Todo} cannot be modified while in the given status.
 */
public class TodoUnmodifiablePropertyException extends TodoDomainException {

    /**
     * Constructs a {@code TodoUnmodifiablePropertyException} for the given {@code Todo}
     * @param todo  the {@code Todo} that cannot be modified due to its status
     */
    public TodoUnmodifiablePropertyException(Todo todo) {
        super(String.format("Cannot modify todo item %s: the todo is %s", todo.getId(), todo.getStatus()));
    }

}