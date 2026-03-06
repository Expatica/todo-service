package com.expatica.todoservice.repository;

import com.expatica.todoservice.domain.Todo;
import com.expatica.todoservice.domain.TodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface TodoRepository extends JpaRepository<Todo, UUID> {

    /**
     * Find all todos with any of the given statuses, with pagination support.
     *
     * @param statuses Collection of TodoStatus values to filter by
     * @param pageable Pagination and sorting configuration
     * @return Page of matching todos
     */
    @Query("SELECT t FROM Todo t WHERE t.status IN :statuses")
    Page<Todo> findByStatusIn(
            @Param("statuses") Collection<TodoStatus> statuses,
            Pageable pageable
    );

    /**
     * Find all NOT_DONE todos with a due date in the past (for scheduling).
     * This is used to transition todos to PAST_DUE status.
     *
     * @param now Current instant in UTC
     * @return Collection of todos that should transition to PAST_DUE
     */
    @Query("SELECT t FROM Todo t WHERE t.status = 'NOT_DONE' AND t.dueAt < :now")
    Collection<Todo> findNotDoneWithPastDueDate(@Param("now") Instant now);

    /**
     * Bulk update todos from NOT_DONE to PAST_DUE status.
     * Used by the scheduled task to transition overdue todos.
     *
     * @param now Current instant in UTC
     * @return Number of todos updated
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Todo t SET t.status = 'PAST_DUE' WHERE t.status = 'NOT_DONE' AND t.dueAt < :now")
    int updateNotDoneToPastDue(@Param("now") Instant now);
}
