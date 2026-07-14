--liquibase formatted sql

--changeset ivankos:001-create-orders-table
CREATE TABLE orders
(
    id           uuid NOT NULL,
    customer_id  uuid NOT NULL,
    status       varchar(32) NOT NULL,
    total_amount numeric(19, 2) NOT NULL,
    created_at   timestamp(6) with time zone NOT NULL,
    updated_at   timestamp(6) with time zone NOT NULL,
    CONSTRAINT orders_pkey PRIMARY KEY (id),
    CONSTRAINT orders_status_check CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'REFUNDED'))
);

--rollback DROP TABLE orders;
