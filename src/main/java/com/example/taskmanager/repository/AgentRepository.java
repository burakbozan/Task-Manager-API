package com.example.taskmanager.repository;

import com.example.taskmanager.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Long> {
    List<Agent> findAllByOwner_UsernameOrderByIdAsc(String username);

    Optional<Agent> findByIdAndOwner_Username(Long id, String username);
}
