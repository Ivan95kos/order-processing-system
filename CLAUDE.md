# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

All commands are run from the repo root (Gradle wrapper).

- Build everything: `./gradlew build` (Windows: `gradlew.bat build`)
- Run a service: `./gradlew :order-service:bootRun` (same for `:payment-service`, `:inventory-service`)
- Run all tests: `./gradlew test`
- Run one module's tests: `./gradlew :inventory-service:test`
- Run a single test class: `./gradlew :inventory-service:test --tests "com.ivankos.inventoryservice.domain.InventoryItemTest"`
- Start local infra (Kafka, Postgres, Redis, Elasticsearch, Keycloak, Localstack): `docker compose up -d`

Ports: `order-service` 8080, `payment-service` 8081, `inventory-service` 8082, Keycloak 8181 (deliberately not 8080).

Integration tests that boot a Spring context bring their own infrastructure via Testcontainers (currently `inventory-service` only): they extend `IntegrationTestSupport`, which starts a throwaway Postgres and switches the Kafka listener off, so they need a running Docker daemon but not `docker compose`. Pure unit tests (domain objects, services with mocked ports) run without any infrastructure and should stay that way.

Kafka CLI runs through `docker exec` (no `.sh` scripts on the Windows host), single-line commands only:

```
docker exec -it order-processing-system-kafka-1 /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic inventory-events --from-beginning --property print.key=true
```

## Architecture

Multi-module Gradle build (root `order-processing-system`) with three services so far: `order-service`, `payment-service`, `inventory-service`. Root `build.gradle.kts` applies shared plugin versions and repositories; module-specific dependencies live in each module's `build.gradle.kts`. Spring Boot 4.1 / Java 21 throughout.

### Two architectural styles, on purpose

`order-service` and `payment-service` are **layered** (controller → service → repository). `inventory-service` is **hexagonal** (ports and adapters). This is a deliberate contrast, not inconsistency — do not "harmonize" one into the other. When adding code, follow the style of the module you are in.

### Saga (choreography)

Services communicate only through Kafka events; there are no synchronous calls between them. Each service owns its own database (`order_db`, `payment_db`, `inventory_db`), seeded by `init-scripts/`.

Current wiring:

- `order-service` creates an order (`PENDING`) and publishes `OrderCreated` to `order-events`.
- `payment-service` consumes `order-events`, decides the payment outcome, publishes `PaymentCompleted` / `PaymentFailed` to `payment-events`.
- `order-service` consumes `payment-events` and drives the order's state machine, including the compensating transition on failure.
- `inventory-service` consumes `order-events`, reserves stock, publishes `StockReserved` / `StockReservationFailed` to `inventory-events`.

Not wired yet (next steps): `order-service` does not consume `inventory-events`, and the chain is currently parallel rather than sequential. The intended design is **stock first, payment second** (releasing a reservation is cheaper to compensate than refunding money), which means `payment-service` should eventually listen to `inventory-events` instead of `order-events`, and `OrderStatus` needs an intermediate `STOCK_RESERVED` state.

### inventory-service (hexagonal)

```
com.ivankos.inventoryservice/
├── domain/                  ← centre: no Spring, no JPA, no Kafka imports
│   ├── InventoryItem        ← available/reserved counters + reserve()/release()
│   └── exception/
├── application/
│   ├── port/in/             ← ReserveStockUseCase + dto/ (what the world may ask)
│   ├── port/out/            ← InventoryRepository, InventoryEventPublisher, event/
│   └── ReserveStockService  ← implements the inbound port, orchestrates outbound ones
└── adapter/                 ← edge: Spring/JPA/Kafka live here and nowhere else
    ├── in/kafka/            ← config, event, listener, mapper
    └── out/
        ├── persistence/     ← InventoryItemJpaEntity, Spring Data repo, mapping bridge
        └── kafka/           ← config, KafkaInventoryEventPublisher
```

Rules for this module:

