package com.snovasys.ledgerline.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snovasys.ledgerline.dto.CreateAccountRequest;
import com.snovasys.ledgerline.entity.AccountType;
import com.snovasys.ledgerline.entity.BankAccountEntity;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.entity.UserEntity;
import com.snovasys.ledgerline.exception.ResourceNotFoundException;
import com.snovasys.ledgerline.repository.BankAccountRepository;
import com.snovasys.ledgerline.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AccountServiceTest {

    private final BankAccountRepository accountRepository = Mockito.mock(BankAccountRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final AccountService accountService = new AccountService(accountRepository, userRepository);
    private final UUID ownerId = UUID.randomUUID();

    @Test
    void creationUsesAuthenticatedOwnerAndStartsAtExactZeroBalance() {
        var owner = new UserEntity("owner@example.com", "hash", Role.ACCOUNT_HOLDER);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
        when(accountRepository.saveAndFlush(any(BankAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = accountService.create(ownerId, new CreateAccountRequest(AccountType.SAVINGS));
        ArgumentCaptor<BankAccountEntity> accountCaptor =
                ArgumentCaptor.forClass(BankAccountEntity.class);
        verify(accountRepository).saveAndFlush(accountCaptor.capture());
        var account = accountCaptor.getValue();

        assertEquals(owner, account.getOwner());
        assertEquals(AccountType.SAVINGS, account.getAccountType());
        assertEquals("0.00", account.getBalance().toPlainString());
        assertInstanceOf(BigDecimal.class, account.getBalance());
        assertEquals(16, account.getAccountNumber().length());
        assertEquals("0.00", response.balance());
    }

    @Test
    void accountNumberCollisionIsCheckedAndRetriedBeforeSaving() {
        var owner = new UserEntity("owner@example.com", "hash", Role.ACCOUNT_HOLDER);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(accountRepository.existsByAccountNumber(any())).thenReturn(true, false);
        when(accountRepository.saveAndFlush(any(BankAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = accountService.create(ownerId, new CreateAccountRequest(AccountType.CHECKING));

        assertEquals(16, response.accountNumber().length());
        verify(accountRepository, org.mockito.Mockito.times(2)).existsByAccountNumber(any());
        verify(accountRepository).saveAndFlush(any(BankAccountEntity.class));
    }

    @Test
    void accountHolderCannotReadAnotherUsersAccount() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByIdAndOwner_Id(accountId, ownerId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.get(accountId, ownerId, Role.ACCOUNT_HOLDER));
        verify(accountRepository, never()).findById(accountId);
    }

    @Test
    void accountHolderListIsScopedToAuthenticatedOwner() {
        accountService.list(ownerId, Role.ACCOUNT_HOLDER);

        verify(accountRepository).findAllByOwner_IdOrderByCreatedAtDesc(ownerId);
        verify(accountRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void accountHolderDetailLookupIncludesAuthenticatedOwner() {
        UUID accountId = UUID.randomUUID();
        var owner = new UserEntity("owner@example.com", "hash", Role.ACCOUNT_HOLDER);
        var account = new BankAccountEntity("1234567890123456", owner, AccountType.CHECKING);
        when(accountRepository.findByIdAndOwner_Id(accountId, ownerId))
                .thenReturn(Optional.of(account));

        var response = accountService.get(accountId, ownerId, Role.ACCOUNT_HOLDER);

        assertEquals("1234567890123456", response.accountNumber());
        verify(accountRepository, never()).findById(accountId);
    }

    @Test
    void auditorCanReadAllAccountsWithoutWriteCapabilitiesInService() {
        accountService.list(ownerId, Role.AUDITOR);

        verify(accountRepository).findAllByOrderByCreatedAtDesc();
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void administratorAccountListIsNotOwnerScoped() {
        accountService.list(ownerId, Role.ADMIN);

        verify(accountRepository).findAllByOrderByCreatedAtDesc();
        verify(accountRepository, never()).findAllByOwner_IdOrderByCreatedAtDesc(any());
    }
}
