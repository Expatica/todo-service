package com.expatica.todoservice.service;

import com.expatica.todoservice.config.ValidationConfig;
import com.expatica.todoservice.domain.*;
import com.expatica.todoservice.repository.TodoRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({TodoService.class, ValidationConfig.class})
@ActiveProfiles("test")
@DisplayName("TodoService Business Rules and State Transitions")
class TodoServiceTest {

    @Autowired
    private TodoService todoService;

    @Autowired
    private TodoRepository todoRepository;

    private Instant futureTime;
    private Instant pastTime;

    @BeforeEach
    void setUp() {
        futureTime = Instant.now().plusSeconds(3600);
        pastTime = Instant.now().minusSeconds(3600);
    }

    @Nested
    @DisplayName("Create Todo")
    class CreateTodoTests {

        @Test
        @DisplayName("should create todo with valid description and future due date")
        void createTodo_validInput_succeeds() {
            Instant farFuture = Instant.now().plusSeconds(86400); // 1 day in future
            Todo created = todoService.createTodo("Buy milk", farFuture);

            assertNotNull(created.getId());
            assertEquals("Buy milk", created.getDescription());
            assertEquals(farFuture, created.getDueAt());
            assertEquals(TodoStatus.NOT_DONE, created.getStatus());
            assertNotNull(created.getCreatedAt());
            assertNull(created.getCompletedAt());
        }

        @Test
        @DisplayName("should persist todo to database")
        void createTodo_persistsToDatabase() {
            Instant farFuture = Instant.now().plusSeconds(86400);
            Todo created = todoService.createTodo("Buy milk", farFuture);

            assertTrue(todoRepository.existsById(created.getId()));
            Todo retrieved = todoRepository.findById(created.getId()).orElse(null);
            assertNotNull(retrieved);
            assertEquals(created.getId(), retrieved.getId());
        }

        @Test
        @DisplayName("should reject blank description")
        void createTodo_blankDescription_fails() {
            assertThrows(Exception.class, () -> todoService.createTodo("", futureTime));
            assertThrows(Exception.class, () -> todoService.createTodo("   ", futureTime));
        }

        @Test
        @DisplayName("should reject past due date")
        void createTodo_pastDueDate_fails() {
            assertThrows(Exception.class, () -> todoService.createTodo("Buy milk", pastTime));
        }

        @Test
        @DisplayName("should reject null due date")
        void createTodo_nullDueDate_fails() {
            assertThrows(Exception.class, () -> todoService.createTodo("Buy milk", null));
        }

        @Test
        @DisplayName("should reject description longer than 255 characters")
        void createTodo_tooLongDescription_fails() {
            String longDescription = "a".repeat(256);
            assertThrows(Exception.class, () -> todoService.createTodo(longDescription, futureTime));
        }
    }

    @Nested
    @DisplayName("Get Todo")
    class GetTodoTests {

        @Test
        @DisplayName("should retrieve todo by ID")
        void getTodoById_exists_returnsOptional() {
            Todo created = todoService.createTodo("Task", futureTime);

            var found = todoService.getTodoById(created.getId());
            assertTrue(found.isPresent());
            assertEquals(created.getId(), found.get().getId());
        }

        @Test
        @DisplayName("should return empty optional when todo not found")
        void getTodoById_notExists_returnsEmpty() {
            var found = todoService.getTodoById(UUID.randomUUID());
            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("should return empty optional when todo not found")
        void getTodoById_nullUUID_throwsException() {
            assertThrows(Exception.class, () -> todoService.getTodoById(null));
        }
    }

    @Nested
    @DisplayName("Update Description - NOT_DONE Status")
    class UpdateDescriptionNotDoneTests {

        @Test
        @DisplayName("should update description of NOT_DONE todo with future due date")
        void updateDescription_notDoneWithFutureDue_succeeds() {
            Todo created = todoService.createTodo("Initial description", futureTime);

            Todo updated = todoService.updateDescription(created.getId(), "Updated description");

            assertEquals("Updated description", updated.getDescription());
            assertEquals(TodoStatus.NOT_DONE, updated.getStatus());
        }

