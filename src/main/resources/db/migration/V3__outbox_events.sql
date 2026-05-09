CREATE TABLE outbox_events (
    event_id UUID NOT NULL PRIMARY KEY,
    payment_ref UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 10 CHECK (max_attempts > 0),
    last_error_code VARCHAR(64),
    last_error_message VARCHAR(1024),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '7 days'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_events_status_next_attempt_at
    ON outbox_events (status, next_attempt_at);

CREATE INDEX idx_outbox_events_payment_ref
    ON outbox_events (payment_ref);

CREATE INDEX idx_outbox_events_expires_at
    ON outbox_events (expires_at);
