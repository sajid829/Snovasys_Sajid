package com.snovasys.ledgerline.dto;

import com.snovasys.ledgerline.entity.Role;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt) {
}
