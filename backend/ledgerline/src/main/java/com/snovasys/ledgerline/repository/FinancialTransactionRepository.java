package com.snovasys.ledgerline.repository;

import com.snovasys.ledgerline.entity.FinancialTransactionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransactionEntity, UUID> {

    Optional<FinancialTransactionEntity> findByActor_IdAndIdempotencyKey(UUID actorId, String idempotencyKey);

    List<FinancialTransactionEntity> findAllBySourceAccount_IdOrDestinationAccount_IdOrderByCreatedAtDesc(
            UUID sourceAccountId, UUID destinationAccountId);
}
