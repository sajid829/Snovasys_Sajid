package com.snovasys.ledgerline.dto;

import java.time.Instant;

public record AuthResponse(
        AuthUserResponse user,
        String accessToken,
        String tokenType,
        Instant expiresAt) {
}
