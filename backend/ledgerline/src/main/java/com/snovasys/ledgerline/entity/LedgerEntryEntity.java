package com.snovasys.ledgerline.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "ledger_entry", schema = "ledgerline")
public class LedgerEntryEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, updatable = false)
    private FinancialTransactionEntity transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", updatable = false)
    private BankAccountEntity account;

    @Column(name = "system_account_code", length = 32, updatable = false)
    private String systemAccountCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10, updatable = false)
    private LedgerEntryType entryType;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntryEntity() {
    }

    public LedgerEntryEntity(
            FinancialTransactionEntity transaction,
            BankAccountEntity account,
            String systemAccountCode,
            LedgerEntryType entryType,
            BigDecimal amount) {
        this.transaction = transaction;
        this.account = account;
        this.systemAccountCode = systemAccountCode;
        this.entryType = entryType;
        this.amount = amount;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public FinancialTransactionEntity getTransaction() {
        return transaction;
    }

    public BankAccountEntity getAccount() {
        return account;
    }

    public String getSystemAccountCode() {
        return systemAccountCode;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
