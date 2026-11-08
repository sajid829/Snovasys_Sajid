package com.snovasys.ledgerline.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(16, account.getAccountNumber().length());
        assertEquals("0.00", response.balance());
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
    void auditorCanReadAllAccountsWithoutWriteCapabilitiesInService() {
        accountService.list(ownerId, Role.AUDITOR);

        verify(accountRepository).findAllByOrderByCreatedAtDesc();
        verify(accountRepository, never()).saveAndFlush(any());
    }
}
