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
@Table(name = "financial_transaction", schema = "ledgerline")
public class FinancialTransactionEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false, updatable = false)
    private UserEntity actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private FinancialTransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", updatable = false)
    private BankAccountEntity sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id", updatable = false)
    private BankAccountEntity destinationAccount;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "idempotency_key", nullable = false, length = 128, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private FinancialTransactionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FinancialTransactionEntity() {
    }

    public FinancialTransactionEntity(
            UserEntity actor,
            FinancialTransactionType type,
            BankAccountEntity sourceAccount,
            BankAccountEntity destinationAccount,
            BigDecimal amount,
            String idempotencyKey) {
        this.actor = actor;
        this.type = type;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = FinancialTransactionStatus.COMPLETED;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UserEntity getActor() {
        return actor;
    }

    public FinancialTransactionType getType() {
        return type;
    }

    public BankAccountEntity getSourceAccount() {
        return sourceAccount;
    }

    public BankAccountEntity getDestinationAccount() {
        return destinationAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public FinancialTransactionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
