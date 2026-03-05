package com.expatica.todoservice.controller;

import com.expatica.todoservice.controller.dto.CreateTodoRequest;
import com.expatica.todoservice.controller.dto.TodoResponse;
import com.expatica.todoservice.domain.TodoStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for TodoController.
 * Tests HTTP endpoints and DTO mapping.
 */
@SpringBootTest
@AutoConfigureJsonTesters
class TodoControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testCreateTodo() throws Exception {
        // Arrange
        Instant futureDate = Instant.now().plus(7, ChronoUnit.DAYS);
        CreateTodoRequest request = new CreateTodoRequest("Buy milk", futureDate);

        // Act & Assert
        var result = mockMvc.perform(
                        post("/todos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.description").value("Buy milk"))
                .andExpect(jsonPath("$.status").value(TodoStatus.NOT_DONE.toString()))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        TodoResponse response = objectMapper.readValue(responseBody, TodoResponse.class);

        assertThat(response.description()).isEqualTo("Buy milk");
        assertThat(response.status()).isEqualTo(TodoStatus.NOT_DONE);
    }

    @Test
    void testGetTodoById() throws Exception {
        // Arrange - Create a todo first
        Instant futureDate = Instant.now().plus(7, ChronoUnit.DAYS);
        CreateTodoRequest createRequest = new CreateTodoRequest("Test todo", futureDate);

        var createResult = mockMvc.perform(
                        post("/todos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createRequest))
                )
                .andExpect(status().isCreated())
                .andReturn();

        TodoResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                TodoResponse.class
        );
        var todoId = created.id();

        // Act & Assert - Retrieve the todo
        mockMvc.perform(get("/todos/{id}", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todoId.toString()))
                .andExpect(jsonPath("$.description").value("Test todo"))
                .andExpect(jsonPath("$.status").value(TodoStatus.NOT_DONE.toString()));
    }

    @Test
    void testCreateTodoWithInvalidDescription() throws Exception {
        // Arrange
        Instant futureDate = Instant.now().plus(7, ChronoUnit.DAYS);
        CreateTodoRequest request = new CreateTodoRequest("", futureDate);

        // Act & Assert
        assertThatThrownBy(() ->
                mockMvc.perform(
                        post("/todos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
        ).isInstanceOf(Exception.class);

    }
}
