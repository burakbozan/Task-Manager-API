package com.example.taskmanager.service;

import com.example.taskmanager.model.Agent;
import com.example.taskmanager.model.AgentLifecycleStage;
import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.AgentRepository;
import com.example.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentServiceTest {
    private static final String USERNAME = "alice";

    private AgentRepository agentRepository;
    private AgentService service;
    private Agent agent;

    @BeforeEach
    void setUp() {
        agentRepository = mock(AgentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        service = new AgentService(agentRepository, userRepository);
        User owner = new User(USERNAME, "hashed-password", java.util.Set.of("ROLE_USER"));
        agent = new Agent("Research agent", "Summarizes research", owner);
        when(agentRepository.findByIdAndOwner_Username(1L, USERNAME)).thenReturn(Optional.of(agent));
    }

    @Test
    void advancesOnlyToTheNextLifecycleStage() {
        when(agentRepository.save(agent)).thenReturn(agent);

        Agent result = service.transition(1L, AgentLifecycleStage.DEVELOPMENT, USERNAME);

        assertEquals(AgentLifecycleStage.DEVELOPMENT, result.getStage());
        verify(agentRepository).save(agent);
    }

    @Test
    void rejectsSkippingLifecycleStages() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.transition(1L, AgentLifecycleStage.TESTING, USERNAME));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(agentRepository, never()).save(agent);
    }

    @Test
    void repeatingCurrentStageIsIdempotent() {
        Agent result = service.transition(1L, AgentLifecycleStage.DRAFT, USERNAME);

        assertEquals(AgentLifecycleStage.DRAFT, result.getStage());
        verify(agentRepository, never()).save(agent);
    }

    @Test
    void doesNotExposeAgentsOwnedByAnotherUser() {
        when(agentRepository.findByIdAndOwner_Username(1L, "bob")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.findById(1L, "bob"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
