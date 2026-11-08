package com.snovasys.ledgerline.service;

import com.snovasys.ledgerline.dto.AdminUserResponse;
import com.snovasys.ledgerline.dto.AdminUserUpdateRequest;
import com.snovasys.ledgerline.entity.Role;
import com.snovasys.ledgerline.entity.UserEntity;
import com.snovasys.ledgerline.exception.ResourceNotFoundException;
import com.snovasys.ledgerline.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponse update(UUID userId, UUID actingAdminId, AdminUserUpdateRequest request) {
        if (userId.equals(actingAdminId) && !request.enabled()) {
            throw new IllegalArgumentException("Administrators cannot disable their own account.");
        }
        UserEntity user = userRepository.findById(userId).orElseThrow(ResourceNotFoundException::new);
        if (userId.equals(actingAdminId) && user.getRole() != request.role()) {
            throw new IllegalArgumentException("Administrators cannot change their own role.");
        }
        boolean removesEnabledAdmin = user.getRole() == Role.ADMIN
                && user.isEnabled()
                && (request.role() != Role.ADMIN || !request.enabled());
        if (removesEnabledAdmin
                && userRepository.findAllByRoleAndEnabledTrue(Role.ADMIN).size() <= 1) {
            throw new IllegalArgumentException("At least one enabled administrator must remain.");
        }
        user.updateAccess(request.role(), request.enabled());
        return toResponse(user);
    }

    private AdminUserResponse toResponse(UserEntity user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt());
    }
}
