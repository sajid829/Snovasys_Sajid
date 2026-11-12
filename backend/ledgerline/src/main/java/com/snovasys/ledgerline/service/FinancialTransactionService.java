package com.snovasys.ledgerline.service;

import com.snovasys.ledgerline.dto.TransactionResponse;
import com.snovasys.ledgerline.entity.BankAccountEntity;
import com.snovasys.ledgerline.entity.FinancialTransactionEntity;
import com.snovasys.ledgerline.entity.FinancialTransactionType;
import com.snovasys.ledgerline.entity.LedgerEntryEntity;
import com.snovasys.ledgerline.entity.LedgerEntryType;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.exception.IdempotencyConflictException;
import com.snovasys.ledgerline.exception.InsufficientFundsException;
import com.snovasys.ledgerline.exception.ResourceNotFoundException;
import com.snovasys.ledgerline.repository.BankAccountRepository;
import com.snovasys.ledgerline.repository.FinancialTransactionRepository;
import com.snovasys.ledgerline.repository.LedgerEntryRepository;
import com.snovasys.ledgerline.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialTransactionService {

    private static final String CASH_ACCOUNT = "CASH";

    private final BankAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public FinancialTransactionService(
            BankAccountRepository accountRepository,
            UserRepository userRepository,
            FinancialTransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public TransactionResponse deposit(UUID actorId, UUID accountId, BigDecimal amount, String idempotencyKey) {
        BigDecimal normalizedAmount = validateAmount(amount);
        String key = validateIdempotencyKey(idempotencyKey);
        var existing = findExisting(actorId, key, FinancialTransactionType.DEPOSIT, null, accountId,
                normalizedAmount);
        if (existing != null) {
            return transactionResponse(existing, accountId);
        }

        var account = lockAccount(accountId);
        ensureOwner(account, actorId);
        ensureActive(account);
        existing = findExisting(actorId, key, FinancialTransactionType.DEPOSIT, null, accountId,
                normalizedAmount);
        if (existing != null) {
            return transactionResponse(existing, accountId);
        }

        account.credit(normalizedAmount);
        var transaction = createTransaction(actorId, FinancialTransactionType.DEPOSIT, null, account,
                normalizedAmount, key);
        saveEntries(
                new LedgerEntryEntity(transaction, null, CASH_ACCOUNT, LedgerEntryType.DEBIT, normalizedAmount),
                new LedgerEntryEntity(transaction, account, null, LedgerEntryType.CREDIT, normalizedAmount));
        return transactionResponse(transaction, accountId);
    }

    @Transactional
    public TransactionResponse withdraw(UUID actorId, UUID accountId, BigDecimal amount, String idempotencyKey) {
        BigDecimal normalizedAmount = validateAmount(amount);
        String key = validateIdempotencyKey(idempotencyKey);
        var existing = findExisting(actorId, key, FinancialTransactionType.WITHDRAWAL, accountId, null,
                normalizedAmount);
        if (existing != null) {
            return transactionResponse(existing, accountId);
        }

        var account = lockAccount(accountId);
        ensureOwner(account, actorId);
        ensureActive(account);
        existing = findExisting(actorId, key, FinancialTransactionType.WITHDRAWAL, accountId, null,
                normalizedAmount);
        if (existing != null) {
            return transactionResponse(existing, accountId);
        }

        try {
            account.debit(normalizedAmount);
        } catch (IllegalStateException exception) {
            throw new InsufficientFundsException();
        }
        var transaction = createTransaction(actorId, FinancialTransactionType.WITHDRAWAL, account, null,
                normalizedAmount, key);
        saveEntries(
                new LedgerEntryEntity(transaction, account, null, LedgerEntryType.DEBIT, normalizedAmount),
                new LedgerEntryEntity(transaction, null, CASH_ACCOUNT, LedgerEntryType.CREDIT, normalizedAmount));
        return transactionResponse(transaction, accountId);
    }

    @Transactional
    public TransactionResponse transfer(
            UUID actorId, UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount, String idempotencyKey) {
        if (sourceAccountId == null || destinationAccountId == null
                || sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different.");
        }
        BigDecimal normalizedAmount = validateAmount(amount);
        String key = validateIdempotencyKey(idempotencyKey);
        var existing = findExisting(actorId, key, FinancialTransactionType.TRANSFER,
                sourceAccountId, destinationAccountId, normalizedAmount);
        if (existing != null) {
            return transactionResponse(existing, sourceAccountId);
        }

        List<UUID> accountIds = new ArrayList<>(List.of(sourceAccountId, destinationAccountId));
        accountIds.sort(Comparator.naturalOrder());
        var lockedAccounts = new java.util.HashMap<UUID, BankAccountEntity>();
        for (UUID id : accountIds) {
            lockedAccounts.put(id, lockAccount(id));
        }
        var source = lockedAccounts.get(sourceAccountId);
        var destination = lockedAccounts.get(destinationAccountId);
        ensureOwner(source, actorId);
        ensureOwner(destination, actorId);
        ensureActive(source);
        ensureActive(destination);
        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new IllegalArgumentException("Accounts must use the same currency.");
        }
        existing = findExisting(actorId, key, FinancialTransactionType.TRANSFER,
                sourceAccountId, destinationAccountId, normalizedAmount);
        if (existing != null) {
            return transactionResponse(existing, sourceAccountId);
        }

        try {
            source.debit(normalizedAmount);
        } catch (IllegalStateException exception) {
            throw new InsufficientFundsException();
        }
        destination.credit(normalizedAmount);
        var transaction = createTransaction(actorId, FinancialTransactionType.TRANSFER, source, destination,
                normalizedAmount, key);
        saveEntries(
                new LedgerEntryEntity(transaction, source, null, LedgerEntryType.DEBIT, normalizedAmount),
                new LedgerEntryEntity(transaction, destination, null, LedgerEntryType.CREDIT, normalizedAmount));
        return transactionResponse(transaction, sourceAccountId);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> history(UUID actorId, UUID accountId, Role role) {
        var account = role == Role.ACCOUNT_HOLDER
                ? accountRepository.findByIdAndOwner_Id(accountId, actorId)
                : accountRepository.findById(accountId);
        var requestedAccount = account.orElseThrow(ResourceNotFoundException::new);
        return transactionRepository
                .findAllBySourceAccount_IdOrDestinationAccount_IdOrderByCreatedAtDesc(accountId, accountId)
                .stream()
                .map(transaction -> transactionResponse(transaction, requestedAccount.getId()))
                .toList();
    }

    private FinancialTransactionEntity createTransaction(
            UUID actorId,
            FinancialTransactionType type,
            BankAccountEntity source,
            BankAccountEntity destination,
            BigDecimal amount,
            String idempotencyKey) {
        var actor = userRepository.findById(actorId).orElseThrow(ResourceNotFoundException::new);
        return transactionRepository.saveAndFlush(
                new FinancialTransactionEntity(actor, type, source, destination, amount, idempotencyKey));
    }

    private void saveEntries(LedgerEntryEntity debit, LedgerEntryEntity credit) {
        ledgerEntryRepository.saveAllAndFlush(List.of(debit, credit));
    }

    private BankAccountEntity lockAccount(UUID accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(ResourceNotFoundException::new);
    }

    private void ensureOwner(BankAccountEntity account, UUID actorId) {
        if (account.getOwner().getId() == null || !account.getOwner().getId().equals(actorId)) {
            throw new ResourceNotFoundException();
        }
    }

    private void ensureActive(BankAccountEntity account) {
        if (account.getStatus() != com.snovasys.ledgerline.entity.AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Transactions are allowed only on active accounts.");
        }
    }

    private FinancialTransactionEntity findExisting(
            UUID actorId,
            String key,
            FinancialTransactionType type,
            UUID sourceId,
            UUID destinationId,
            BigDecimal amount) {
        var existing = transactionRepository.findByActor_IdAndIdempotencyKey(actorId, key);
        if (existing.isEmpty()) {
            return null;
        }
        var transaction = existing.get();
        boolean matches = transaction.getType() == type
                && sameAccount(transaction.getSourceAccount(), sourceId)
                && sameAccount(transaction.getDestinationAccount(), destinationId)
                && transaction.getAmount().compareTo(amount) == 0;
        if (!matches) {
            throw new IdempotencyConflictException();
        }
        return transaction;
    }

    private boolean sameAccount(BankAccountEntity account, UUID accountId) {
        return account == null ? accountId == null : account.getId().equals(accountId);
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17) {
            throw new IllegalArgumentException("Amount must be positive and fit NUMERIC(19,2).");
        }
        return amount.setScale(2);
    }

    private String validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 128) {
            throw new IllegalArgumentException("A valid idempotency key is required.");
        }
        return key;
    }

    private TransactionResponse transactionResponse(FinancialTransactionEntity transaction, UUID accountId) {
        BankAccountEntity counterparty;
        if (transaction.getType() == FinancialTransactionType.DEPOSIT) {
            counterparty = null;
        } else if (transaction.getType() == FinancialTransactionType.WITHDRAWAL) {
            counterparty = null;
        } else {
            counterparty = transaction.getSourceAccount().getId().equals(accountId)
                    ? transaction.getDestinationAccount()
                    : transaction.getSourceAccount();
        }
        String currency = transaction.getSourceAccount() != null
                ? transaction.getSourceAccount().getCurrency()
                : transaction.getDestinationAccount().getCurrency();
        String direction = transaction.getDestinationAccount() != null
                && transaction.getDestinationAccount().getId().equals(accountId)
                ? LedgerEntryType.CREDIT.name()
                : LedgerEntryType.DEBIT.name();
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getStatus(),
                direction,
                transaction.getAmount().toPlainString(),
                currency,
                counterparty == null ? CASH_ACCOUNT : counterparty.getAccountNumber(),
                transaction.getCreatedAt());
    }
}
