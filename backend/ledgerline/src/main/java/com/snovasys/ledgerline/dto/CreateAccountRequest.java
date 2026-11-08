package com.snovasys.ledgerline.dto;

import com.snovasys.ledgerline.entity.AccountType;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(@NotNull AccountType accountType) {
}
