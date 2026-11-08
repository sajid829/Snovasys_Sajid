package com.snovasys.ledgerline.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snovasys.ledgerline.dto.AuthResponse;
import com.snovasys.ledgerline.dto.AuthUserResponse;
import com.snovasys.ledgerline.dto.LoginRequest;
import com.snovasys.ledgerline.dto.RegisterRequest;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.entity.UserEntity;
import com.snovasys.ledgerline.exception.DuplicateEmailException;
import com.snovasys.ledgerline.exception.InvalidCredentialsException;
import com.snovasys.ledgerline.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

class AuthServiceTest {

    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final JwtTokenService jwtTokenService = org.mockito.Mockito.mock(JwtTokenService.class);
    private final AuthService authService =
            new AuthService(userRepository, passwordEncoder, jwtTokenService);
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(jwtTokenService.issueFor(any())).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            return new AuthResponse(
                    new AuthUserResponse(userId, user.getEmail(), user.getRole()),
                    "test-token",
                    "Bearer",
                    Instant.now().plusSeconds(900));
        });
    }

    @Test
    void registrationHashesPasswordAndAssignsAccountHolderRole() {
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(
                new RegisterRequest(" Person@Example.com ", "correct-horse-battery"));
        ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).saveAndFlush(saved.capture());

        assertEquals("person@example.com", saved.getValue().getEmail());
        assertNotEquals("correct-horse-battery", saved.getValue().getPasswordHash());
        assertTrue(passwordEncoder.matches(
                "correct-horse-battery", saved.getValue().getPasswordHash()));
        assertEquals(Role.ACCOUNT_HOLDER, saved.getValue().getRole());
        assertEquals(Role.ACCOUNT_HOLDER, response.user().role());
    }

    @Test
    void duplicateEmailIsRejectedWithoutSavingAnotherUser() {
        when(userRepository.findByEmail("person@example.com"))
                .thenReturn(Optional.of(new UserEntity(
                        "person@example.com", passwordEncoder.encode("existing-password"), Role.ACCOUNT_HOLDER)));

        assertThrows(
                DuplicateEmailException.class,
                () -> authService.register(new RegisterRequest(
                        "PERSON@example.com", "correct-horse-battery")));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void loginIssuesTokenOnlyForMatchingPassword() {
        UserEntity user = new UserEntity(
                "person@example.com",
                passwordEncoder.encode("correct-horse-battery"),
                Role.AUDITOR);
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(
                new LoginRequest("person@example.com", "correct-horse-battery"));

        assertEquals(Role.AUDITOR, response.user().role());
        verify(jwtTokenService).issueFor(user);
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownEmailWithSameError() {
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.empty());
        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest(
                        "person@example.com", "incorrect-password")));

        when(userRepository.findByEmail("person@example.com"))
                .thenReturn(Optional.of(new UserEntity(
                        "person@example.com",
                        passwordEncoder.encode("correct-horse-battery"),
                        Role.ACCOUNT_HOLDER)));
        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest(
                        "person@example.com", "incorrect-password")));
        verify(jwtTokenService, never()).issueFor(any());
    }

    @Test
    void logoutIncrementsTokenVersionToRevokeIssuedTokens() {
        UserEntity user = new UserEntity(
                "person@example.com", passwordEncoder.encode("correct-horse-battery"), Role.ACCOUNT_HOLDER);
        when(userRepository.findById(eq(userId))).thenReturn(Optional.of(user));

        authService.logout(userId, 0);

        assertEquals(1, user.getTokenVersion());
    }
}
