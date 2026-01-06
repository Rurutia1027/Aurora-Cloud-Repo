-- V1__create_customer_table.sql
CREATE TABLE customer (
                          id BIGINT PRIMARY KEY DEFAULT nextval('customer_id_sequence'),
                          first_name VARCHAR(100) NOT NULL,
                          last_name VARCHAR(100) NOT NULL,
                          email VARCHAR(255) NOT NULL UNIQUE,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
