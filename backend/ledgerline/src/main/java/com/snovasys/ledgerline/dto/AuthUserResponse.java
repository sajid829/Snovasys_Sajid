package com.snovasys.ledgerline.dto;

import com.snovasys.ledgerline.entity.Role;
import java.util.UUID;

public record AuthUserResponse(UUID id, String email, Role role) {
}
