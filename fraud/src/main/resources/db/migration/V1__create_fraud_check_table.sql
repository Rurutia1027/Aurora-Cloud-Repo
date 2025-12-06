CREATE TABLE IF NOT EXISTS fraud_checks
(
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT  NOT NULL,
    is_fraud    BOOLEAN NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
