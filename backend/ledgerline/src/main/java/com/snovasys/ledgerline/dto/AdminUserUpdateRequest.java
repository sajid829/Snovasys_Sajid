package com.snovasys.ledgerline.dto;

import com.snovasys.ledgerline.entity.Role;
import jakarta.validation.constraints.NotNull;

public record AdminUserUpdateRequest(@NotNull Role role, @NotNull Boolean enabled) {
}
