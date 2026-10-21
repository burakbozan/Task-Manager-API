package com.example.taskmanager.service;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceTest {
    private static final String OWNER_NAME = "alice";

    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private TaskService service;
    private Task task;
    private User owner;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        userRepository = mock(UserRepository.class);
        service = new TaskService(taskRepository, userRepository);
        owner = new User(OWNER_NAME, "hashed-password", Set.of("ROLE_USER"));
        task = new Task();
        task.setId(1L);
        task.setTitle("Prepare project");
        task.setOwner(owner);
        when(taskRepository.findByIdAndOwner_Username(1L, OWNER_NAME)).thenReturn(Optional.of(task));
    }

    @Test
    void listsOnlyTasksForAuthenticatedUser() {
        when(taskRepository.findAllByOwner_UsernameOrderByIdAsc(OWNER_NAME)).thenReturn(List.of(task));

        assertEquals(List.of(task), service.findAll(OWNER_NAME));
        verify(taskRepository).findAllByOwner_UsernameOrderByIdAsc(OWNER_NAME);
    }

    @Test
    void createIgnoresClientIdAndAssignsAuthenticatedOwner() {
        Task requested = new Task();
        requested.setId(99L);
        requested.setTitle("New task");
        when(userRepository.findByUsername(OWNER_NAME)).thenReturn(Optional.of(owner));
        when(taskRepository.save(requested)).thenReturn(requested);

        Task created = service.create(requested, OWNER_NAME);

        assertNull(created.getId());
        assertEquals(owner, created.getOwner());
        verify(taskRepository).save(requested);
    }

    @Test
    void updatesOnlyFieldsOwnedByTheAuthenticatedUser() {
        Task changes = new Task();
        changes.setTitle("Updated");
        changes.setDescription("Details");
        changes.setCompleted(true);
        when(taskRepository.save(task)).thenReturn(task);

        Task updated = service.update(1L, changes, OWNER_NAME);

        assertEquals("Updated", updated.getTitle());
        assertEquals("Details", updated.getDescription());
        assertEquals(true, updated.isCompleted());
        assertEquals(owner, updated.getOwner());
    }

    @Test
    void cannotReadOrDeleteAnotherUsersTask() {
        when(taskRepository.findByIdAndOwner_Username(1L, "bob")).thenReturn(Optional.empty());

        ResponseStatusException readException = assertThrows(
                ResponseStatusException.class,
                () -> service.findById(1L, "bob"));
        ResponseStatusException deleteException = assertThrows(
                ResponseStatusException.class,
                () -> service.delete(1L, "bob"));

        assertEquals(HttpStatus.NOT_FOUND, readException.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, deleteException.getStatusCode());
        verify(taskRepository, never()).delete(task);
    }
}
