package com.snovasys.ledgerline.service;

import com.snovasys.ledgerline.dto.AuthResponse;
import com.snovasys.ledgerline.dto.LoginRequest;
import com.snovasys.ledgerline.dto.RegisterRequest;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.entity.UserEntity;
import com.snovasys.ledgerline.exception.DuplicateEmailException;
import com.snovasys.ledgerline.exception.InvalidCredentialsException;
import com.snovasys.ledgerline.exception.ResourceNotFoundException;
import com.snovasys.ledgerline.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        validateBcryptPasswordLength(request.password());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException();
        }

        UserEntity user = new UserEntity(
                email,
                passwordEncoder.encode(request.password()),
                Role.ACCOUNT_HOLDER);
        user = userRepository.saveAndFlush(user);
        return jwtTokenService.issueFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        String candidateHash = user == null ? dummyPasswordHash : user.getPasswordHash();
        boolean passwordWithinLimit =
                request.password().getBytes(StandardCharsets.UTF_8).length <= 72;
        String passwordForCheck = passwordWithinLimit ? request.password() : "invalid-password";
        boolean passwordMatches = passwordEncoder.matches(passwordForCheck, candidateHash);
        if (user == null || !user.isEnabled() || !passwordWithinLimit || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return jwtTokenService.issueFor(user);
    }

    @Transactional
    public void logout(UUID userId, long tokenVersion) {
        UserEntity user = userRepository.findById(userId).orElseThrow(ResourceNotFoundException::new);
        if (user.getTokenVersion() != tokenVersion) {
            throw new InvalidCredentialsException();
        }
        user.invalidateTokens();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes.");
        }
    }
}
