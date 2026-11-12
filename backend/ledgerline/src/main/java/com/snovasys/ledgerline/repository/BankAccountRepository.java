package com.snovasys.ledgerline.repository;

import com.snovasys.ledgerline.entity.BankAccountEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface BankAccountRepository extends JpaRepository<BankAccountEntity, UUID> {

    List<BankAccountEntity> findAllByOwner_IdOrderByCreatedAtDesc(UUID ownerId);

    Optional<BankAccountEntity> findByIdAndOwner_Id(UUID id, UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from BankAccountEntity account where account.id = :id")
    Optional<BankAccountEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByAccountNumber(String accountNumber);

    default List<BankAccountEntity> findAllByOrderByCreatedAtDesc() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
