# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

All commands are run from the repo root (Gradle wrapper).

- Build: `./gradlew build` (Windows: `gradlew.bat build`)
- Run the service: `./gradlew :order-service:bootRun`
- Run all tests: `./gradlew :order-service:test`
- Run a single test class: `./gradlew :order-service:test --tests "com.ivankos.orderservice.OrderServiceApplicationTests"`
- Start local infra (Kafka, Postgres, Redis, Elasticsearch, Keycloak, Localstack): `docker compose up -d`

Tests use `@SpringBootTest` and boot the full context, so Postgres (and Kafka, once a consumer/producer test exists) must be reachable — start `docker compose up -d postgres kafka` first. There are no embedded/testcontainers substitutes configured.

## Architecture

This is a multi-module Gradle build (root `order-processing-system`) intended to grow into multiple services; only `order-service` exists so far. Root `build.gradle.kts` just applies shared plugin versions/repositories to all subprojects — module-specific dependencies live in `order-service/build.gradle.kts`.

`order-service` is a Spring Boot 4.1.0 / Java 21 app with a standard layered flow:

`OrderController` → `OrderService` → `OrderRepository` (Spring Data JPA), with `OrderMapper` (MapStruct, `componentModel = "spring"`) handling all entity/DTO/event conversions.

Order creation (`OrderService.createOrder`) does three things in sequence:
1. Persists an `Order` entity (`model/Order.java`) with `status = PENDING`. `totalAmount` is currently hardcoded to `BigDecimal.ZERO` — pricing is meant to come from a future Inventory service (see TODO in `OrderService`).
2. Builds an `OrderCreatedEvent` via `OrderMapper.toCreatedEvent` and publishes it to Kafka on the `app.kafka.topic.order-events` topic (`application.yaml`), keyed by order ID, using a dedicated `KafkaTemplate<String, OrderEvent>` (`config/KafkaConfig.java`) that serializes with `JacksonJsonSerializer`.
3. Returns an `OrderResponse` DTO.

Events are modeled as a sealed interface: `OrderEvent` permits `OrderCreatedEvent` (`event/` package) — extend this `permits` clause when adding new event types.

Schema is managed by Liquibase, not Hibernate: `spring.jpa.hibernate.ddl-auto` is `validate`, so any entity change requires a new changeset under `order-service/src/main/resources/db/changelog/changes/`, registered in `db.changelog-master.yaml`.

Errors are translated to RFC 7807 responses via `GlobalExceptionHandler` (`@RestControllerAdvice` + `ProblemDetail`); `spring.mvc.problemdetails.enabled` is on. Add new domain exceptions to `exception/` and a handler method there.

`docker-compose.yml` provisions the full target environment (Kafka, Postgres — also seeded with a `keycloak_db` via `init-scripts/init-keycloak-db.sql`, Redis, Elasticsearch, Keycloak, Localstack) even though `order-service` currently only uses Postgres and Kafka; the other services are infrastructure for planned future services/features. Keycloak binds to host port 8181 (not 8080) specifically to avoid clashing with `order-service`.

Manual/exploratory API testing uses the JetBrains HTTP Client: `order-service/src/test/resources/http/orders.http` chains a create → get request using a post-response script that captures the created order's ID.

## Design decisions

These are deliberate choices, not defaults - follow them when writing new code:

- **No service interfaces without a second implementation.** `OrderServiceImpl implements OrderService`-style indirection is a cargo-cult pattern from JDK dynamic proxy days. Spring proxies concrete classes via CGLIB, Mockito mocks classes directly. Add an interface only when a second implementation or a real module boundary appears.
- **Entity owns its construction invariants, not the service.** `Order.create(customerId, totalAmount)` is a static factory method on the entity itself - "a new order is always PENDING" belongs to the class, not to orchestrating code. `@Builder` is scoped `AccessLevel.PRIVATE` so `create()` stays the only public construction path. Avoid anemic domain model: state-transition methods (`markPaid()`, `cancel()`, etc.) belong on `Order`, not scattered across services.
- **Events carry full state, not just an ID.** `OrderCreatedEvent` is event-carried state transfer - consumers don't call back over REST for details. In a choreography saga, a synchronous callback would defeat the point of choreography.
- **Envelope fields (`eventId`, `occurredAt`) are added to every event from day one**, even before a consumer exists - retrofitting them later means old events in the topic lack the field (schema evolution problem).
- **One Kafka topic per aggregate (`order-events`), not per event type.** Kafka only guarantees order within a partition; keying by `orderId` into a single topic keeps an order's full event history ordered. A topic-per-type split would lose that guarantee across topics.
- **ProblemDetail (RFC 9457), not a custom error DTO.** Spring's built-in support already covers framework exceptions (validation, deserialization, type mismatch) for free; `GlobalExceptionHandler` is only for domain exceptions Spring doesn't know about.
- **DTOs are Java records, entities are Lombok classes.** Records give compiler-enforced immutability and work with record patterns; JPA entities need mutability (dirty checking) and true inheritance (lazy-loading proxies), which records structurally can't provide.