- **Nothing under `domain/` or `application/` may import `jakarta.persistence`, `org.apache.kafka` or Spring Data types.** If a change wants to, the mapping belongs in an adapter instead.
- **Inbound ports are implemented by the core, outbound ports by adapters.** `ReserveStockUseCase` ← `ReserveStockService` (inside); `InventoryRepository` ← `JpaInventoryRepository` (outside). Adapters never define ports of their own — a Kafka listener calls a port, it doesn't implement one.
- **Ports are named after their domain role**, never after the mechanism or the technology behind them: `InventoryRepository`, not `JpaVerifyPort`. The word "Kafka" must not appear at port level.
- **Three languages, mapped at every boundary**: wire event (`OrderCreatedEvent`) → port input (`ReserveOrderRequest`) → domain (`InventoryItem`), plus domain ↔ JPA entity on the way out. The middle type is what keeps the use case independent of transport; do not shortcut it by passing the Kafka event straight into the port.
- **Package growth**: ports stay grouped by direction (`port/in`, `port/out`) because they are the hexagon's boundary; implementations move to feature packages (`application/reservestock/`) once there is more than one use case.

### Persistence

Schema is Liquibase-managed everywhere; `spring.jpa.hibernate.ddl-auto` is `validate`, so any entity change needs a new changeset under `<module>/src/main/resources/db/changelog/changes/`, registered explicitly in `db.changelog-master.yaml` (explicit `include`, not `includeAll`).

`inventory-service` seeds three products with fixed UUIDs (`1111…`, `2222…`, `3333…`) via changeset `002`. Any manual test order must use one of those `productId`s or the reservation will always take the "product not found" branch.

### Manual API testing

JetBrains HTTP Client files under `<module>/src/test/resources/http/`; `orders.http` chains create → get using a post-response script that captures the created order's ID.

## Design decisions

These are deliberate choices, not defaults - follow them when writing new code.

### General

- **No service interfaces without a second implementation.** `OrderServiceImpl implements OrderService`-style indirection is a cargo-cult pattern from JDK dynamic proxy days. Spring proxies concrete classes via CGLIB, Mockito mocks classes directly. Add an interface only when a second implementation or a real module boundary appears. **Hexagonal ports are the exception**: they exist to invert a dependency across the hexagon's boundary, which is a real reason, not ceremony.
- **Entity owns its construction invariants, not the service.** `Order.create(customerId, totalAmount)` is a static factory on the entity - "a new order is always PENDING" belongs to the class, not to orchestrating code. `@Builder` is scoped `AccessLevel.PRIVATE` so `create()` stays the only public construction path.
- **Rich domain model over anemic.** State transitions live on the object that owns the state: `Order.markPaid()`, `InventoryItem.reserve()/release()`. **Never put `@Setter` on a class whose invariants matter** — a public setter is a back door around the guard and makes the domain method decorative.
- **DTOs are Java records, entities are Lombok classes.** Records give compiler-enforced immutability and work with record patterns; JPA entities need mutability (dirty checking) and true inheritance (lazy-loading proxies), which records structurally can't provide.
- **ProblemDetail (RFC 9457), not a custom error DTO.** Spring's built-in support already covers framework exceptions for free; `GlobalExceptionHandler` is only for domain exceptions Spring doesn't know about.

### Events and Kafka

- **Events carry full state, not just an ID.** Event-carried state transfer — consumers don't call back over REST for details. In a choreography saga a synchronous callback would defeat the point.
- **Envelope fields (`eventId`, `occurredAt`) on every event from day one**, even before a consumer exists — retrofitting them means old events in the topic lack the field.
- **One topic per aggregate** (`order-events`, `payment-events`, `inventory-events`), not per event type, and **each service publishes only to its own topic**. Kafka guarantees order within a partition only; keying by `orderId` into one topic keeps an aggregate's history ordered. The key exists for **ordering through repetition**, not uniqueness — every event of one order shares the same key on purpose.
- **Events are sealed interfaces with record implementations**, and the discriminator (`paymentStatus`, `inventoryStatus`) is a **canonical record component**, never a method in the body — Jackson serializes records by component, so a method silently disappears from the wire. Each record gets a **compact constructor that hardcodes its own discriminator**, so a `StockReservedEvent` carrying `FAILED` is structurally impossible to build.
- **Consumers own their event contracts (tolerant reader).** Each consuming service declares its own record with only the fields it reads, `FAIL_ON_UNKNOWN_PROPERTIES=false` and `setUseTypeHeaders(false)`; discriminators are read as `String` with a `default` branch, never as an enum, so an unknown value doesn't kill the listener. Field names must match the wire exactly — a renamed field deserializes to `null`, not an error.
- **Typed `KafkaTemplate` beans are declared explicitly.** The autoconfigured `KafkaTemplate<Object, Object>` does not satisfy injection of a typed template (Spring matches beans by generic type), so each producer module declares its own `@Bean` built from `KafkaProperties.buildProducerProperties()`. Use `JacksonJsonSerializer`/`JacksonJsonDeserializer` (Jackson 3), not the deprecated `JsonSerializer`.

