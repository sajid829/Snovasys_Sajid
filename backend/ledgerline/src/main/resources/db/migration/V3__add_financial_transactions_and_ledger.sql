CREATE TABLE ledgerline.financial_transaction (
    id uuid PRIMARY KEY,
    actor_id uuid NOT NULL,
    type varchar(20) NOT NULL,
    source_account_id uuid,
    destination_account_id uuid,
    amount numeric(19, 2) NOT NULL,
    idempotency_key varchar(128) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'COMPLETED',
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT financial_transaction_actor_fk FOREIGN KEY (actor_id)
        REFERENCES ledgerline.app_user (id) ON DELETE RESTRICT,
    CONSTRAINT financial_transaction_source_fk FOREIGN KEY (source_account_id)
        REFERENCES ledgerline.bank_account (id) ON DELETE RESTRICT,
    CONSTRAINT financial_transaction_destination_fk FOREIGN KEY (destination_account_id)
        REFERENCES ledgerline.bank_account (id) ON DELETE RESTRICT,
    CONSTRAINT financial_transaction_type_check CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    CONSTRAINT financial_transaction_status_check CHECK (status = 'COMPLETED'),
    CONSTRAINT financial_transaction_amount_check CHECK (amount > 0),
    CONSTRAINT financial_transaction_idempotency_key_check CHECK (length(btrim(idempotency_key)) > 0),
    CONSTRAINT financial_transaction_accounts_check CHECK (
        (type = 'DEPOSIT' AND source_account_id IS NULL AND destination_account_id IS NOT NULL)
        OR (type = 'WITHDRAWAL' AND source_account_id IS NOT NULL AND destination_account_id IS NULL)
        OR (type = 'TRANSFER' AND source_account_id IS NOT NULL AND destination_account_id IS NOT NULL
            AND source_account_id <> destination_account_id)
    ),
    CONSTRAINT financial_transaction_actor_key_unique UNIQUE (actor_id, idempotency_key)
);

CREATE INDEX financial_transaction_source_created_idx
    ON ledgerline.financial_transaction (source_account_id, created_at DESC);
CREATE INDEX financial_transaction_destination_created_idx
    ON ledgerline.financial_transaction (destination_account_id, created_at DESC);

CREATE TABLE ledgerline.ledger_entry (
    id uuid PRIMARY KEY,
    transaction_id uuid NOT NULL,
    account_id uuid,
    system_account_code varchar(32),
    entry_type varchar(10) NOT NULL,
    amount numeric(19, 2) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ledger_entry_transaction_fk FOREIGN KEY (transaction_id)
        REFERENCES ledgerline.financial_transaction (id) ON DELETE RESTRICT,
    CONSTRAINT ledger_entry_account_fk FOREIGN KEY (account_id)
        REFERENCES ledgerline.bank_account (id) ON DELETE RESTRICT,
    CONSTRAINT ledger_entry_entry_type_check CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ledger_entry_amount_check CHECK (amount > 0),
    CONSTRAINT ledger_entry_account_reference_check CHECK (
        (account_id IS NOT NULL AND system_account_code IS NULL)
        OR (account_id IS NULL AND system_account_code = 'CASH')
    )
);

CREATE INDEX ledger_entry_transaction_idx ON ledgerline.ledger_entry (transaction_id);
CREATE INDEX ledger_entry_account_created_idx ON ledgerline.ledger_entry (account_id, created_at DESC);

CREATE FUNCTION ledgerline.enforce_balanced_transaction() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    transaction_id_to_check uuid;
    transaction_row ledgerline.financial_transaction%ROWTYPE;
    entry_count integer;
    debit_count integer;
    credit_count integer;
    debit_total numeric(19, 2);
    credit_total numeric(19, 2);
    entry_amount_min numeric(19, 2);
    entry_amount_max numeric(19, 2);
    expected_debit_account uuid;
    expected_credit_account uuid;
    expected_debit_system varchar(32);
    expected_credit_system varchar(32);
