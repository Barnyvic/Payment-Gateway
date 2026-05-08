CREATE TABLE idempotency_records (
    id UUID NOT NULL PRIMARY KEY,
    operation VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    response_snapshot TEXT,
    payment_ref UUID,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '24 hours'),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_idempotency_records_operation_key
    ON idempotency_records (operation, idempotency_key);

CREATE INDEX idx_idempotency_records_status_expires_at
    ON idempotency_records (status, expires_at);

CREATE INDEX idx_idempotency_records_created_at
    ON idempotency_records (created_at);
