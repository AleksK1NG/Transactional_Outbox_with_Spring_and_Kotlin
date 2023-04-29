CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS microservices;


DROP TABLE IF EXISTS microservices.orders CASCADE;
DROP TABLE IF EXISTS microservices.product_items CASCADE;


CREATE TABLE IF NOT EXISTS microservices.orders
(
    id         UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),
    email      VARCHAR(60) UNIQUE       NOT NULL CHECK ( email <> '' ),
    address    VARCHAR(500)             NOT NULL CHECK ( address <> '' ),
    payment_id VARCHAR(255),
    status     VARCHAR(20)              NOT NULL CHECK ( status <> '' ),
    version    BIGINT                   NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS orders_email_idx ON microservices.orders (email);


CREATE TABLE IF NOT EXISTS microservices.product_items
(
    id         UUID                                               DEFAULT uuid_generate_v4(),
    order_id   UUID REFERENCES microservices.orders (id) NOT NULL,
    title      VARCHAR(250)                              NOT NULL CHECK ( title <> '' ),
    quantity   BIGINT                                    NOT NULL DEFAULT 0,
    price      DECIMAL(16, 2)                            NOT NULL DEFAULT 0.00,
    version    BIGINT                                    NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE                  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE                  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS product_items_order_id_idx ON microservices.product_items (order_id);


CREATE TABLE IF NOT EXISTS microservices.outbox_table
(
    event_id     UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),
    event_type   VARCHAR(250)             NOT NULL CHECK ( event_type <> '' ),
    aggregate_id VARCHAR(250)             NOT NULL CHECK ( aggregate_id <> '' ),
    version      SERIAL                   NOT NULL,
    data         BYTEA,
    timestamp    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS outbox_table_aggregate_id_idx ON microservices.outbox_table (aggregate_id);
