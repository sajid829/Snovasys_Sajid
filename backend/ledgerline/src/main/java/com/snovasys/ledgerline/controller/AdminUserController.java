package com.snovasys.ledgerline.controller;

import com.snovasys.ledgerline.dto.AdminUserResponse;
import com.snovasys.ledgerline.dto.AdminUserUpdateRequest;
import com.snovasys.ledgerline.service.AdminUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserResponse> list() {
        return adminUserService.list();
    }

    @PatchMapping("/{userId}")
    public AdminUserResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserUpdateRequest request) {
        return adminUserService.update(
                userId,
                UUID.fromString(jwt.getSubject()),
                request);
    }
}