        @Test
        @DisplayName("should persist description change to database")
        void updateDescription_notDoneWithFutureDue_persisted() {
            Todo created = todoService.createTodo("Initial", futureTime);

            todoService.updateDescription(created.getId(), "Updated");

            Todo retrieved = todoRepository.findById(created.getId()).orElse(null);
            assertNotNull(retrieved);
            assertEquals("Updated", retrieved.getDescription());
        }

        @Test
        @DisplayName("should reject update of todo not found")
        void updateDescription_todoNotFound_fails() {
            UUID nonExistentId = UUID.randomUUID();
            assertThrows(
                java.util.NoSuchElementException.class,
                () -> todoService.updateDescription(nonExistentId, "New description")
            );
        }
    }

    @Nested
    @DisplayName("Update Description - DONE Status (Forbidden)")
    class UpdateDescriptionDoneTests {

        @Test
        @DisplayName("should reject description update for DONE todo")
        void updateDescription_done_forbidden() {
            Todo created = todoService.createTodo("Buy milk", futureTime);
            todoService.markAsDone(created.getId());

            assertThrows(
                IllegalTodoStateException.class,
                () -> todoService.updateDescription(created.getId(), "Buy cheese")
            );
        }
    }

    @Nested
    @DisplayName("Update Description - PAST_DUE Status (Immutable)")
    class UpdateDescriptionPastDueTests {

        @Test
        @DisplayName("should reject description update for PAST_DUE todo")
        void updateDescription_pastDue_forbidden() {
            Todo pastDue = new TodoBuilder("Buy milk", pastTime)
                    .withStatus(TodoStatus.PAST_DUE)
                    .build();
            todoRepository.save(pastDue);

            assertThrows(
                ImmutableTodoException.class,
                () -> todoService.updateDescription(pastDue.getId(), "Buy cheese")
            );
        }
    }

    @Nested
    @DisplayName("Mark as DONE - NOT_DONE Status")
    class MarkAsDoneNotDoneTests {

        @Test
        @DisplayName("should mark NOT_DONE todo as DONE")
        void markAsDone_notDone_succeeds() {
            Todo created = todoService.createTodo("Buy milk", futureTime);

            Todo updated = todoService.markAsDone(created.getId());

            assertEquals(TodoStatus.DONE, updated.getStatus());
            assertNotNull(updated.getCompletedAt());
        }

        @Test
        @DisplayName("should persist status and completedAt to database")
        void markAsDone_notDone_persisted() {
            Todo created = todoService.createTodo("Buy milk", futureTime);

            todoService.markAsDone(created.getId());

            Todo retrieved = todoRepository.findById(created.getId()).orElse(null);
            assertNotNull(retrieved);
            assertEquals(TodoStatus.DONE, retrieved.getStatus());
            assertNotNull(retrieved.getCompletedAt());
        }

        @Test
        @DisplayName("should reject mark as done when todo not found")
        void markAsDone_todoNotFound_fails() {
            UUID nonExistentId = UUID.randomUUID();
            assertThrows(
                java.util.NoSuchElementException.class,
                () -> todoService.markAsDone(nonExistentId)
            );
        }
    }

    @Nested
    @DisplayName("Mark as DONE - DONE Status (Forbidden)")
    class MarkAsDoneDoneTests {

        @Test
        @DisplayName("should reject marking already DONE todo as DONE again")
        void markAsDone_alreadyDone_forbidden() {
            Todo created = todoService.createTodo("Buy milk", futureTime);
            todoService.markAsDone(created.getId());

            assertThrows(
                InvalidTodoStatusTransitionException.class,
                () -> todoService.markAsDone(created.getId())
            );
        }
    }

    @Nested
    @DisplayName("Mark as DONE - PAST_DUE Status (Immutable)")
    class MarkAsDonePastDueTests {

