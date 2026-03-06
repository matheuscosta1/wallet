# Wallet Service

> A backend microservice responsible for managing digital wallets and processing financial transactions asynchronously with high reliability and consistency.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org/)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.x-black)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-red)](https://redis.io/)

---

## Table of Contents

- [Overview](#overview)
- [Technologies](#technologies)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Architecture](#architecture)
- [Data Models](#data-models)
- [Transaction Flow](#transaction-flow)
- [Design Patterns](#design-patterns)
- [Concurrency & Consistency](#concurrency--consistency)
- [Kafka Resilience](#kafka-resilience)
- [Tests](#tests)
- [Future Improvements](#future-improvements)

---

## Overview

**Wallet Service** is a Java/Spring Boot backend that provides six operations on digital wallets:

| Operation | Method | Endpoint |
|---|---|---|
| Create wallet | `POST` | `/wallet/creation` |
| Get balance | `GET` | `/wallet/balance` |
| Deposit funds | `POST` | `/wallet/deposit` |
| Withdraw funds | `POST` | `/wallet/withdraw` |
| Transfer between wallets | `POST` | `/wallet/transfer` |
| Transaction history | `GET` | `/wallet/history` |
| Generic operation | `POST` | `/wallet/any-operation` |

Financial transactions (deposit, withdrawal and transfer) are **processed asynchronously via Apache Kafka**, ensuring the API responds immediately to the client while the actual processing happens in the background with consistency and idempotency guarantees.

---

## Technologies

- **Java 21** + **Spring Boot** — main runtime and framework
- **PostgreSQL** — primary persistence for wallets and transactions
- **Apache Kafka** — asynchronous transaction processing (FIFO, retry, DLQ)
- **Redis** — idempotency control to prevent duplicate transaction processing
- **Docker / Docker Compose** — local infrastructure orchestration
- **Testcontainers** — functional tests with real infrastructure
- **Swagger / OpenAPI** — interactive API documentation
- **Maven** — build and dependency management

---

## Getting Started

### Prerequisites

- Docker and Docker Compose installed
- Java 21+
- Maven 3.8+

### 1. Build the project

```bash
mvn clean install
```

### 2. Start infrastructure (PostgreSQL + Kafka + Redis)

```bash
cd src/main/resources/compose
docker-compose up -d
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The application starts at `http://localhost:8080/wallet`.

### 4. Interactive API documentation (Swagger)

Visit: [http://localhost:8080/wallet/swagger-ui/index.html](http://localhost:8080/wallet/swagger-ui/index.html)

A Postman collection is also available at `collections/Wallet.postman_collection.json`.

---

## API Endpoints

### `POST /wallet/creation`

Creates a new wallet for a user.

```json
{
  "userId": "user-account-id"
}
```

### `GET /wallet/balance`

Returns the current balance of a wallet.

```json
{
  "userId": "user-account-id"
}
```

### `POST /wallet/deposit`

Deposits funds into a wallet. Processed asynchronously via Kafka.

```json
{
  "userId": "user-account-id",
  "amount": 100.00,
  "idempotencyId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:** returns a `transactionId` immediately (HTTP 200). The balance is updated asynchronously.

### `POST /wallet/withdraw`

Withdraws funds from a wallet. Processed asynchronously via Kafka.

```json
{
  "userId": "user-account-id",
  "amount": 50.00,
  "idempotencyId": "550e8400-e29b-41d4-a716-446655440001"
}
```

### `POST /wallet/transfer`

Transfers funds between two wallets. Processed asynchronously via Kafka.

```json
{
  "fromUserId": "user-account-id-0",
  "toUserId": "user-account-id-1",
  "amount": 25.00,
  "idempotencyId": "550e8400-e29b-41d4-a716-446655440002"
}
```

### `GET /wallet/history`

Returns the transaction history of a user filtered by a specific date.

```json
{
  "userId": "user-account-id",
  "date": "2025-02-21 00:00:00.000"
}
```

Each transaction record includes `balanceBeforeTransaction` and `balanceAfterTransaction` for full traceability.

### `POST /wallet/any-operation`

Generic endpoint that accepts any transaction type (`DEPOSIT`, `WITHDRAW`, `TRANSFER`) in a single payload.

---

## Architecture

The project uses **Hexagonal Architecture (Ports & Adapters)** combined with **Event-Driven Architecture via Apache Kafka**.

### Core Principle

The **domain depends on nothing external**. It defines interfaces (Ports) that describe what it needs. The infrastructure (JPA, Kafka, Redis) implements those interfaces (Adapters), ensuring business rules are testable and replaceable independently.

### Package Structure

```
br.com.wallet.project
├── domain/                         ← Pure core — zero framework dependencies
│   ├── model/
│   │   ├── Wallet.java             ← Aggregate root (deposit / withdraw)
│   │   ├── Transaction.java
│   │   └── TransactionMessage.java ← Domain Event published to Kafka
│   └── exception/
│       ├── WalletErrors.java
│       └── WalletDomainException.java
│
├── application/                    ← Use cases and contracts (Ports)
│   ├── port/
│   │   ├── in/                     ← Driving Ports
│   │   │   ├── WalletUseCase.java
│   │   │   └── TransactionUseCase.java
│   │   └── out/                    ← Driven Ports
│   │       ├── WalletRepository.java
│   │       ├── TransactionRepository.java
│   │       ├── TransferRepository.java
│   │       ├── TransactionEventPublisher.java
│   │       └── IdempotencyRepository.java
│   ├── command/
│   │   └── TransactionCommand.java ← Use case intent object
│   ├── service/
│   │   ├── WalletService.java      ← Implements WalletUseCase
│   │   └── TransactionService.java ← Implements TransactionUseCase
│   └── strategy/
│       ├── TransactionStrategy.java
│       ├── TransactionStrategyFactory.java
│       ├── DepositStrategy.java
│       ├── WithdrawStrategy.java
│       └── TransferStrategy.java
│
├── adapter/
│   ├── in/web/                     ← Driving Adapter — REST
│   │   ├── controller/
│   │   │   ├── WalletController.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── request/                ← Inbound DTOs
│   │   └── response/               ← Outbound DTOs
│   └── out/
│       ├── persistence/            ← Driven Adapter — JPA + PostgreSQL
│       │   ├── WalletPersistenceAdapter.java
│       │   ├── TransactionPersistenceAdapter.java
│       │   ├── entity/             ← JPA entities (outside domain)
│       │   └── jpa/                ← Spring Data Repositories
│       ├── messaging/kafka/        ← Driven Adapter — Apache Kafka
│       │   ├── producer/KafkaTransactionEventPublisher.java
│       │   └── consumer/KafkaTransactionConsumer.java
│       └── cache/redis/            ← Driven Adapter — Redis
│           └── RedisIdempotencyAdapter.java
│
└── shared/
    ├── mapper/                     ← Cross-layer converters
    └── util/MoneyUtil.java
```

### Layer Responsibilities

| Layer | Responsibility | Dependencies |
|---|---|---|
| **domain** | Business rules, aggregates, domain events | None |
| **application** | Use cases, ports (contracts), strategies | domain |
| **adapter/in** | Receive HTTP calls, validate requests | application |
| **adapter/out** | Persistence, messaging, cache | application |
| **shared** | Mappers, shared utilities | domain, application |

---

## Data Models

### Wallet

Aggregate root representing a user's wallet.

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Auto-generated PK |
| `userId` | `String` | Unique user identifier |
| `balance` | `BigDecimal` | Current balance |
| `version` | `Long` | Optimistic Locking control |

### Transaction

Records each financial operation with a balance snapshot.

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Auto-generated PK |
| `walletId` | `FK` | Related wallet |
| `transactionTrackId` | `UUID` | Tracking ID |
| `type` | `Enum` | `DEPOSIT` / `WITHDRAW` / `TRANSFER` |
| `amount` | `BigDecimal` | Operation amount |
| `balanceBeforeTransaction` | `BigDecimal` | Balance before the operation |
| `balanceAfterTransaction` | `BigDecimal` | Balance after the operation |
| `timestamp` | `LocalDateTime` | Date and time of operation |

### Transfer

Records the relationship between both sides of a transfer (debit + credit), enabling full traceability.

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Auto-generated PK |
| `fromWalletId` | `FK` | Source wallet |
| `toWalletId` | `FK` | Destination wallet |
| `debitTransactionId` | `FK` | Withdraw transaction (source) |
| `creditTransactionId` | `FK` | Deposit transaction (destination) |
| `amount` | `BigDecimal` | Transferred amount |
| `timestamp` | `LocalDateTime` | Date and time of operation |

---

## Transaction Flow

```
1.  Client → POST /deposit
2.  WalletController validates the request
3.  TransactionService builds a TransactionCommand
4.  KafkaTransactionEventPublisher publishes a message to the wallet-transactions topic
5.  API returns TransactionResponse immediately (HTTP 200)

          — From this point, processing is asynchronous —

6.  KafkaTransactionConsumer receives the message
7.  RedisIdempotencyAdapter checks if the idempotencyId was already processed
    ├── If duplicate → silently discards the message
    └── If new → registers as IN_PROGRESS
8.  TransactionStrategyFactory resolves the correct strategy
    ├── DEPOSIT  → DepositStrategy
    ├── WITHDRAW → WithdrawStrategy
    └── TRANSFER → TransferStrategy (2 atomic operations)
9.  Strategy applies business rules on the Wallet aggregate
10. WalletPersistenceAdapter updates the balance in PostgreSQL (with lock)
11. TransactionPersistenceAdapter records the transaction with balance snapshot
12. Redis entry marked as COMPLETED
```

---

## Design Patterns

| Pattern | Where it is applied |
|---|---|
| **Hexagonal / Ports & Adapters** | Full separation between domain and infrastructure |
| **Strategy** | `DepositStrategy`, `WithdrawStrategy`, `TransferStrategy` |
| **Factory** | `TransactionStrategyFactory` resolves strategy by `TransactionType` |
| **Command** | `TransactionCommand` as the use case intent object |
| **Domain Event** | `TransactionMessage` published to Kafka after intent is registered |
| **Idempotency** | `IdempotencyRepository` → Redis `setIfAbsent` prevents duplicate processing |
| **Optimistic Locking** | `@Version` on `WalletEntity` detects concurrent updates |
| **Pessimistic Locking** | `@Lock(PESSIMISTIC_WRITE)` on `JpaWalletRepository` blocks concurrent reads |

---

## Concurrency & Consistency

The system combines three strategies to ensure consistency under high concurrency:

**Pessimistic Locking (`SELECT FOR UPDATE`):** when fetching a wallet to process a transaction, an exclusive lock is applied at the database level. This ensures no other transaction can read or modify the same record until the current transaction commits.

**Optimistic Locking via `@Version`:** the `version` field on `WalletEntity` is incremented with every update. If two transactions attempt to update the same version simultaneously, the second one receives an `OptimisticLockException` and is rejected.

**Idempotency via Redis:** the `idempotencyId` (UUID provided by the client) is registered in Redis using `setIfAbsent` before any processing begins. If a second message with the same ID arrives (Kafka retry, network failure, etc.), it is discarded before any database operation takes place.

---

## Kafka Resilience

- **FIFO processing:** Kafka guarantees ordering within a partition. Transactions from the same wallet are routed to the same partition, ensuring sequential processing.
- **Transactional producer:** `@Transactional("kafkaTransactionManager")` guarantees exactly-once delivery — only one message per operation is published, even on application retries.
- **Automatic retry with backoff:** `@RetryableTopic` with exponential backoff reprocesses messages on transient failures (e.g., database timeout).
- **Dead Letter Queue (DLQ):** after exhausting all retry attempts, the message is sent to a DLQ topic via `@DltHandler` for manual inspection and reprocessing.
- **Manual acknowledgment:** the consumer only commits the offset after successful processing (`AcknowledgmentMode.MANUAL`), guaranteeing at-least-once delivery and zero message loss.

---

## Tests

### Unit Tests

Located in `src/test/.../unit/`. Uses **Mockito** to mock repositories and services, testing business logic in complete isolation from infrastructure.

### Functional Tests

Located in `src/test/.../functional/`. Uses **Testcontainers** with the project's `docker-compose.yml` to spin up real instances of PostgreSQL, Kafka and Redis, validating the complete end-to-end flow.

To run unit tests only:

```bash
mvn test -Dtest="**/unit/**"
```

To run all tests including functional (requires Docker):

```bash
mvn verify
```

---

## Future Improvements

- **Spring Security** — authentication and authorization via JWT / OAuth2
- **Rate limiting** — protection against endpoint abuse (e.g., Bucket4j)
- **Observability** — integration with OpenTelemetry / Micrometer / Grafana
- **Notification service** — notify users after each transaction (email/push)
- **Pagination** on transaction history results
- **Multi-currency support** — multiple currencies with conversion rates

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
