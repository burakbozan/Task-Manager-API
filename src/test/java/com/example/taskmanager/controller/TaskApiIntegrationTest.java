package com.example.taskmanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectsTaskOwnershipAndSupportsTaskAndAgentLifecycle() throws Exception {
        String suffix = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":" ","password":"test-password-123"}
                                """))
                .andExpect(status().isBadRequest());
        String alice = register("alice-" + suffix);
        String bob = register("bob-" + suffix);

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","description":"invalid","completed":false}
                                """))
                .andExpect(status().isBadRequest());

        MvcResult createdTask = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 999,
                                  "owner": {"username": "bob"},
                                  "title": "Owned task",
                                  "description": "Private task",
                                  "completed": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(999)))
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andReturn();
        long taskId = objectMapper.readTree(createdTask.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .header("Authorization", bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Attempted update","description":"No access","completed":true}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated task","description":"Changed","completed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated task"))
                .andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .header("Authorization", bearer(alice)))
                .andExpect(status().isNoContent());

        MvcResult createdAgent = mockMvc.perform(post("/api/agents")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Lifecycle agent","description":"Test"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stage").value("DRAFT"))
                .andReturn();
        JsonNode agent = objectMapper.readTree(createdAgent.getResponse().getContentAsString());
        long agentId = agent.get("id").asLong();

        mockMvc.perform(post("/api/agents/{id}/lifecycle", agentId)
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stage":"DEVELOPMENT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("DEVELOPMENT"));
        mockMvc.perform(post("/api/agents/{id}/lifecycle", agentId)
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stage":"DEPLOYED"}
                                """))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/agents/{id}", agentId)
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
    }

    private String register(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.AuthRequest(username, "test-password-123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
