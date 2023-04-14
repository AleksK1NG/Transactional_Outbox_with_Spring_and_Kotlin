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
