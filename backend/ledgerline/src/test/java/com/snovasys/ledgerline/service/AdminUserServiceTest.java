package com.snovasys.ledgerline.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snovasys.ledgerline.dto.AdminUserUpdateRequest;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.entity.UserEntity;
import com.snovasys.ledgerline.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AdminUserServiceTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final AdminUserService service = new AdminUserService(userRepository);

    @Test
    void preventsRemovingTheLastEnabledAdministrator() {
        UUID userId = UUID.randomUUID();
        UUID actingAdminId = UUID.randomUUID();
        UserEntity admin = new UserEntity("admin@example.com", "hash", Role.ADMIN);
        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(userRepository.findAllByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(List.of(admin));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.update(
                        userId,
                        actingAdminId,
                        new AdminUserUpdateRequest(Role.AUDITOR, true)));
        assertEquals(Role.ADMIN, admin.getRole());
        verify(userRepository).findAllByRoleAndEnabledTrue(Role.ADMIN);
    }

    @Test
    void accessChangesIncrementTokenVersion() {
        UUID userId = UUID.randomUUID();
        UUID actingAdminId = UUID.randomUUID();
        UserEntity accountHolder = new UserEntity(
                "person@example.com", "hash", Role.ACCOUNT_HOLDER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(accountHolder));

        var response = service.update(
                userId,
                actingAdminId,
                new AdminUserUpdateRequest(Role.AUDITOR, true));

        assertEquals(Role.AUDITOR, response.role());
        assertEquals(1, accountHolder.getTokenVersion());
    }

    @Test
    void administratorCannotDisableThemself() {
        UUID adminId = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.update(
                        adminId,
                        adminId,
                        new AdminUserUpdateRequest(Role.ADMIN, false)));
    }
}
