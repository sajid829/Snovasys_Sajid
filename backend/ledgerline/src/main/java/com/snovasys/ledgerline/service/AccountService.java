package com.snovasys.ledgerline.service;

import com.snovasys.ledgerline.dto.AccountResponse;
import com.snovasys.ledgerline.dto.CreateAccountRequest;
import com.snovasys.ledgerline.entity.BankAccountEntity;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.exception.ResourceNotFoundException;
import com.snovasys.ledgerline.repository.BankAccountRepository;
import com.snovasys.ledgerline.repository.UserRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final long FIRST_ACCOUNT_NUMBER = 1_000_000_000_000_000L;
    private static final long ACCOUNT_NUMBER_BOUND = 10_000_000_000_000_000L;

    private final BankAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountService(BankAccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AccountResponse create(UUID ownerId, CreateAccountRequest request) {
        var owner = userRepository.findById(ownerId).orElseThrow(ResourceNotFoundException::new);
        for (int attempt = 0; attempt < 5; attempt++) {
            String accountNumber = Long.toString(
                    secureRandom.nextLong(FIRST_ACCOUNT_NUMBER, ACCOUNT_NUMBER_BOUND));
            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                var account = accountRepository.saveAndFlush(
                        new BankAccountEntity(accountNumber, owner, request.accountType()));
                return toResponse(account);
            }
        }
        throw new IllegalStateException("Could not allocate a unique account number.");
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(UUID userId, Role role) {
        var accounts = role == Role.ACCOUNT_HOLDER
                ? accountRepository.findAllByOwner_IdOrderByCreatedAtDesc(userId)
                : accountRepository.findAllByOrderByCreatedAtDesc();
        return accounts.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID accountId, UUID userId, Role role) {
        var account = role == Role.ACCOUNT_HOLDER
                ? accountRepository.findByIdAndOwner_Id(accountId, userId)
                : accountRepository.findById(accountId);
        return account.map(this::toResponse).orElseThrow(ResourceNotFoundException::new);
    }

    private AccountResponse toResponse(BankAccountEntity account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getStatus(),
                account.getCurrency(),
                account.getBalance().toPlainString(),
                account.getCreatedAt());
    }
}
