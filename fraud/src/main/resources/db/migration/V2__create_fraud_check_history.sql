CREATE SEQUENCE IF NOT EXISTS fraud_id_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS fraud_check_history
(
    id           BIGSERIAL PRIMARY KEY,
    customer_id  INTEGER                     NOT NULL,
    is_fraudster BOOLEAN                     NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