        @Test
        @DisplayName("should reject marking PAST_DUE todo as DONE (immutable)")
        void markAsDone_pastDue_forbidden() {
            Todo pastDue = new TodoBuilder("Buy milk", pastTime)
                    .withStatus(TodoStatus.PAST_DUE)
                    .build();
            todoRepository.save(pastDue);

            assertThrows(
                ImmutableTodoException.class,
                () -> todoService.markAsDone(pastDue.getId())
            );
        }
    }

    @Nested
    @DisplayName("Mark as NOT_DONE - DONE Status with Future Due Date")
    class MarkAsNotDoneDoneTests {

        @Test
        @DisplayName("should mark DONE todo as NOT_DONE when due date is future")
        void markAsNotDone_doneWithFutureDue_succeeds() {
            Todo created = todoService.createTodo("Buy milk", futureTime);
            todoService.markAsDone(created.getId());

            Todo updated = todoService.markAsNotDone(created.getId());

            assertEquals(TodoStatus.NOT_DONE, updated.getStatus());
            assertNull(updated.getCompletedAt());
        }

        @Test
        @DisplayName("should persist status change to database")
        void markAsNotDone_doneWithFutureDue_persisted() {
            Todo created = todoService.createTodo("Buy milk", futureTime);
            todoService.markAsDone(created.getId());

            todoService.markAsNotDone(created.getId());

            Todo retrieved = todoRepository.findById(created.getId()).orElse(null);
            assertNotNull(retrieved);
            assertEquals(TodoStatus.NOT_DONE, retrieved.getStatus());
            assertNull(retrieved.getCompletedAt());
        }
    }

    @Nested
    @DisplayName("Mark as NOT_DONE - DONE Status with Past Due Date (Forbidden)")
    class MarkAsNotDonePastDueTests {

        @Test
        @DisplayName("should reject reopening DONE todo when due date has passed")
        void markAsNotDone_doneWithPastDue_forbidden() {
            // Create a DONE todo with past due date using builder
            Todo done = new TodoBuilder("Buy milk", pastTime)
                    .withStatus(TodoStatus.DONE)
                    .withCompletedAt(Instant.now().minusSeconds(60))
                    .build();
            todoRepository.save(done);

            assertThrows(
                ImmutableTodoException.class,
                () -> todoService.markAsNotDone(done.getId())
            );
        }

        @Test
        @DisplayName("should prevent reopening expired tasks")
        void markAsNotDone_expiredTask_preventReopening() {
            // Create an expired DONE todo
            Todo expired = new TodoBuilder("Report due", pastTime)
                    .withStatus(TodoStatus.DONE)
                    .withCompletedAt(Instant.now().minusSeconds(100))
                    .build();
            todoRepository.save(expired);

            assertThrows(
                ImmutableTodoException.class,
                () -> todoService.markAsNotDone(expired.getId())
            );
        }
    }

    @Nested
    @DisplayName("Mark as NOT_DONE - NOT_DONE Status (Forbidden)")
    class MarkAsNotDoneNotDoneTests {

        @Test
        @DisplayName("should reject marking NOT_DONE todo as NOT_DONE again")
        void markAsNotDone_alreadyNotDone_forbidden() {
            Todo created = todoService.createTodo("Buy milk", futureTime);

            assertThrows(
                InvalidTodoStatusTransitionException.class,
                () -> todoService.markAsNotDone(created.getId())
            );
        }
    }

    @Nested
    @DisplayName("Mark as NOT_DONE - PAST_DUE Status (Immutable)")
    class MarkAsNotDonePastDueImmuteTests {

        @Test
        @DisplayName("should reject marking PAST_DUE todo as NOT_DONE (immutable)")
        void markAsNotDone_pastDue_forbidden() {
            Todo pastDue = new TodoBuilder("Buy milk", pastTime)
                    .withStatus(TodoStatus.PAST_DUE)
                    .build();
            todoRepository.save(pastDue);

            // PAST_DUE status blocks the transition
            assertThrows(
                (Class<? extends Throwable>) ImmutableTodoException.class,
                () -> todoService.markAsNotDone(pastDue.getId())
            );
        }
    }

    @Nested
    @DisplayName("Filtering by Status")
    class FilterByStatusTests {

