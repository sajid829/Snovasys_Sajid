package com.snovasys.ledgerline.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snovasys.ledgerline.config.CorsConfiguration;
import com.snovasys.ledgerline.dto.AccountResponse;
import com.snovasys.ledgerline.dto.CreateAccountRequest;
import com.snovasys.ledgerline.entity.AccountStatus;
import com.snovasys.ledgerline.entity.AccountType;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.security.SecurityConfiguration;
import com.snovasys.ledgerline.service.AccountService;
import com.snovasys.ledgerline.service.AdminUserService;
import com.snovasys.ledgerline.service.AuthService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(
        controllers = {
            HealthController.class,
            AccountController.class,
            AdminUserController.class,
            AuthController.class
        },
        properties = {
            "DB_URL=jdbc:postgresql://localhost:5432/ledgerline_test",
            "DB_USERNAME=ledgerline_test",
            "DB_PASSWORD=test-only-password",
            "LEDGERLINE_JWT_SECRET=dGVzdC1zZWNyZXQtdGhhdC1pcy1hdC1sZWFzdC0zMi1ieXRlcy1sb25n"
        })
@Import({SecurityConfiguration.class, CorsConfiguration.class})
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private AuthService authService;

    private final UUID accountHolderId = UUID.randomUUID();
    private final UUID auditorId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    @Test
    void auditorCanReadAccountsButCannotCreateOrManageUsers() throws Exception {
        when(accountService.list(auditorId, Role.AUDITOR)).thenReturn(List.of());
        mockMvc.perform(get("/api/accounts").with(bearerToken(auditorId, Role.AUDITOR)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/accounts")
                        .with(bearerToken(auditorId, Role.AUDITOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountType\":\"SAVINGS\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users").with(bearerToken(auditorId, Role.AUDITOR)))
                .andExpect(status().isForbidden());

        verify(accountService).list(auditorId, Role.AUDITOR);
        verify(accountService, never()).create(any(), any());
        verify(adminUserService, never()).list();
    }

    @Test
    void accountHolderCanCreateAnAccountButCannotManageUsers() throws Exception {
        when(accountService.create(eq(accountHolderId), any(CreateAccountRequest.class)))
                .thenReturn(new AccountResponse(
                        UUID.randomUUID(),
                        "1234567890123456",
                        AccountType.SAVINGS,
                        AccountStatus.ACTIVE,
                        "INR",
                        "0.00",
                        Instant.now()));

        mockMvc.perform(post("/api/accounts")
                        .with(bearerToken(accountHolderId, Role.ACCOUNT_HOLDER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountType\":\"SAVINGS\","
                                + "\"ownerId\":\"" + adminId + "\","
                                + "\"userId\":\"" + adminId + "\","
                                + "\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/users").with(bearerToken(accountHolderId, Role.ACCOUNT_HOLDER)))
                .andExpect(status().isForbidden());

        verify(accountService).create(eq(accountHolderId), any(CreateAccountRequest.class));
        verify(adminUserService, never()).list();
    }

    @Test
    void unauthenticatedRequestsCannotReadAccountsOrManageUsers() throws Exception {
        mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountType\":\"CHECKING\"}"))
                .andExpect(status().isUnauthorized());

        verify(accountService, never()).list(any(), any());
        verify(accountService, never()).create(any(), any());
        verify(adminUserService, never()).list();
    }

    @Test
    void onlyAdminCanListAndUpdateUsers() throws Exception {
        when(adminUserService.list()).thenReturn(List.of());
        mockMvc.perform(get("/api/admin/users").with(bearerToken(adminId, Role.ADMIN)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/users/" + UUID.randomUUID())
                        .with(bearerToken(accountHolderId, Role.ACCOUNT_HOLDER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"AUDITOR\",\"enabled\":true}"))
                .andExpect(status().isForbidden());

        verify(adminUserService).list();
        verify(adminUserService, never()).update(any(), any(), any());
    }

    private RequestPostProcessor bearerToken(UUID userId, Role role) {
        String token = role.name().toLowerCase() + "-test-token";
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "HS256")
                .issuer("https://ledgerline.local")
                .subject(userId.toString())
                .issuedAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("role", role.name())
                .claim("ver", 0L)
                .build();
        when(jwtDecoder.decode(token)).thenReturn(jwt);
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    @Test
    void registrationAndLoginArePublicButLogoutRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
        verify(authService, never()).login(any());
    }
}
