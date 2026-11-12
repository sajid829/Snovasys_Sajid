package com.snovasys.ledgerline.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snovasys.ledgerline.entity.AccountType;
import com.snovasys.ledgerline.entity.BankAccountEntity;
import com.snovasys.ledgerline.entity.FinancialTransactionEntity;
import com.snovasys.ledgerline.entity.FinancialTransactionType;
import com.snovasys.ledgerline.entity.LedgerEntryEntity;
import com.snovasys.ledgerline.entity.LedgerEntryType;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.entity.UserEntity;
import com.snovasys.ledgerline.exception.IdempotencyConflictException;
import com.snovasys.ledgerline.exception.InsufficientFundsException;
import com.snovasys.ledgerline.exception.ResourceNotFoundException;
import com.snovasys.ledgerline.repository.BankAccountRepository;
import com.snovasys.ledgerline.repository.FinancialTransactionRepository;
import com.snovasys.ledgerline.repository.LedgerEntryRepository;
import com.snovasys.ledgerline.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.stream.StreamSupport;

class FinancialTransactionServiceTest {

    private final BankAccountRepository accountRepository = Mockito.mock(BankAccountRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final FinancialTransactionRepository transactionRepository =
            Mockito.mock(FinancialTransactionRepository.class);
    private final LedgerEntryRepository ledgerEntryRepository = Mockito.mock(LedgerEntryRepository.class);
    private final UUID actorId = UUID.randomUUID();
    private final UUID sourceId = UUID.randomUUID();
    private final UUID destinationId = UUID.randomUUID();
    private UserEntity owner;
    private BankAccountEntity source;
    private BankAccountEntity destination;
    private FinancialTransactionService service;

    @BeforeEach
    void setUp() {
        service = new FinancialTransactionService(
                accountRepository, userRepository, transactionRepository, ledgerEntryRepository);
        owner = new UserEntity("owner@example.com", "hash", Role.ACCOUNT_HOLDER);
        ReflectionTestUtils.setField(owner, "id", actorId);
        source = account("1111111111111111", sourceId, owner);
        destination = account("2222222222222222", destinationId, owner);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(owner));
        when(transactionRepository.findByActor_IdAndIdempotencyKey(actorId, "request-1"))
                .thenReturn(Optional.empty());
        when(transactionRepository.saveAndFlush(any(FinancialTransactionEntity.class)))
                .thenAnswer(invocation -> {
                    var transaction = invocation.getArgument(0, FinancialTransactionEntity.class);
                    ReflectionTestUtils.setField(transaction, "id", UUID.randomUUID());
                    ReflectionTestUtils.setField(transaction, "createdAt", Instant.now());
                    return transaction;
                });
    }

    @Test
    void depositCreditsAccountAndCreatesBalancedDebitAndCredit() {
        when(accountRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));

        var result = service.deposit(actorId, sourceId, new BigDecimal("25.50"), "request-1");