        @Test
        @DisplayName("should filter todos by single status")
        void getTodosByStatuses_singleStatus_returnsMatching() {
            Todo notDone = todoService.createTodo("Task 1", futureTime);
            Todo done = todoService.createTodo("Task 2", futureTime);
            todoService.markAsDone(done.getId());

            Pageable pageable = PageRequest.of(0, 10);
            Page<Todo> result = todoService.getTodosByStatuses(List.of(TodoStatus.NOT_DONE), pageable);

            assertEquals(1, result.getTotalElements());
            assertTrue(result.getContent().stream()
                    .allMatch(t -> t.getStatus() == TodoStatus.NOT_DONE));
            assertTrue(result.getContent().stream()
                    .anyMatch(t -> t.getId().equals(notDone.getId())));
        }

        @Test
        @DisplayName("should filter todos by multiple statuses")
        void getTodosByStatuses_multipleStatuses_returnsMatching() {
            Todo notDone = todoService.createTodo("Task 1", futureTime);
            Todo done = todoService.createTodo("Task 2", futureTime);
            todoService.markAsDone(done.getId());
            Todo pastDue = new TodoBuilder("Task 3", pastTime)
                    .withStatus(TodoStatus.PAST_DUE)
                    .build();
            todoRepository.save(pastDue);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Todo> result = todoService.getTodosByStatuses(
                    List.of(TodoStatus.NOT_DONE, TodoStatus.DONE),
                    pageable
            );

            assertEquals(2, result.getTotalElements());
            assertTrue(result.getContent().stream()
                    .allMatch(t -> t.getStatus() == TodoStatus.NOT_DONE || t.getStatus() == TodoStatus.DONE));
        }

        @Test
        @DisplayName("should respect pagination parameters")
        void getTodosByStatuses_pagination_respectsLimit() {
            for (int i = 0; i < 15; i++) {
                todoService.createTodo("Task " + i, futureTime);
            }

            Pageable pageable = PageRequest.of(0, 10);
            Page<Todo> firstPage = todoService.getTodosByStatuses(List.of(TodoStatus.NOT_DONE), pageable);

            assertEquals(10, firstPage.getContent().size());
            assertEquals(15, firstPage.getTotalElements());
            assertTrue(firstPage.hasNext());

            Pageable secondPageable = PageRequest.of(1, 10);
            Page<Todo> secondPage = todoService.getTodosByStatuses(List.of(TodoStatus.NOT_DONE), secondPageable);

            assertEquals(5, secondPage.getContent().size());
            assertFalse(secondPage.hasNext());
        }

        @Test
        @DisplayName("should return empty page when no todos match status filter")
        void getTodosByStatuses_noMatches_returnsEmpty() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Todo> result = todoService.getTodosByStatuses(List.of(TodoStatus.PAST_DUE), pageable);

            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
        }
    }

    @Nested
    @DisplayName("Transition to PAST_DUE - Scheduled Task")
    class TransitionToPastDueTests {

        @Test
        @DisplayName("should find NOT_DONE todos with past due date")
        void findNotDoneWithPastDueDate_returnsOverdueNotDone() {
            Todo notDoneWithFuture = todoService.createTodo("Task 1", futureTime);

            // Create NOT_DONE todo with past due date using builder
            Todo notDoneWithPast = new TodoBuilder("Task 2", pastTime)
                    .withStatus(TodoStatus.NOT_DONE)
                    .build();
            todoRepository.save(notDoneWithPast);

            // Create DONE todo with past due date
            Todo done = new TodoBuilder("Task 3", pastTime)
                    .withStatus(TodoStatus.DONE)
                    .withCompletedAt(Instant.now().minusSeconds(100))
                    .build();
            todoRepository.save(done);

            Collection<Todo> overdue = todoService.findNotDoneWithPastDueDate(Instant.now());

            assertEquals(1, overdue.size());
            assertTrue(overdue.stream()
                    .anyMatch(t -> t.getId().equals(notDoneWithPast.getId())));
            assertTrue(overdue.stream()
                    .noneMatch(t -> t.getId().equals(notDoneWithFuture.getId())));
            assertTrue(overdue.stream()
                    .noneMatch(t -> t.getId().equals(done.getId())));
        }

