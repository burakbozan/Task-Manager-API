package com.example.taskmanager.service;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.repository.TaskRepository;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {
    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public List<Task> findAll() {
        return repository.findAll();
    }

    @Retryable(
            retryFor = TransientDataAccessException.class,
            maxAttemptsExpression = "${app.resilience.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${app.resilience.initial-delay-ms:100}", multiplier = 2))
    public Task save(Task task) {
        return repository.save(task);
    }

    @Retryable(
            retryFor = TransientDataAccessException.class,
            maxAttemptsExpression = "${app.resilience.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${app.resilience.initial-delay-ms:100}", multiplier = 2))
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