### Transactions, failures and the saga

- **A business failure becomes an event; a technical failure propagates.** `InsufficientStockException`, "product not found" and a declined payment are valid saga outcomes: catch them, publish a failure event, let the offset commit. Retrying will not create stock. Infrastructure exceptions (`DataIntegrityViolationException`, connection loss, timeouts) must **not** be caught — retry may genuinely help, and if it doesn't the record belongs in a DLQ. Swallowing them disguises a bug as a business decision: the order gets cancelled "because stock ran out" while the real cause was broken code.
- **Never rethrow a business failure out of a Kafka listener** — an unhandled exception means the offset isn't committed and Kafka redelivers a perfectly valid event forever.
- **Consumer-side idempotency is state-based.** The entity's state machine has three branches: transition, no-op if already in the target state, throw on an illegal transition. This is separate from producer-side idempotency (`enable.idempotence`, PID + sequence), which only deduplicates broker retries.
- **All-or-nothing across order lines** is enforced by a single transaction around the whole loop; the loop lives in the application service, never in the aggregate — one stock item has no business knowing about other lines of someone else's order.
- **`TransactionTemplate`, not `@Transactional`, when the transactional block and its `catch` live in the same class.** `this.someMethod()` bypasses the Spring proxy and the annotation is silently ignored — no transaction, no rollback, partial state committed. Equally important: a `catch` **inside** a transactional method kills the rollback, so the transactional part must throw and the handler must sit outside it.
- **Dirty checking does not work in the hexagonal module.** `InventoryRepository` returns domain objects built by a mapper after the entity was read, so they were never managed — Hibernate tracks `InventoryItemJpaEntity`, which the service never sees. Persistence is explicit through the bridge. Inside the adapter, the bridge must **read the managed entity and mutate only the business fields**: building a fresh entity and letting `merge` copy it wipes every field the domain doesn't carry (audit timestamps included) to `null`.
- **The dual-write gap is known, deliberate tech debt.** `save` + `kafkaTemplate.send` are not atomic; wrapping them in `@Transactional` would be false atomicity. The fix is the Transactional Outbox pattern, scheduled as its own step — do not paper over it in the meantime.

## Testing

- **JUnit 5 + Mockito, and Mockito is used through `BDDMockito`** — `given(...).willReturn(...)`, `then(mock).should()`, `willAnswer(...)`. Do not mix in `when(...).thenReturn(...)` / `verify(...)`; the BDD API is the house style and reads consistently with the Given/When/Then comment blocks in the test bodies.
- **AssertJ for assertions** (`assertThat`, `assertThatThrownBy`), not JUnit's built-ins.
- **Test names describe behaviour**: `methodName_expectedOutcome_whenCondition`, e.g. `reserve_publishesStockReservedEvent_whenAllItemsAreAvailable`.
- **Domain objects are tested with plain JUnit, no Spring context.** In the hexagonal module this is the point of the architecture — business rules are reachable without Kafka or a database. A test that boots a context to check an invariant is a smell.
- **Application services are tested with mocked ports**, never with a real repository or publisher.
- **Assert that state is unchanged after an expected exception**, not just that the exception was thrown — that is what proves the guard runs before the mutation rather than halfway through it.
- **Prefer `ArgumentCaptor` over matcher-heavy verification** when asserting on a published event; capture it, then assert on its type and fields.
- **Know what a test does not cover and say so in a comment.** A mocked `TransactionTemplate` that just runs the callback verifies flow, not rollback; real atomicity needs an integration test.
- Integration tests use Testcontainers — extend `IntegrationTestSupport` (Postgres via `@ServiceConnection`, Kafka listener off, `test` profile for SQL logging) rather than adding a bare `@SpringBootTest` that silently depends on a running `docker compose`. `ReserveStockConcurrencyIntegrationTest` shows when a real DB earns its keep: reach for one only where the point is behaviour Testcontainers can show and mocks can't (here, the optimistic-lock retry under concurrency).
