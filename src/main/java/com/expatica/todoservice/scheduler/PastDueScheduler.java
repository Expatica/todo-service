package com.expatica.todoservice.scheduler;

import com.expatica.todoservice.repository.TodoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * PastDueScheduler is responsible for actively transitioning TODO items from NOT_DONE
 * to PAST_DUE status when their due date has passed.
 *
 * <p>This scheduler runs periodically to ensure deterministic state and consistency across
 * the system. Rather than relying on lazy evaluation, this active transition guarantees that:</p>
 *
 * <ul>
 *   <li>Filtering and querying operations always return accurate current state</li>
 *   <li>Past-due transitions happen deterministically at scheduled intervals</li>
 *   <li>No hidden state changes occur during read operations</li>
 * </ul>
 *
 * <p>The scheduled task targets all TODO items where:
 * <ul>
 *   <li>status == NOT_DONE</li>
 *   <li>dueAt < now(UTC)</li>
 * </ul>
 * </p>
 */
@Component
public class PastDueScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PastDueScheduler.class);

    private final TodoRepository todoRepository;
    private final Clock clock;

    public PastDueScheduler(TodoRepository todoRepository, Clock clock) {
        this.todoRepository = todoRepository;
        this.clock = clock;
    }

    /**
     * Scheduled task that runs every minute to transition overdue NOT_DONE todos to PAST_DUE.
     *
     * <p>This method executes within a transactional boundary to ensure atomic bulk updates.
     * It uses a direct UPDATE query for efficiency rather than loading and updating individual entities.</p>
     */
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    @Transactional
    public void transitionNotDoneToPastDue() {
        Instant now = Instant.now(clock);
        int updated = todoRepository.updateNotDoneToPastDue(now);

        if (updated > 0) {
            logger.info("Transitioned {} TODO(s) from NOT_DONE to PAST_DUE", updated);
        }
    }
}

