DROP TABLE IF EXISTS microservices.orders CASCADE;
DROP TABLE IF EXISTS microservices.bank_accounts_outbox CASCADE;


CREATE TABLE IF NOT EXISTS microservices.orders
(
    id         UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),
    email      VARCHAR(60) UNIQUE       NOT NULL CHECK ( email <> '' ),
    address    VARCHAR(500)             NOT NULL CHECK ( address <> '' ),
    status     VARCHAR(20)              NOT NULL CHECK ( status <> '' ),
    total_sum  DECIMAL(16, 2)           NOT NULL DEFAULT 0.00,
    version    INTEGER                  NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS orders_email_idx ON microservices.orders (email);

CREATE TABLE IF NOT EXISTS microservices.order_items
(
    id         UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),
    order_id   UUID REFERENCES microservices.orders (id),
    title      VARCHAR(250) UNIQUE      NOT NULL CHECK ( title <> '' ),
    quantity   BIGINT                   NOT NULL DEFAULT 0,
    price      DECIMAL(16, 2)           NOT NULL DEFAULT 0.00,
    version    INTEGER                  NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS order_items_order_id_idx ON microservices.order_items (order_id);