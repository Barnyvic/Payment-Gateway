CREATE TABLE payment_receipts (
    payment_ref UUID NOT NULL PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    state VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    authorized_at TIMESTAMPTZ,
    captured_at TIMESTAMPTZ,
    voided_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    bank_authorization_id VARCHAR(255),
    bank_capture_id VARCHAR(255),
    bank_void_id VARCHAR(255),
    bank_refund_id VARCHAR(255),
    last_error_code VARCHAR(64),
    last_error_message VARCHAR(1024),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payment_receipts_order_id ON payment_receipts (order_id);
CREATE INDEX idx_payment_receipts_customer_id ON payment_receipts (customer_id);
