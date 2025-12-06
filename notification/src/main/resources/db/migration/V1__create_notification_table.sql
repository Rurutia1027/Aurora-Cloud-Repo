CREATE TABLE IF NOT EXISTS notifications
(
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    message     TEXT   NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);