package com.expatica.todoservice.domain;

import com.expatica.todoservice.domain.exception.ImmutableTodoException;
import com.expatica.todoservice.domain.exception.InvalidTodoStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TodoTest {
    
    Instant now;
    Instant future;
    Instant past;
    
    @BeforeEach
    public void setUp() {
        now = Instant.now();
        future = now.plusSeconds(3600);
        past = now.minusSeconds(3600);
    }

    @Test
    void construct_whenEmptyDescription_fails() {
        assertThrows(IllegalArgumentException.class, () -> new Todo("", future, now));
        assertThrows(IllegalArgumentException.class, () -> new Todo("  ", future, now));
    }

    @Test
    void construct_whenNullDescription_fails() {
        assertThrows(IllegalArgumentException.class, () -> new Todo(null, future, now));
    }

    @Test
    void construct_whenLongDescription_fails() {
        assertThrows(IllegalArgumentException.class, () -> new Todo("A".repeat(256), future, now));
    }

    @Test
    void construct_whenDueDateNull_fails() {
        assertThrows(IllegalArgumentException.class, () -> new Todo("Task", null, now));
    }

    @Test
    void construct_whenDueDatePrecise_truncatesToMinute() {
        Instant futureTime = now.plusSeconds(3600);
        Todo t = new Todo("Task", future, now);
        assertEquals(futureTime.truncatedTo(ChronoUnit.MINUTES), t.getDueAt());
    }

    @Test
    void changeDescription_whenDueDateFuture_changesDescription() {
        Todo t = new Todo("initial", future, now);
        t.changeDescription("updated", now);
        assertEquals("updated", t.getDescription());
    }

    @Test
    void changeDescription_whenDueDatePast_throwsImmutableTodoException() {
        Todo t = new Todo("initial", past, past);
        assertThrows(ImmutableTodoException.class, () -> t.changeDescription("updated", now));
    }

    @Test
    void changeDescription_whenDescriptionEmpty_throwsIllegalArgumentException() {
        Todo t = new Todo("initial", future, now);
        assertThrows(IllegalArgumentException.class, () -> t.changeDescription("", now));
    }

    @Test
    void changeDescription_whenDescriptionNull_throwsIllegalArgumentException() {
        Todo t = new Todo("initial", future, now);
        assertThrows(IllegalArgumentException.class, () -> t.changeDescription(null, now));
    }

    @Test
    void changeDescription_whenDescriptionTooLong_throwsIllegalArgumentException() {
        Todo t = new Todo("initial", future, now);
        assertThrows(IllegalArgumentException.class, () -> t.changeDescription("A".repeat(256), now));
    }

    @Test
    void markAsDone_setsStatusAndCompletedAt_whenDueDateFuture() {
        Todo t = new Todo("task", future, now);
        t.markAsDone(now);
        assertEquals(TodoStatus.DONE, t.getStatus());
        assertNotNull(t.getCompletedAt());
        assertEquals(now, t.getCompletedAt());
    }

    @Test
    void markAsDone_whenDueDatePast_throwsImmutableTodoException() {
        Todo t = new Todo("task", past, past);
        assertThrows(ImmutableTodoException.class, () -> t.markAsDone(now));
    }

    @Test
    void markAsNotDone_fromDone_setsNotDone() {
        Todo t = new Todo("task", future, now);
        t.markAsDone(now);
        t.markAsNotDone(now);
        assertEquals(TodoStatus.NOT_DONE, t.getStatus());
        assertNull(t.getCompletedAt());
    }

    @Test
    void markAsNotDone_whenDueDatePast_throwsImmutableTodoException() {
        Todo t = new Todo("task", now, past);
        t.markAsDone(past);
        assertThrows(ImmutableTodoException.class,  () -> t.markAsNotDone(future));
    }

    @Test
    void markAsNotDone_whenNotDone_throwsInvalidTodoStatusTransitionException() {
        Todo t = new Todo("task", future, now);
        assertThrows(InvalidTodoStatusTransitionException.class, () -> t.markAsNotDone(now));
    }

    @Test
    void markAsDone_whenAlreadyDone_throwsInvalidTodoStatusTransitionException() {
        Todo t = new Todo("task", future, now);
        t.markAsDone(now);
        assertThrows(InvalidTodoStatusTransitionException.class, () -> t.markAsDone(now));
    }
}

