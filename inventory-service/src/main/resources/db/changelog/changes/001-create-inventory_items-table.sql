--liquibase formatted sql

--changeset ivankos:001-create-inventory_items-table
CREATE TABLE inventory_items
(
    product_id uuid                        NOT NULL,
    available  INT                         NOT NULL,
    reserved   INT                         NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT inventory_items_pkey PRIMARY KEY (product_id)
);

--rollback DROP TABLE inventory_items;
