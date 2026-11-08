CREATE TABLE ledgerline.app_user (
    id uuid PRIMARY KEY,
    email varchar(254) NOT NULL,
    password_hash varchar(100) NOT NULL,
    role varchar(20) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    token_version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT app_user_role_check CHECK (role IN ('ADMIN', 'ACCOUNT_HOLDER', 'AUDITOR'))
);

CREATE UNIQUE INDEX app_user_email_lower_unique ON ledgerline.app_user (lower(email));

CREATE TABLE ledgerline.bank_account (
    id uuid PRIMARY KEY,
    account_number varchar(16) NOT NULL,
    owner_id uuid NOT NULL,
    account_type varchar(20) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    currency varchar(3) NOT NULL DEFAULT 'INR',
    balance numeric(19, 2) NOT NULL DEFAULT 0.00,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT bank_account_number_unique UNIQUE (account_number),
    CONSTRAINT bank_account_owner_fk FOREIGN KEY (owner_id)
        REFERENCES ledgerline.app_user (id) ON DELETE RESTRICT,
    CONSTRAINT bank_account_type_check CHECK (account_type IN ('CHECKING', 'SAVINGS')),
    CONSTRAINT bank_account_status_check CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT bank_account_balance_nonnegative CHECK (balance >= 0)
);

CREATE INDEX bank_account_owner_created_idx
    ON ledgerline.bank_account (owner_id, created_at DESC);
