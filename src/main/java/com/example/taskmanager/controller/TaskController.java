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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "List tasks", description = "Returns all tasks.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tasks returned",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = Task.class)),
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
    public List<Task> getAllTasks() {
        return service.findAll();
    }

    @PostMapping
    @Operation(summary = "Create a task", description = "Creates and returns a task.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Task created",
                    content = @Content(
                            schema = @Schema(implementation = Task.class),
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
    public Task createTask(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Task data to create",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Task.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "title": "Prepare project",
                                      "description": "Set up the Task Manager API",
                                      "completed": false
                                    }
                                    """)))
            @RequestBody Task task) {
        return service.save(task);
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
            @PathVariable Long id) {
        service.delete(id);
    }
}
