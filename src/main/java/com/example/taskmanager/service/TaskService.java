package com.example.taskmanager.service;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaskService {
    private final TaskRepository repository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public List<Task> findAll(String username) {
        return repository.findAllByOwner_UsernameOrderByIdAsc(username);
    }

    public Task findById(Long id, String username) {
        return findOwnedTask(id, username);
    }

    @Retryable(
            retryFor = TransientDataAccessException.class,
            maxAttemptsExpression = "${app.resilience.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${app.resilience.initial-delay-ms:100}", multiplier = 2))
    public Task create(Task task, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        task.setId(null);
        task.setOwner(owner);
        return repository.save(task);
    }

    @Retryable(
            retryFor = TransientDataAccessException.class,
            maxAttemptsExpression = "${app.resilience.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${app.resilience.initial-delay-ms:100}", multiplier = 2))
    public Task update(Long id, Task changes, String username) {
        Task task = findOwnedTask(id, username);
        task.setTitle(changes.getTitle());
        task.setDescription(changes.getDescription());
        task.setCompleted(changes.isCompleted());
        return repository.save(task);
    }

    @Retryable(
            retryFor = TransientDataAccessException.class,
            maxAttemptsExpression = "${app.resilience.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${app.resilience.initial-delay-ms:100}", multiplier = 2))
    public void delete(Long id, String username) {
        repository.delete(findOwnedTask(id, username));
    }

    private Task findOwnedTask(Long id, String username) {
        return repository.findByIdAndOwner_Username(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }
}
