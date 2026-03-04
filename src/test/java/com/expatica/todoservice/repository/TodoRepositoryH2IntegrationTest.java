package com.expatica.todoservice.repository;

import com.expatica.todoservice.domain.Todo;
import com.expatica.todoservice.domain.TodoBuilder;
import com.expatica.todoservice.domain.TodoStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests to verify H2 database connectivity and schema verification.
 * These tests ensure that:
 * - H2 database is properly configured
 * - Connection pooling works
 * - JPA/Hibernate DDL generation creates the expected schema
 * - Transactions work correctly
 */
@DataJpaTest
@ActiveProfiles("test")
public class TodoRepositoryH2IntegrationTest {

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void verifyH2DatabaseConnection() {
        // Verify DataSource is properly configured
        assertNotNull(dataSource, "DataSource should be injected");

        // Try to get a connection
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Should be able to obtain a database connection");
            assertFalse(connection.isClosed(), "Connection should be open");
        } catch (Exception e) {
            fail("Failed to establish H2 database connection: " + e.getMessage());
        }
    }

    @Test
    void verifyH2DatabaseIsH2Driver() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String databaseProductName = metadata.getDatabaseProductName();

            assertEquals("H2", databaseProductName, "Should be connected to H2 database");
            assertTrue(metadata.getURL().contains("h2:mem"), "Should be using in-memory H2 database");
        }
    }

    @Test
    void verifyTodoTableExists() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();

            // Check for TODO table
            try (ResultSet tables = metadata.getTables(null, null, "TODO", null)) {
                assertTrue(tables.next(), "TODO table should exist in schema");
                assertEquals("TODO", tables.getString("TABLE_NAME"));
            }
        }
    }

    @Test
    void verifyTodoTableColumns() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();

            try (ResultSet columns = metadata.getColumns(null, null, "TODO", null)) {
                int columnCount = 0;
                while (columns.next()) {
                    columnCount++;
                    String columnName = columns.getString("COLUMN_NAME");

                    // Verify expected columns exist
                    assertTrue(
                        columnName.matches("(ID|DESCRIPTION|DUE_AT|CREATED_AT|COMPLETED_AT|STATUS)"),
                        "Unexpected column: " + columnName
                    );
                }

                assertEquals(6, columnCount, "TODO table should have exactly 6 columns");
            }
        }
    }

    @Test
    void verifyDataPersistenceToH2() {
        // Create and save a Todo
        Instant future = Instant.now().plusSeconds(3600);
        Todo todo = new Todo("H2 Integration Test", future);

        Todo saved = todoRepository.save(todo);
        assertNotNull(saved.getId(), "Saved Todo should have an ID");

        // Flush to ensure data is written to H2
        todoRepository.flush();

        // Retrieve from database
        Todo retrieved = todoRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved, "Should retrieve Todo from H2 database");
        assertEquals("H2 Integration Test", retrieved.getDescription());
        assertEquals(TodoStatus.NOT_DONE, retrieved.getStatus());
    }

    @Test
    void verifyDataIsolationBetweenTests() {
        // This test verifies that @DataJpaTest provides transaction isolation
        // Each test method should have a clean H2 database state

        long count = todoRepository.count();
        assertEquals(0, count, "Each test should start with empty database due to @DataJpaTest transaction rollback");
    }

    @Test
    void verifyH2TransactionSupport() {
        Instant future = Instant.now().plusSeconds(3600);

        // Save multiple todos in a transaction
        Todo todo1 = todoRepository.save(new Todo("Todo 1", future));
        Todo todo2 = todoRepository.save(new Todo("Todo 2", future));

        todoRepository.flush();

        // Verify both were saved
        assertEquals(2, todoRepository.count());

        // Both should exist
        assertTrue(todoRepository.existsById(todo1.getId()));
        assertTrue(todoRepository.existsById(todo2.getId()));
    }

    @Test
    void verifyH2SupportsUpdates() {
        Instant future = Instant.now().plusSeconds(3600);
        Todo todo = new Todo("Original description", future);
        Todo saved = todoRepository.save(todo);

        todoRepository.flush();

        // Retrieve and update
        Todo retrieved = todoRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved);

        retrieved.changeDescription("Updated description");
        todoRepository.save(retrieved);
        todoRepository.flush();

        // Verify update persisted to H2
        Todo updated = todoRepository.findById(saved.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("Updated description", updated.getDescription());
    }

    @Test
    void verifyH2SupportsDeletes() {
        Instant future = Instant.now().plusSeconds(3600);
        Todo todo = new Todo("To be deleted", future);
        Todo saved = todoRepository.save(todo);

        todoRepository.flush();
        assertTrue(todoRepository.existsById(saved.getId()));

        // Delete
        todoRepository.deleteById(saved.getId());
        todoRepository.flush();

        // Verify deletion from H2
        assertFalse(todoRepository.existsById(saved.getId()));
    }

    @Test
    void verifyH2InMemoryDatabaseIsolation() {
        // Each @DataJpaTest gets its own H2 in-memory database instance
        // and each test method gets a transaction that's rolled back

        Instant future = Instant.now().plusSeconds(3600);
        todoRepository.save(new Todo("Test data", future));

        assertEquals(1, todoRepository.count());
        // After test completes, transaction rolls back and next test starts fresh
    }

    @Test
    void verifyH2CustomQueryExecution() {
        Instant now = Instant.now();
        Instant future = now.plusSeconds(3600);
        Instant past = now.minusSeconds(3600);

        // Create todos with various states
        todoRepository.save(new Todo("Future task", future));
        todoRepository.save(new TodoBuilder("Past task", past).build());

        todoRepository.flush();

        // Verify custom query works with H2
        var result = todoRepository.findNotDoneWithPastDueDate(now);
        assertEquals(1, result.size(), "Should find 1 past-due todo via custom query");
    }
}


