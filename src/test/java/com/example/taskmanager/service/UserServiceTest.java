package com.example.taskmanager.service;

import com.example.taskmanager.model.User;
import com.example.taskmanager.repository.UserRepository;
import com.example.taskmanager.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserServiceTest {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserService(
                userRepository,
                passwordEncoder,
                new JwtUtil("", 86_400_000));
    }

    @Test
    void rejectsPasswordsLongerThanBcryptUtf8Limit() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.register("alice", "é".repeat(37)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void rejectsConcurrentDuplicateRegistrationAsConflict() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secure-password")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate username"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.register("alice", "secure-password"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void rejectsOverlongPasswordDuringLoginWithoutQueryingUserStore() {
        assertThrows(
                BadCredentialsException.class,
                () -> service.authenticate("alice", "é".repeat(37)));

        verifyNoInteractions(userRepository, passwordEncoder);
    }
}