        @Test
        @DisplayName("should not return DONE todos even if past due")
        void findNotDoneWithPastDueDate_ignoreDoneTodos() {
            Todo done = todoService.createTodo("Completed task", futureTime);
            todoService.markAsDone(done.getId());

            // Manually create a DONE todo with past due date
            Todo donePastDue = new TodoBuilder("Old completed", pastTime)
                    .withStatus(TodoStatus.DONE)
                    .withCompletedAt(Instant.now().minusSeconds(200))
                    .build();
            todoRepository.save(donePastDue);

            Collection<Todo> overdue = todoService.findNotDoneWithPastDueDate(Instant.now());

            assertTrue(overdue.stream()
                    .noneMatch(t -> t.getStatus() == TodoStatus.DONE));
        }

        @Test
        @DisplayName("should transition NOT_DONE todos to PAST_DUE")
        void transitionToPastDue_updatesPastDueNotDone() {
            Todo future = todoService.createTodo("Future task", futureTime);

            // Create NOT_DONE todos with past due dates
            Todo past1 = new TodoBuilder("Overdue 1", pastTime.minusSeconds(100))
                    .withStatus(TodoStatus.NOT_DONE)
                    .build();
            todoRepository.save(past1);

            Todo past2 = new TodoBuilder("Overdue 2", pastTime.minusSeconds(200))
                    .withStatus(TodoStatus.NOT_DONE)
                    .build();
            todoRepository.save(past2);

            int updated = todoService.transitionToPastDue(Instant.now());

            assertEquals(2, updated);

            // Verify status change in database
            Todo retrievedPast1 = todoRepository.findById(past1.getId()).orElse(null);
            Todo retrievedPast2 = todoRepository.findById(past2.getId()).orElse(null);
            Todo retrievedFuture = todoRepository.findById(future.getId()).orElse(null);

            assertNotNull(retrievedPast1);
            assertNotNull(retrievedPast2);
            assertNotNull(retrievedFuture);

            assertEquals(TodoStatus.PAST_DUE, retrievedPast1.getStatus());
            assertEquals(TodoStatus.PAST_DUE, retrievedPast2.getStatus());
            assertEquals(TodoStatus.NOT_DONE, retrievedFuture.getStatus());
        }

        @Test
        @DisplayName("should not transition DONE todos to PAST_DUE")
        void transitionToPastDue_ignoredDoneTodos() {
            Todo done = todoService.createTodo("Completed", futureTime);
            todoService.markAsDone(done.getId());

            Todo notDonePast = new TodoBuilder("Overdue", pastTime)
                    .withStatus(TodoStatus.NOT_DONE)
                    .build();
            todoRepository.save(notDonePast);

            int updated = todoService.transitionToPastDue(Instant.now());

            assertEquals(1, updated);

            // Verify DONE status is preserved
            Todo retrievedDone = todoRepository.findById(done.getId()).orElse(null);
            assertNotNull(retrievedDone);
            assertEquals(TodoStatus.DONE, retrievedDone.getStatus());
        }

        @Test
        @DisplayName("should handle empty transition correctly")
        void transitionToPastDue_noOverdueItems_returnsZero() {
            todoService.createTodo("Task 1", futureTime);
            todoService.createTodo("Task 2", futureTime);

            int updated = todoService.transitionToPastDue(Instant.now());

            assertEquals(0, updated);
        }

        @Test
        @DisplayName("should be idempotent - running twice has same effect")
        void transitionToPastDue_idempotent() {
            Todo past = new TodoBuilder("Overdue", pastTime)
                    .withStatus(TodoStatus.NOT_DONE)
                    .build();
            todoRepository.save(past);

            int updated1 = todoService.transitionToPastDue(Instant.now());
            int updated2 = todoService.transitionToPastDue(Instant.now());

            assertEquals(1, updated1);
            assertEquals(0, updated2); // Second run should update nothing
        }
    }