        assertEquals("25.50", source.getBalance().toPlainString());
        assertEquals("DEPOSIT", result.type().name());
        assertBalancedEntries(LedgerEntryType.DEBIT, LedgerEntryType.CREDIT, null, source);
    }

    @Test
    void withdrawalDebitsAccountAndCreatesBalancedEntries() {
        source.credit(new BigDecimal("40.00"));
        when(accountRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));

        var result = service.withdraw(actorId, sourceId, new BigDecimal("12.35"), "request-1");

        assertEquals("27.65", source.getBalance().toPlainString());
        assertEquals("WITHDRAWAL", result.type().name());
        assertBalancedEntries(LedgerEntryType.DEBIT, LedgerEntryType.CREDIT, source, null);
    }

    @Test
    void insufficientFundsDoesNotPersistTransactionOrLedger() {
        source.credit(new BigDecimal("5.00"));
        when(accountRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));

        assertThrows(
                InsufficientFundsException.class,
                () -> service.withdraw(actorId, sourceId, new BigDecimal("5.01"), "request-1"));

        assertEquals("5.00", source.getBalance().toPlainString());
        verify(transactionRepository, never()).saveAndFlush(any());
        verify(ledgerEntryRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void transferMovesMoneyAndCreatesMatchingLedgerEntries() {
        source.credit(new BigDecimal("100.00"));
        when(accountRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(destinationId)).thenReturn(Optional.of(destination));

        var result = service.transfer(
                actorId, sourceId, destinationId, new BigDecimal("30.25"), "request-1");

        assertEquals("69.75", source.getBalance().toPlainString());
        assertEquals("30.25", destination.getBalance().toPlainString());
        assertEquals("TRANSFER", result.type().name());
        assertBalancedEntries(LedgerEntryType.DEBIT, LedgerEntryType.CREDIT, source, destination);
    }

    @Test
    void sameAccountTransferIsRejectedBeforeAnyDatabaseAccess() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.transfer(actorId, sourceId, sourceId, new BigDecimal("1.00"), "request-1"));

        verify(accountRepository, never()).findByIdForUpdate(any());
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void idempotencyKeyReplaysTheOriginalSuccessfulTransaction() {
        var prior = new FinancialTransactionEntity(
                owner, FinancialTransactionType.DEPOSIT, null, source,
                new BigDecimal("25.00"), "request-1");
        ReflectionTestUtils.setField(prior, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(prior, "createdAt", Instant.now());
        when(transactionRepository.findByActor_IdAndIdempotencyKey(actorId, "request-1"))
                .thenReturn(Optional.of(prior));

        var response = service.deposit(actorId, sourceId, new BigDecimal("25.00"), "request-1");

        assertEquals(prior.getId(), response.id());
        verify(accountRepository, never()).findByIdForUpdate(any());
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void reusingIdempotencyKeyForDifferentAmountIsRejected() {
        var prior = new FinancialTransactionEntity(
                owner, FinancialTransactionType.DEPOSIT, null, source,
                new BigDecimal("25.00"), "request-1");
        when(transactionRepository.findByActor_IdAndIdempotencyKey(actorId, "request-1"))
                .thenReturn(Optional.of(prior));

        assertThrows(
                IdempotencyConflictException.class,
                () -> service.deposit(actorId, sourceId, new BigDecimal("26.00"), "request-1"));
    }

    @Test
    void accountHolderCannotTransactOnAnotherOwnersAccount() {
        var otherOwner = new UserEntity("other@example.com", "hash", Role.ACCOUNT_HOLDER);
        ReflectionTestUtils.setField(otherOwner, "id", UUID.randomUUID());
        var privateAccount = account("3333333333333333", sourceId, otherOwner);
        when(accountRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(privateAccount));

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.deposit(actorId, sourceId, new BigDecimal("1.00"), "request-1"));
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void invalidMoneyValuesAreRejectedBeforeAccountChanges() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.deposit(actorId, sourceId, new BigDecimal("0.001"), "request-1"));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.deposit(actorId, sourceId, new BigDecimal("0.00"), "request-1"));
        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    private void assertBalancedEntries(
            LedgerEntryType expectedDebit,
            LedgerEntryType expectedCredit,
            BankAccountEntity expectedDebitAccount,
            BankAccountEntity expectedCreditAccount) {
        verify(ledgerEntryRepository).saveAllAndFlush(ArgumentMatchers.argThat(entries -> {
            var entryList = StreamSupport.stream(entries.spliterator(), false).toList();
            if (entryList.size() != 2) {
                return false;
            }
            LedgerEntryEntity debit = entryList.stream()
                    .filter(entry -> entry.getEntryType() == expectedDebit)
                    .findFirst()
                    .orElse(null);
            LedgerEntryEntity credit = entryList.stream()
                    .filter(entry -> entry.getEntryType() == expectedCredit)
                    .findFirst()
                    .orElse(null);
            return debit != null && credit != null
                    && debit.getAmount().compareTo(credit.getAmount()) == 0
                    && (expectedDebitAccount == null
                        ? "CASH".equals(debit.getSystemAccountCode())
                        : debit.getAccount() == expectedDebitAccount)
                    && (expectedCreditAccount == null
                        ? "CASH".equals(credit.getSystemAccountCode())
                        : credit.getAccount() == expectedCreditAccount);
        }));
    }

    private BankAccountEntity account(String number, UUID id, UserEntity accountOwner) {
        var account = new BankAccountEntity(number, accountOwner, AccountType.CHECKING);
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }
}
