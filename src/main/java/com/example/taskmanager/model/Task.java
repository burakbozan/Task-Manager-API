package com.example.taskmanager.model;

import jakarta.persistence.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A task managed by the authenticated user")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Database-generated task identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Short task title", example = "Prepare project")
    private String title;

    @Schema(description = "Additional details about the task", example = "Set up the Task Manager API")
    private String description;

    @Schema(description = "Whether the task has been completed", example = "false")
    private boolean completed;
}
