--liquibase formatted sql

--changeset ivankos:002-seed-inventory-items
INSERT INTO inventory_items (product_id, available, reserved, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 100, 0, now(), now()),
    ('22222222-2222-2222-2222-222222222222', 50, 0, now(), now()),
    ('33333333-3333-3333-3333-333333333333', 10, 0, now(), now());

--rollback DELETE FROM inventory_items WHERE product_id IN ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333');