package com.expatica.todoservice.domain;

public enum TodoStatus {
    NOT_DONE,
    DONE,
    PAST_DUE;

    public boolean canTransitionTo(TodoStatus next) {
        return switch (this) {
            case NOT_DONE -> next == DONE || next == PAST_DUE;
            case DONE -> next == NOT_DONE;
            case PAST_DUE -> false;
        };
    }
}
