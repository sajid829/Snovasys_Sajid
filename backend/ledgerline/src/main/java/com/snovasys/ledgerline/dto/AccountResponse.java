package com.snovasys.ledgerline.dto;

import com.snovasys.ledgerline.entity.AccountStatus;
import com.snovasys.ledgerline.entity.AccountType;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        AccountType accountType,
        AccountStatus status,
        String currency,
        String balance,
        Instant createdAt) {
}
