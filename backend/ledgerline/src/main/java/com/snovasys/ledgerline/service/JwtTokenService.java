package com.snovasys.ledgerline.service;

import com.snovasys.ledgerline.dto.AuthResponse;
import com.snovasys.ledgerline.dto.AuthUserResponse;
import com.snovasys.ledgerline.entity.UserEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expirationSeconds;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.expiration-seconds:900}") long expirationSeconds) {
        if (expirationSeconds < 60 || expirationSeconds > 3600) {
            throw new IllegalStateException(
                    "LEDGERLINE_JWT_EXPIRATION_SECONDS must be between 60 and 3600.");
        }
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    public AuthResponse issueFor(UserEntity user) {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);
        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("role", user.getRole().name())
                .claim("ver", user.getTokenVersion())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
        return new AuthResponse(
                new AuthUserResponse(user.getId(), user.getEmail(), user.getRole()),
                accessToken,
                "Bearer",
                expiresAt);
    }
}
