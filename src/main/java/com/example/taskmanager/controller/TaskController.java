package com.example.taskmanager.controller;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List tasks", description = "Returns tasks owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tasks returned",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class)),
                            examples = @ExampleObject(value = """
                                    [
                                      {
                                        "id": 1,
                                        "title": "Prepare project",
                                        "description": "Set up the Task Manager API",
                                        "completed": false
                                      }
                                    ]
                                    """))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public List<TaskResponse> getAllTasks(Authentication authentication) {
        return service.findAll(authentication.getName()).stream().map(TaskResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task", description = "Returns the specified task if owned by the authenticated user.")
    public TaskResponse getTask(@PathVariable Long id, Authentication authentication) {
        return TaskResponse.from(service.findById(id, authentication.getName()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a task", description = "Creates and returns a task.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Task created",
                    content = @Content(
                            schema = @Schema(implementation = TaskResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "title": "Prepare project",
                                      "description": "Set up the Task Manager API",
                                      "completed": false
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public TaskResponse createTask(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Task data to create",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TaskRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "title": "Prepare project",
                                      "description": "Set up the Task Manager API",
                                      "completed": false
                                    }
                                    """)))
            @Valid @RequestBody TaskRequest request,
            Authentication authentication) {
        return TaskResponse.from(service.create(toTask(request), authentication.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task", description = "Updates a task owned by the authenticated user.")
    public TaskResponse updateTask(
            @Parameter(description = "Identifier of the task to update", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            Authentication authentication) {
        return TaskResponse.from(service.update(id, toTask(request), authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a task", description = "Deletes the task with the specified identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deleted"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public void deleteTask(
            @Parameter(description = "Identifier of the task to delete", example = "1")
            @PathVariable Long id,
            Authentication authentication) {
        service.delete(id, authentication.getName());
    }

    private Task toTask(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setCompleted(request.completed());
        return task;
    }

    public record TaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            boolean completed) {
    }

    public record TaskResponse(
            Long id,
            String title,
            String description,
            boolean completed,
            Instant createdAt,
            Instant updatedAt) {
        static TaskResponse from(Task task) {
            return new TaskResponse(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.isCompleted(),
                    task.getCreatedAt(),
                    task.getUpdatedAt());
        }
    }
}
