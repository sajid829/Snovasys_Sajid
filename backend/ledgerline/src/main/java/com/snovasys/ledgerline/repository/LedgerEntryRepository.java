package com.snovasys.ledgerline.repository;

import com.snovasys.ledgerline.entity.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {
}