    @Nested
    @DisplayName("State Transition Matrix - Forbidden Transitions")
    class StateTransitionMatrixTests {

        @Test
        @DisplayName("NOT_DONE -> DONE: allowed when future due date")
        void notDone_to_done_allowed() {
            Todo todo = todoService.createTodo("Task", futureTime);
            assertDoesNotThrow(() -> todoService.markAsDone(todo.getId()));
        }

        @Test
        @DisplayName("NOT_DONE -> PAST_DUE: allowed via scheduler only")
        void notDone_to_pastDue_allowedViaScheduler() {
            Todo todo = new TodoBuilder("Task", pastTime).build();
            todoRepository.save(todo);
            int updated = todoService.transitionToPastDue(Instant.now());
            assertEquals(1, updated);
        }

        @Test
        @DisplayName("DONE -> NOT_DONE: allowed only if future due date")
        void done_to_notDone_allowedIfFutureDue() {
            Todo todo = todoService.createTodo("Task", futureTime);
            todoService.markAsDone(todo.getId());
            assertDoesNotThrow(() -> todoService.markAsNotDone(todo.getId()));
        }

        @Test
        @DisplayName("DONE -> NOT_DONE: forbidden if past due date")
        void done_to_notDone_forbiddenIfPastDue() {
            Todo done = new TodoBuilder("Task", pastTime)
                    .withStatus(TodoStatus.DONE)
                    .withCompletedAt(Instant.now().minusSeconds(100))
                    .build();
            todoRepository.save(done);

            assertThrows(ImmutableTodoException.class, () -> todoService.markAsNotDone(done.getId()));
        }

        @Test
        @DisplayName("DONE -> DONE: forbidden (no double marking)")
        void done_to_done_forbidden() {
            Todo todo = todoService.createTodo("Task", futureTime);
            todoService.markAsDone(todo.getId());

            assertThrows(InvalidTodoStatusTransitionException.class, () -> todoService.markAsDone(todo.getId()));
        }

        @Test
        @DisplayName("PAST_DUE -> any state: forbidden (immutable)")
        void pastDue_to_any_forbidden() {
            Todo pastDue = new TodoBuilder("Task", pastTime)
                    .withStatus(TodoStatus.PAST_DUE)
                    .build();
            todoRepository.save(pastDue);

            assertThrows(ImmutableTodoException.class, () -> todoService.markAsDone(pastDue.getId()));
            assertThrows(ImmutableTodoException.class, () -> todoService.markAsNotDone(pastDue.getId()));
            assertThrows(ImmutableTodoException.class,
                    () -> todoService.updateDescription(pastDue.getId(), "Updated"));
        }
    }

    @Nested
    @DisplayName("Transactional Consistency")
    class TransactionalConsistencyTests {

        @Test
        @DisplayName("should maintain consistency when exception occurs during update")
        void updateDescription_exceptionRollsBack() {
            Todo created = todoService.createTodo("Initial", futureTime);
            todoService.markAsDone(created.getId());

            assertThrows(
                IllegalTodoStateException.class,
                () -> todoService.updateDescription(created.getId(), "Updated")
            );

            // Verify description was not changed
            Todo retrieved = todoRepository.findById(created.getId()).orElse(null);
            assertNotNull(retrieved);
            assertEquals("Initial", retrieved.getDescription());
        }

        @Test
        @DisplayName("should ensure bulk transition is atomic")
        void transitionToPastDue_atomic() {
            Todo past1 = new TodoBuilder("Overdue 1", pastTime).build();
            Todo past2 = new TodoBuilder("Overdue 2", pastTime).build();
            todoRepository.saveAll(List.of(past1, past2));

            int updated = todoService.transitionToPastDue(Instant.now());

            assertEquals(2, updated);
            // Verify both were updated atomically
            long pastDueCount = todoRepository.findByStatusIn(
                    List.of(TodoStatus.PAST_DUE),
                    PageRequest.of(0, 10)
            ).getTotalElements();

            assertEquals(2, pastDueCount);
        }
    }
}

