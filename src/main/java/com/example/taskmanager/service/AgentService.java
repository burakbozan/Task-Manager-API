package com.example.taskmanager.service;

import com.example.taskmanager.model.Agent;
import com.example.taskmanager.model.AgentLifecycleStage;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.AgentRepository;
import com.example.taskmanager.repository.UserRepository;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AgentService {
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;

    public AgentService(AgentRepository agentRepository, UserRepository userRepository) {
        this.agentRepository = agentRepository;
        this.userRepository = userRepository;
    }

    public List<Agent> findAll(String username) {
        return agentRepository.findAllByOwner_UsernameOrderByIdAsc(username);
    }

    public Agent findById(Long id, String username) {
        return findOwnedAgent(id, username);
    }

    @Retryable(
            retryFor = TransientDataAccessException.class,
            maxAttemptsExpression = "${app.resilience.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${app.resilience.initial-delay-ms:100}", multiplier = 2))
    public Agent create(String name, String description, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        return agentRepository.save(new Agent(name, description, owner));
    }

    @Retryable(
            retryFor = TransientDataAccessException.class,
            maxAttemptsExpression = "${app.resilience.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${app.resilience.initial-delay-ms:100}", multiplier = 2))
    public Agent update(Long id, String name, String description, String username) {
        Agent agent = findOwnedAgent(id, username);
        agent.setName(name);
        agent.setDescription(description);
        return agentRepository.save(agent);
    }

    @Retryable(
            retryFor = TransientDataAccessException.class,
            maxAttemptsExpression = "${app.resilience.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${app.resilience.initial-delay-ms:100}", multiplier = 2))
    public Agent transition(Long id, AgentLifecycleStage target, String username) {
        Agent agent = findOwnedAgent(id, username);
        AgentLifecycleStage current = agent.getStage();
        if (target == current) {
            return agent;
        }
        if (current.next() != target) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid lifecycle transition from " + current + " to " + target);
        }
        agent.setStage(target);
        return agentRepository.save(agent);
    }

    @Retryable(
            retryFor = TransientDataAccessException.class,
            maxAttemptsExpression = "${app.resilience.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${app.resilience.initial-delay-ms:100}", multiplier = 2))
    public void delete(Long id, String username) {
        agentRepository.delete(findOwnedAgent(id, username));
    }

    private Agent findOwnedAgent(Long id, String username) {
        return agentRepository.findByIdAndOwner_Username(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));
    }
}
