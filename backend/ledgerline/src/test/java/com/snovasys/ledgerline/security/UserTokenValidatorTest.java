package com.snovasys.ledgerline.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.entity.UserEntity;
import com.snovasys.ledgerline.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

class UserTokenValidatorTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final UUID userId = UUID.randomUUID();
    private final UserTokenValidator validator = new UserTokenValidator(
            JwtValidators.createDefaultWithIssuer("https://ledgerline.local"),
            userRepository);
    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity("person@example.com", "hash", Role.ACCOUNT_HOLDER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    @Test
    void acceptsValidTokenForEnabledUserWithCurrentRoleAndVersion() {
        assertTrue(validator.validate(token("ACCOUNT_HOLDER", 0)).hasErrors() == false);
    }

    @Test
    void rejectsTokensAfterLogoutOrRoleChange() {
        user.invalidateTokens();
        assertFalse(validator.validate(token("ACCOUNT_HOLDER", 0)).hasErrors() == false);
        assertFalse(validator.validate(token("ADMIN", 1)).hasErrors() == false);
    }

    @Test
    void rejectsDisabledOrUnknownUsers() {
        user.updateAccess(Role.ACCOUNT_HOLDER, false);
        assertTrue(validator.validate(token("ACCOUNT_HOLDER", 0)).hasErrors());

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertTrue(validator.validate(token("ACCOUNT_HOLDER", 0)).hasErrors());
    }

    @Test
    void rejectsExpiredTokens() {
        Instant now = Instant.now();
        Jwt expired = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .issuer("https://ledgerline.local")
                .subject(userId.toString())
                .issuedAt(now.minusSeconds(1200))
                .expiresAt(now.minusSeconds(600))
                .claim("role", "ACCOUNT_HOLDER")
                .claim("ver", 0L)
                .build();
        assertTrue(validator.validate(expired).hasErrors());
    }

    private Jwt token(String role, long version) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .issuer("https://ledgerline.local")
                .subject(userId.toString())
                .issuedAt(now.minusSeconds(1))
                .expiresAt(now.plusSeconds(60))
                .claim("role", role)
                .claim("ver", version)
                .build();
    }
}