BEGIN
    IF TG_TABLE_NAME = 'financial_transaction' THEN
        transaction_id_to_check := NEW.id;
    ELSIF TG_OP = 'DELETE' THEN
        transaction_id_to_check := OLD.transaction_id;
    ELSE
        transaction_id_to_check := NEW.transaction_id;
    END IF;
    SELECT * INTO transaction_row
    FROM ledgerline.financial_transaction
    WHERE id = transaction_id_to_check;

    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    SELECT count(*),
           count(*) FILTER (WHERE entry_type = 'DEBIT'),
           count(*) FILTER (WHERE entry_type = 'CREDIT'),
           COALESCE(sum(amount) FILTER (WHERE entry_type = 'DEBIT'), 0),
           COALESCE(sum(amount) FILTER (WHERE entry_type = 'CREDIT'), 0),
           min(amount),
           max(amount)
    INTO entry_count, debit_count, credit_count, debit_total, credit_total, entry_amount_min, entry_amount_max
    FROM ledgerline.ledger_entry
    WHERE transaction_id = transaction_id_to_check;

    IF transaction_row.type = 'DEPOSIT' THEN
        expected_debit_account := NULL;
        expected_debit_system := 'CASH';
        expected_credit_account := transaction_row.destination_account_id;
        expected_credit_system := NULL;
    ELSIF transaction_row.type = 'WITHDRAWAL' THEN
        expected_debit_account := transaction_row.source_account_id;
        expected_debit_system := NULL;
        expected_credit_account := NULL;
        expected_credit_system := 'CASH';
    ELSE
        expected_debit_account := transaction_row.source_account_id;
        expected_debit_system := NULL;
        expected_credit_account := transaction_row.destination_account_id;
        expected_credit_system := NULL;
    END IF;

    IF entry_count <> 2 OR debit_count <> 1 OR credit_count <> 1
       OR debit_total <> credit_total
       OR entry_amount_min <> transaction_row.amount OR entry_amount_max <> transaction_row.amount
       OR NOT EXISTS (
           SELECT 1 FROM ledgerline.ledger_entry
           WHERE transaction_id = transaction_id_to_check AND entry_type = 'DEBIT'
             AND account_id IS NOT DISTINCT FROM expected_debit_account
             AND system_account_code IS NOT DISTINCT FROM expected_debit_system
       )
       OR NOT EXISTS (
           SELECT 1 FROM ledgerline.ledger_entry
           WHERE transaction_id = transaction_id_to_check AND entry_type = 'CREDIT'
             AND account_id IS NOT DISTINCT FROM expected_credit_account
             AND system_account_code IS NOT DISTINCT FROM expected_credit_system
       ) THEN
        RAISE EXCEPTION 'Financial transaction % must have exactly two matching balanced ledger entries',
            transaction_id_to_check USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER financial_transaction_requires_balanced_entries
AFTER INSERT ON ledgerline.financial_transaction
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION ledgerline.enforce_balanced_transaction();

CREATE CONSTRAINT TRIGGER ledger_entry_keeps_transaction_balanced
AFTER INSERT OR UPDATE OR DELETE ON ledgerline.ledger_entry
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION ledgerline.enforce_balanced_transaction();

CREATE FUNCTION ledgerline.prevent_financial_record_changes() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Financial records are append-only' USING ERRCODE = '55000';
END;
$$;

CREATE TRIGGER ledger_entry_is_immutable
BEFORE UPDATE OR DELETE ON ledgerline.ledger_entry
FOR EACH ROW EXECUTE FUNCTION ledgerline.prevent_financial_record_changes();

CREATE TRIGGER financial_transaction_is_immutable
BEFORE UPDATE OR DELETE ON ledgerline.financial_transaction
FOR EACH ROW EXECUTE FUNCTION ledgerline.prevent_financial_record_changes();
