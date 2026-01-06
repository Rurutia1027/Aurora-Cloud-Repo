CREATE TABLE IF NOT EXISTS notification
(
    notification_id      INTEGER PRIMARY KEY DEFAULT nextval('notification_id_sequence'),
    to_customer_id       INTEGER NOT NULL,
    to_customer_email    VARCHAR(255) NOT NULL,
    sender               VARCHAR(255) NOT NULL,
    message              TEXT NOT NULL,
    sent_at              TIMESTAMP NOT NULL
);