package com.example.taskmanager.repository;

import com.example.taskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByOwner_UsernameOrderByIdAsc(String username);

    Optional<Task> findByIdAndOwner_Username(Long id, String username);
}
