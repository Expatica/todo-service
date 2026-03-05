package com.expatica.todoservice.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TodoTest {

    @Test
    void construct_whenEmptyDescription_fails() {
        assertThrows(IllegalArgumentException.class, () -> new Todo("", Instant.now().plusSeconds(3600)));
        assertThrows(IllegalArgumentException.class, () -> new Todo("  ", Instant.now().plusSeconds(3600)));
    }

    @Test
    void construct_whenNullDescription_fails() {
        assertThrows(IllegalArgumentException.class, () -> new Todo(null, Instant.now().plusSeconds(3600)));
    }

    @Test
    void construct_whenLongDescription_fails() {
        assertThrows(IllegalArgumentException.class, () -> new Todo("A".repeat(256), Instant.now().plusSeconds(3600)));
    }

    @Test
    void construct_whenDueDateNull_fails() {
        assertThrows(IllegalArgumentException.class, () -> new Todo("Task", null));
    }

    @Test
    void construct_whenDueDatePrecise_truncatesToMinute() {
        Instant futureTime = Instant.now().plusSeconds(3600);
        Todo t = new Todo("Task", Instant.now().plusSeconds(3600));
        assertEquals(futureTime.truncatedTo(ChronoUnit.MINUTES), t.getDueAt());
    }

    @Test
    void changeDescription_whenDueDateFuture_changesDescription() {
        Todo t = new Todo("initial", Instant.now().plusSeconds(3600));
        t.changeDescription("updated");
        assertEquals("updated", t.getDescription());
    }

    @Test
    void changeDescription_whenDueDatePast_throwsImmutableTodoException() {
        Todo t = new TodoBuilder("initial", Instant.now().minusSeconds(60)).build();
        assertThrows(ImmutableTodoException.class, () -> t.changeDescription("updated"));
    }

    @Test
    void changeDescription_whenDescriptionEmpty_throwsIllegalArgumentException() {
        Todo t = new TodoBuilder("initial", Instant.now().plusSeconds(3600)).build();
        assertThrows(IllegalArgumentException.class, () -> t.changeDescription(""));
    }

    @Test
    void changeDescription_whenDescriptionNull_throwsIllegalArgumentException() {
        Todo t = new TodoBuilder("initial", Instant.now().plusSeconds(3600)).build();
        assertThrows(IllegalArgumentException.class, () -> t.changeDescription(null));
    }

    @Test
    void changeDescription_whenDescriptionTooLong_throwsIllegalArgumentException() {
        Todo t = new TodoBuilder("initial", Instant.now().plusSeconds(3600)).build();
        assertThrows(IllegalArgumentException.class, () -> t.changeDescription("A".repeat(256)));
    }

    @Test
    void getStatus_transitionsToPastDue_whenDuePassed() {
        Todo t = new TodoBuilder("initial", Instant.now().minusSeconds(60)).build();
        assertEquals(TodoStatus.PAST_DUE, t.getStatus());
        // subsequent calls should remain PAST_DUE
        assertEquals(TodoStatus.PAST_DUE, t.getStatus());
    }

    @Test
    void markAsDone_setsStatusAndCompletedAt_whenDueDateFuture() {
        Todo t = new Todo("task", Instant.now().plusSeconds(3600));
        t.markAsDone();
        assertEquals(TodoStatus.DONE, t.getStatus());
        assertNotNull(t.getCompletedAt());
        // completedAt should be in the past (or now) relative to test time
        assertTrue(t.getCompletedAt().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void markAsDone_whenDueDatePast_throwsImmutableTodoException() {
        Todo t = new TodoBuilder("task", Instant.now().minusSeconds(60)).build();
        assertThrows(ImmutableTodoException.class, t::markAsDone);
    }

    @Test
    void markAsNotDone_fromDone_setsNotDone() {
        Todo t = new Todo("task", Instant.now().plusSeconds(3600));
        t.markAsDone();
        t.markAsNotDone();
        assertEquals(TodoStatus.NOT_DONE, t.getStatus());
        assertNull(t.getCompletedAt());
    }

    @Test
    void markAsNotDone_whenDueDatePast_throwsImmutableTodoException() {
        Todo t = new TodoBuilder("task", Instant.now().minusSeconds(60))
                .withStatus(TodoStatus.DONE)
                .withCompletedAt(Instant.now().minusSeconds(120))
                .build();
        assertThrows(ImmutableTodoException.class, t::markAsDone);
    }

    @Test
    void markAsNotDone_whenNotDone_throwsInvalidTodoStatusTransitionException() {
        Todo t = new Todo("task", Instant.now().plusSeconds(3600));
        assertThrows(InvalidTodoStatusTransitionException.class, t::markAsNotDone);
    }

    @Test
    void markAsDone_whenAlreadyDone_throwsInvalidTodoStatusTransitionException() {
        Todo t = new Todo("task", Instant.now().plusSeconds(3600));
        t.markAsDone();
        assertThrows(InvalidTodoStatusTransitionException.class, t::markAsDone);
    }
}

