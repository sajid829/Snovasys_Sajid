package com.snovasys.ledgerline.controller;

import com.snovasys.ledgerline.dto.MoneyRequest;
import com.snovasys.ledgerline.dto.TransactionResponse;
import com.snovasys.ledgerline.dto.TransferRequest;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.service.FinancialTransactionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FinancialTransactionController {

    private final FinancialTransactionService transactionService;

    public FinancialTransactionController(FinancialTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/accounts/{accountId}/deposits")
    public TransactionResponse deposit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accountId,
            @Valid @RequestBody MoneyRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return transactionService.deposit(
                authenticatedUserId(jwt), accountId, request.amount(), idempotencyKey);
    }

    @PostMapping("/accounts/{accountId}/withdrawals")
    public TransactionResponse withdraw(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accountId,
            @Valid @RequestBody MoneyRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return transactionService.withdraw(
                authenticatedUserId(jwt), accountId, request.amount(), idempotencyKey);
    }

    @PostMapping("/transfers")
    public TransactionResponse transfer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return transactionService.transfer(
                authenticatedUserId(jwt),
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount(),
                idempotencyKey);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public List<TransactionResponse> history(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accountId) {
        return transactionService.history(
                authenticatedUserId(jwt),
                accountId,
                Role.valueOf(jwt.getClaimAsString("role")));
    }

    private UUID authenticatedUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
