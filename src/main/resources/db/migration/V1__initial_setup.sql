CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS microservices;

DROP TABLE IF EXISTS microservices.bank_accounts CASCADE;
DROP TABLE IF EXISTS microservices.bank_accounts_outbox CASCADE;


CREATE TABLE IF NOT EXISTS microservices.bank_accounts
(
    id         UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),
    first_name VARCHAR(60)              NOT NULL CHECK ( first_name <> '' ),
    last_name  VARCHAR(60)              NOT NULL CHECK ( last_name <> '' ),
    email      VARCHAR(60) UNIQUE       NOT NULL CHECK ( email <> '' ),
    address    VARCHAR(500)             NOT NULL CHECK ( address <> '' ),
    phone      VARCHAR(20) UNIQUE       NOT NULL CHECK ( phone <> '' ),
    balance    DECIMAL(16, 2)           NOT NULL DEFAULT 0.00,
    currency   VARCHAR(3)               NOT NULL DEFAULT 'USD',
    version    SERIAL                   NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS bank_account_email_idx ON microservices.bank_accounts (email);


CREATE TABLE IF NOT EXISTS microservices.bank_accounts_outbox
(
    event_id     UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),
    event_type   VARCHAR(250)             NOT NULL CHECK ( event_type <> '' ),
    aggregate_id UUID                     NOT NULL,
    version      SERIAL                   NOT NULL,
    data         BYTEA,
    timestamp    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS bank_account_outbox_aggregate_id_idx ON microservices.bank_accounts_outbox (aggregate_id);
