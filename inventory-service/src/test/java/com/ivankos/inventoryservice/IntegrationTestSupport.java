package com.ivankos.inventoryservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for tests that boot the full Spring context. Provides a throwaway Postgres via
 * Testcontainers (Liquibase migrates and seeds it on startup) and switches the Kafka listener off
 * — these tests exercise the app against its database, not a broker, so they need a running Docker
 * daemon but not {@code docker compose}. The {@code test} profile overlays SQL logging
 * (see {@code application-test.yaml}).
 */
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
}