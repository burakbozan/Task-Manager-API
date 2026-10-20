package com.example.taskmanager.controller;

import com.example.taskmanager.model.Agent;
import com.example.taskmanager.model.AgentLifecycleStage;
import com.example.taskmanager.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/agents")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {
    private final AgentService service;

    public AgentController(AgentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List the authenticated user's agents")
    public List<AgentResponse> getAgents(Authentication authentication) {
        return service.findAll(authentication.getName()).stream().map(AgentResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an agent")
    public AgentResponse getAgent(@PathVariable Long id, Authentication authentication) {
        return AgentResponse.from(service.findById(id, authentication.getName()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an agent in the draft stage")
    public AgentResponse createAgent(
            @Valid @RequestBody AgentRequest request,
            Authentication authentication) {
        return AgentResponse.from(service.create(
                request.name(), request.description(), authentication.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an agent's name and description")
    public AgentResponse updateAgent(
            @PathVariable Long id,
            @Valid @RequestBody AgentRequest request,
            Authentication authentication) {
        return AgentResponse.from(service.update(
                id, request.name(), request.description(), authentication.getName()));
    }

    @PostMapping("/{id}/lifecycle")
    @Operation(summary = "Advance an agent to its next lifecycle stage")
    public AgentResponse transitionAgent(
            @PathVariable Long id,
            @Valid @RequestBody LifecycleRequest request,
            Authentication authentication) {
        return AgentResponse.from(service.transition(id, request.stage(), authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an agent")
    public void deleteAgent(@PathVariable Long id, Authentication authentication) {
        service.delete(id, authentication.getName());
    }

    public record AgentRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 2000) String description) {
    }

    public record LifecycleRequest(@NotNull AgentLifecycleStage stage) {
    }

    public record AgentResponse(
            Long id,
            String name,
            String description,
            AgentLifecycleStage stage,
            Instant createdAt,
            Instant updatedAt) {
        static AgentResponse from(Agent agent) {
            return new AgentResponse(
                    agent.getId(),
                    agent.getName(),
                    agent.getDescription(),
                    agent.getStage(),
                    agent.getCreatedAt(),
                    agent.getUpdatedAt());
        }
    }
}
