package com.snovasys.ledgerline.dto;

import com.snovasys.ledgerline.entity.FinancialTransactionStatus;
import com.snovasys.ledgerline.entity.FinancialTransactionType;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        FinancialTransactionType type,
        FinancialTransactionStatus status,
        String direction,
        String amount,
        String currency,
        String counterpartyAccount,
        Instant createdAt) {
}
