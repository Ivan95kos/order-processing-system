--liquibase formatted sql

--changeset ivankos:001-create-payments-table
CREATE TABLE payments
(
    id         uuid                        NOT NULL,
    order_id   uuid                        NOT NULL,
    amount     numeric(19, 2)              NOT NULL,
    status     varchar(32)                 NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT payments_pkey PRIMARY KEY (id),
    CONSTRAINT payments_order_id_key UNIQUE (order_id),
    CONSTRAINT payments_status_check CHECK (status IN ('COMPLETED', 'FAILED'))
);

--rollback DROP TABLE payments;
