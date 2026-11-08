package com.snovasys.ledgerline.repository;

import com.snovasys.ledgerline.entity.UserEntity;
import com.snovasys.ledgerline.entity.Role;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<UserEntity> findAllByRoleAndEnabledTrue(Role role);
}
