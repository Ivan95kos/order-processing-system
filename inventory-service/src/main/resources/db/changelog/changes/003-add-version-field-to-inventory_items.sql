--liquibase formatted sql

--changeset ivankos:003-add-version-field-to-inventory_items
ALTER TABLE inventory_items
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

--rollback ALTER TABLE inventory_items DROP COLUMN version;
