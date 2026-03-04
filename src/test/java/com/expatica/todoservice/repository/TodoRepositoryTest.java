package com.expatica.todoservice.repository;

import com.expatica.todoservice.domain.Todo;
import com.expatica.todoservice.domain.TodoBuilder;
import com.expatica.todoservice.domain.TodoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class TodoRepositoryTest {

    @Autowired
    private TodoRepository todoRepository;

    private Instant futureTime;
    private Instant pastTime;

    @BeforeEach
    void setUp() {
        futureTime = Instant.now().plusSeconds(3600);
        pastTime = Instant.now().minusSeconds(3600);
    }

    @Test
    void saveAndFindById_createsAndRetrieves() {
        Todo todo = new Todo("Test task", futureTime);
        Todo saved = todoRepository.saveAndFlush(todo);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertTrue(todoRepository.existsById(saved.getId()));

        Todo found = todoRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Test task", found.getDescription());
        assertNotNull(found.getCreatedAt());
    }

    @Test
    void saveAndFindById_updatesAndRetrieves() {
        Todo todo = new Todo("Test task", futureTime);
        todo.markAsDone();
        Todo saved = todoRepository.save(todo);

        Todo found = todoRepository.findById(saved.getId()).orElse(null);

        assertNotNull(found);
        assertNotNull(found.getCompletedAt());

        found.markAsNotDone();
        found.changeDescription("new description");
        saved = todoRepository.save(found);

        found = todoRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("new description", found.getDescription());
        assertEquals(TodoStatus.NOT_DONE, saved.getStatus());
        assertNull(saved.getCompletedAt());
    }

    @Test
    void findByStatusIn_filtersCorrectly() {
        Todo todo1 = new Todo("Task 1", futureTime);
        Todo todo2 = new Todo("Task 2", futureTime);
        Todo todo3 = new Todo("Task 3", futureTime);

        todoRepository.saveAll(List.of(todo1, todo2, todo3));

        // Mark one as done
        todo1.markAsDone();
        todoRepository.save(todo1);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Todo> result = todoRepository.findByStatusIn(List.of(TodoStatus.NOT_DONE), pageable);

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .allMatch(t -> t.getStatus() == TodoStatus.NOT_DONE));
    }

    @Test
    void findByStatusIn_multipleStatuses() {
        Todo notDone = new Todo("Task 1", futureTime);
        Todo done = new Todo("Task 2", futureTime);
        Todo pastDue = new TodoBuilder("Task 3", pastTime).withStatus(TodoStatus.PAST_DUE).build();

        todoRepository.saveAll(List.of(notDone, done, pastDue));

        done.markAsDone();
        todoRepository.save(done);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Todo> result = todoRepository.findByStatusIn(
                List.of(TodoStatus.DONE, TodoStatus.PAST_DUE),
                pageable
        );

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .allMatch(t -> t.getStatus() == TodoStatus.DONE || t.getStatus() == TodoStatus.PAST_DUE));
    }

    @Test
    void findByStatusIn_withPaging() {
        for (int i = 0; i < 25; i++) {
            todoRepository.save(new Todo("Task " + i, futureTime));
        }

        Pageable page1 = PageRequest.of(0, 10, Sort.by("description").ascending());
        Page<Todo> result = todoRepository.findByStatusIn(List.of(TodoStatus.NOT_DONE), page1);

        assertEquals(25, result.getTotalElements());
        assertEquals(10, result.getContent().size());
        assertEquals(0, result.getNumber());
        assertEquals(3, result.getTotalPages());
    }

    @Test
    void findByStatusIn_withSorting() {
        todoRepository.save(new Todo("Alpha task", futureTime));
        todoRepository.save(new Todo("Beta task", futureTime));
        todoRepository.save(new Todo("Gamma task", futureTime));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("description").ascending());
        Page<Todo> result = todoRepository.findByStatusIn(List.of(TodoStatus.NOT_DONE), pageable);

        List<String> descriptions = result.getContent()
                .stream()
                .map(Todo::getDescription)
                .toList();

        assertEquals(List.of("Alpha task", "Beta task", "Gamma task"), descriptions);
    }

    @Test
    void findNotDoneWithPastDueDate_returnsOnlyNotDonePastDue() {
        Todo notDoneFuture = new Todo("Future task", futureTime);
        Todo notDonePast = new TodoBuilder("Past task", pastTime).build();
        Todo doneFuture = new Todo("Done future", futureTime);
        Todo donePast = new TodoBuilder("Done past", pastTime)
                .withCompletedAt(pastTime)
                .withStatus(TodoStatus.DONE)
                .build();

        todoRepository.saveAll(List.of(notDoneFuture, notDonePast, doneFuture, donePast));

        doneFuture.markAsDone();
        todoRepository.saveAll(List.of(doneFuture, donePast));

        Instant now = Instant.now();
        Collection<Todo> result = todoRepository.findNotDoneWithPastDueDate(now);

        assertEquals(1, result.size());
        Todo found = result.iterator().next();
        assertEquals("Past task", found.getDescription());
        assertEquals(TodoStatus.PAST_DUE, found.getStatus());
        assertTrue(found.getDueAt().isBefore(now));
    }

    @Test
    void updateNotDoneToPastDue_transitionsCorrectly() {
        Todo notDoneFuture = new Todo("Future task", futureTime);
        Todo notDonePast = new TodoBuilder("Past task", pastTime).build();
        Todo done = new Todo("Done task", futureTime);

        todoRepository.saveAll(List.of(notDoneFuture, notDonePast, done));

        done.markAsDone();
        todoRepository.save(done);

        Instant now = Instant.now();
        int updated = todoRepository.updateNotDoneToPastDue(now);

        assertEquals(1, updated);

        Todo transitioned = todoRepository.findById(notDonePast.getId()).orElse(null);
        assertNotNull(transitioned);
        assertEquals(TodoStatus.PAST_DUE, transitioned.getStatus());

        Todo unchanged = todoRepository.findById(done.getId()).orElse(null);
        assertNotNull(unchanged);
        assertEquals(TodoStatus.DONE, unchanged.getStatus());
    }

    @Test
    void updateNotDoneToPastDue_multipleUpdates() {
        for (int i = 0; i < 5; i++) {
            todoRepository.save(new TodoBuilder("Task " + i, pastTime).build());
        }

        Instant now = Instant.now();
        int updated = todoRepository.updateNotDoneToPastDue(now);

        assertEquals(5, updated);

        Page<Todo> result = todoRepository.findByStatusIn(
                List.of(TodoStatus.PAST_DUE),
                PageRequest.of(0, 10)
        );

        assertEquals(5, result.getTotalElements());
    }

    @Test
    void delete_removesCorrectly() {
        Todo todo = new Todo("Task to delete", futureTime);
        Todo saved = todoRepository.save(todo);

        assertTrue(todoRepository.existsById(saved.getId()));

        todoRepository.delete(saved);

        assertFalse(todoRepository.existsById(saved.getId()));
    }
}

