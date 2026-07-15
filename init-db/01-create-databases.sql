-- Bootstrap databases for the whole system (database-per-service).
-- Runs ONCE, only when the Postgres data directory is empty.
-- Schema creation is Liquibase's job - never put DDL here.

CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE inventory_db;
CREATE DATABASE keycloak_db;
