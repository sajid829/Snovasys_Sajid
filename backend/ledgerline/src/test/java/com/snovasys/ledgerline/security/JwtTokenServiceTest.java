package com.snovasys.ledgerline.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.entity.UserEntity;
import com.snovasys.ledgerline.service.JwtTokenService;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenServiceTest {

    private final byte[] keyBytes = new byte[32];

    @Test
    void issuedTokenHasExpectedIdentityRoleAndExpiration() {
        var key = new SecretKeySpec(keyBytes, "HmacSHA256");
        var service = new JwtTokenService(
                new NimbusJwtEncoder(new ImmutableSecret<>(key)),
                "https://ledgerline.local",
                900);
        var user = user(Role.ACCOUNT_HOLDER, 4);

        var response = service.issueFor(user);
        var decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        var jwt = decoder.decode(response.accessToken());

        assertEquals(user.getId().toString(), jwt.getSubject());
        assertEquals("ACCOUNT_HOLDER", jwt.getClaimAsString("role"));
        assertEquals(4L, ((Number) jwt.getClaim("ver")).longValue());
        assertEquals("https://ledgerline.local", jwt.getIssuer().toString());
        assertEquals("Bearer", response.tokenType());
        assertEquals(jwt.getExpiresAt(), response.expiresAt());
    }

    @Test
    void decoderRejectsTamperedToken() {
        var key = new SecretKeySpec(keyBytes, "HmacSHA256");
        var service = new JwtTokenService(
                new NimbusJwtEncoder(new ImmutableSecret<>(key)),
                "https://ledgerline.local",
                900);
        String token = service.issueFor(user(Role.AUDITOR, 0)).accessToken();
        var decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        String tampered = token.substring(0, token.length() - 2) + "ab";

        assertThrows(JwtException.class, () -> decoder.decode(tampered));
    }

    @Test
    void tokenLifetimeOutsideConfiguredBoundsIsRejected() {
        var key = new SecretKeySpec(keyBytes, "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));

        assertThrows(
                IllegalStateException.class,
                () -> new JwtTokenService(encoder, "https://ledgerline.local", 10));
        assertThrows(
                IllegalStateException.class,
                () -> new JwtTokenService(encoder, "https://ledgerline.local", 3601));
    }

    private UserEntity user(Role role, long tokenVersion) {
        var user = Mockito.mock(UserEntity.class);
        UUID id = UUID.randomUUID();
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn("person@example.com");
        when(user.getRole()).thenReturn(role);
        when(user.getTokenVersion()).thenReturn(tokenVersion);
        return user;
    }
}
