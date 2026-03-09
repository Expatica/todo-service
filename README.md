# Todo Service

This is a simple Spring Boot service for managing a list of todo items.

## Service Description and Assumptions

The service provides a REST API to create, retrieve, update, and manage todo items. Each todo has a description, a due date, and a status.

The following assumptions are made:
- Todo items have one of three statuses: `NOT_DONE`, `DONE`, or `PAST_DUE`.
- A background scheduler runs periodically to transition `NOT_DONE` items to `PAST_DUE` when their due date has passed.
- The service uses an in-memory H2 database, so data is not persisted across service restarts.
- Once a due date has passed the todo becomes immutable. The API will disallow modification of any todo in this state
  regardless of the status.

## Tech Stack

- **Runtime Environment:** Java 25
- **Framework:** Spring Boot 4.0.3
- **Key Libraries:**
    - Spring Web MVC (for REST APIs)
    - Spring Data JPA (for database access)
    - H2 (in-memory database)
    - SpringDoc (for OpenAPI documentation)
    - Jackson (for JSON serialization)
    - JUnit 5 (for testing)

## How To

### Build the Service

To build the service, run the following command:

```bash
./gradlew build
```

### Run Automatic Tests

To run the automated tests, use the following command:

```bash
./gradlew test
```

### Run the Service Locally

You can run the service locally in one of two ways:

1.  **Using Gradle:**

    ```bash
    ./gradlew bootRun
    ```

2.  **Using Docker Compose:**

    ```bash
    docker compose up
    ```

The service will be available at `http://localhost:8080/todos`. A demo SPA will be available at
`http://localhost:/8080`. You can view the API documentation at `http://localhost:8080/swagger-ui.html`.

## Notes
### Location of business logic in the domain object
An intentional decision was made to implement domain specific business logic in the domain object, `Todo.java`.
In a larger application, this would ensure that any `Todo` created or used outside of the `TodoService` would comply 
with the intended business logic.

### ClockProvider and TimeProvider via dependency injection
The application initially used `Instant.now()` to get current timestamps. The `ClockProvider` and `TimeProvider` were 
introduced to ensure idempotent tests; `now()` is no longer an unknown dependent when the tests are ran and the internal
clock of the system.

