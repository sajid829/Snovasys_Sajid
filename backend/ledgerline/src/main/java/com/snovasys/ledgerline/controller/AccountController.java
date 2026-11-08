package com.snovasys.ledgerline.controller;

import com.snovasys.ledgerline.dto.AccountResponse;
import com.snovasys.ledgerline.dto.CreateAccountRequest;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(
                authenticatedUserId(jwt),
                request));
    }

    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return accountService.list(authenticatedUserId(jwt), authenticatedRole(jwt));
    }

    @GetMapping("/{accountId}")
    public AccountResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accountId) {
        return accountService.get(
                accountId,
                authenticatedUserId(jwt),
                authenticatedRole(jwt));
    }

    private UUID authenticatedUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private Role authenticatedRole(Jwt jwt) {
        return Role.valueOf(jwt.getClaimAsString("role"));
    }
}
