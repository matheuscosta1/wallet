# Wallet Service

> Microserviço responsável por gerenciar carteiras digitais e processar transações financeiras de forma assíncrona, com alta confiabilidade e consistência.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org/)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.x-black)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-red)](https://redis.io/)

---

## Sumário

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Como Rodar](#como-rodar)
- [Endpoints da API](#endpoints-da-api)
- [Arquitetura](#arquitetura)
- [Modelos de Dados](#modelos-de-dados)
- [Fluxo de uma Transação](#fluxo-de-uma-transação)
- [Padrões de Design](#padrões-de-design)
- [Concorrência e Consistência](#concorrência-e-consistência)
- [Resiliência com Kafka](#resiliência-com-kafka)
- [Testes](#testes)
- [Melhorias Futuras](#melhorias-futuras)

---

## Visão Geral

O **Wallet Service** é um backend em Java/Spring Boot que oferece seis operações sobre carteiras digitais:


| Operação                 | Método | Endpoint                |
| -------------------------- | ------- | ----------------------- |
| Criar carteira             | `POST`  | `/wallet/creation`      |
| Consultar saldo            | `GET`   | `/wallet/balance`       |
| Depositar fundos           | `POST`  | `/wallet/deposit`       |
| Sacar fundos               | `POST`  | `/wallet/withdraw`      |
| Transferir entre carteiras | `POST`  | `/wallet/transfer`      |
| Histórico de transações | `GET`   | `/wallet/history`       |
| Operação genérica       | `POST`  | `/wallet/any-operation` |

As transações financeiras (depósito, saque e transferência) são **processadas de forma assíncrona via Apache Kafka**, garantindo que a API responda imediatamente ao cliente enquanto o processamento real ocorre em background com garantias de consistência e idempotência.

---

## Tecnologias

- **Java 21** + **Spring Boot** — runtime e framework principal
- **PostgreSQL** — persistência principal dos dados de carteiras e transações
- **Apache Kafka** — processamento assíncrono de transações (FIFO, retry, DLQ)
- **Redis** — controle de idempotência para evitar reprocessamento de transações
- **Docker / Docker Compose** — orquestração de infraestrutura local
- **Testcontainers** — testes funcionais com infraestrutura real
- **Swagger / OpenAPI** — documentação interativa da API
- **Maven** — build e gerenciamento de dependências

---

## Como Rodar

### Pré-requisitos

- Docker e Docker Compose instalados
- Java 21+
- Maven 3.8+

### 1. Build do projeto

```bash
mvn clean install
```

### 2. Subir infraestrutura (PostgreSQL + Kafka + Redis)

```bash
cd src/main/resources/compose
docker-compose up -d
```

### 3. Iniciar a aplicação

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080/wallet`.

### 4. Documentação interativa (Swagger)

Acesse: [http://localhost:8080/wallet/swagger-ui/index.html](http://localhost:8080/wallet/swagger-ui/index.html)

Uma coleção Postman está disponível em `collections/Wallet.postman_collection.json`.

---

## Endpoints da API

### `POST /wallet/creation`

Cria uma nova carteira para o usuário.

```json
{
  "userId": "user-account-id"
}
```

### `GET /wallet/balance`

Retorna o saldo atual da carteira.

```json
{
  "userId": "user-account-id"
}
```

### `POST /wallet/deposit`

Deposita fundos na carteira. Processamento assíncrono via Kafka.

```json
{
  "userId": "user-account-id",
  "amount": 100.00,
  "idempotencyId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Resposta:** retorna `transactionId` imediatamente (HTTP 200). O saldo é atualizado de forma assíncrona.

### `POST /wallet/withdraw`

Saca fundos da carteira. Processamento assíncrono via Kafka.

```json
{
  "userId": "user-account-id",
  "amount": 50.00,
  "idempotencyId": "550e8400-e29b-41d4-a716-446655440001"
}
```

### `POST /wallet/transfer`

Transfere fundos entre duas carteiras. Processamento assíncrono via Kafka.

```json
{
  "fromUserId": "user-account-id-0",
  "toUserId": "user-account-id-1",
  "amount": 25.00,
  "idempotencyId": "550e8400-e29b-41d4-a716-446655440002"
}
```

### `GET /wallet/history`

Retorna o histórico de transações de um usuário em uma data específica.

```json
{
  "userId": "user-account-id",
  "date": "2025-02-21 00:00:00.000"
}
```

Cada registro de transação inclui `balanceBeforeTransaction` e `balanceAfterTransaction` para rastreabilidade completa.

### `POST /wallet/any-operation`

Endpoint genérico que aceita qualquer tipo de transação (`DEPOSIT`, `WITHDRAW`, `TRANSFER`) em um único payload.

---

## Arquitetura

O projeto utiliza **Arquitetura Hexagonal (Ports & Adapters)** combinada com **Event-Driven Architecture via Apache Kafka**.

### Princípio Central

O **domínio não depende de nada externo**. Ele define interfaces (Ports) que descrevem o que precisa. A infraestrutura (JPA, Kafka, Redis) implementa essas interfaces (Adapters), garantindo que regras de negócio sejam testáveis e substituíveis de forma independente.

### Estrutura de Pacotes

```
br.com.wallet.project
├── domain/                         ← Núcleo puro — zero dependências de framework
│   ├── model/
│   │   ├── Wallet.java             ← Aggregate root (deposit / withdraw)
│   │   ├── Transaction.java
│   │   └── TransactionMessage.java ← Domain Event publicado no Kafka
│   └── exception/
│       ├── WalletErrors.java
│       └── WalletDomainException.java
│
├── application/                    ← Casos de uso e contratos (Ports)
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
│   │   └── TransactionCommand.java ← Objeto de intenção do caso de uso
│   ├── service/
│   │   ├── WalletService.java      ← Implementa WalletUseCase
│   │   └── TransactionService.java ← Implementa TransactionUseCase
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
│   │   ├── request/                ← DTOs de entrada
│   │   └── response/               ← DTOs de saída
│   └── out/
│       ├── persistence/            ← Driven Adapter — JPA + PostgreSQL
│       │   ├── WalletPersistenceAdapter.java
│       │   ├── TransactionPersistenceAdapter.java
│       │   ├── entity/             ← Entidades JPA (fora do domain)
│       │   └── jpa/                ← Spring Data Repositories
│       ├── messaging/kafka/        ← Driven Adapter — Apache Kafka
│       │   ├── producer/KafkaTransactionEventPublisher.java
│       │   └── consumer/KafkaTransactionConsumer.java
│       └── cache/redis/            ← Driven Adapter — Redis
│           └── RedisIdempotencyAdapter.java
│
└── shared/
    ├── mapper/                     ← Conversões entre camadas
    └── util/MoneyUtil.java
```

### Separação de Camadas


| Camada          | Responsabilidade                             | Dependências       |
| --------------- | -------------------------------------------- | ------------------- |
| **domain**      | Regras de negócio, agregados, domain events | Nenhuma             |
| **application** | Casos de uso, ports (contratos), strategies  | domain              |
| **adapter/in**  | Receber chamadas HTTP, validar requests      | application         |
| **adapter/out** | Persistência, mensageria, cache             | application         |
| **shared**      | Mappers, utilitários compartilhados         | domain, application |

---

## Modelos de Dados

### Wallet

Aggregate root que representa a carteira do usuário.


| Campo     | Tipo         | Descrição                      |
| --------- | ------------ | -------------------------------- |
| `id`      | `Long`       | PK auto-gerado                   |
| `userId`  | `String`     | Identificador único do usuário |
| `balance` | `BigDecimal` | Saldo atual                      |
| `version` | `Long`       | Controle de Optimistic Locking   |

### Transaction

Registra cada operação financeira com snapshot de saldo.


| Campo                      | Tipo            | Descrição                         |
| -------------------------- | --------------- | ----------------------------------- |
| `id`                       | `Long`          | PK auto-gerado                      |
| `walletId`                 | `FK`            | Carteira relacionada                |
| `transactionTrackId`       | `UUID`          | ID de rastreamento                  |
| `type`                     | `Enum`          | `DEPOSIT` / `WITHDRAW` / `TRANSFER` |
| `amount`                   | `BigDecimal`    | Valor da operação                 |
| `balanceBeforeTransaction` | `BigDecimal`    | Saldo antes da operação           |
| `balanceAfterTransaction`  | `BigDecimal`    | Saldo depois da operação          |
| `timestamp`                | `LocalDateTime` | Data/hora da operação             |

### Transfer

Registra a relação entre os dois lados de uma transferência (débito + crédito), permitindo rastreabilidade completa.


| Campo                 | Tipo            | Descrição                        |
| --------------------- | --------------- | ---------------------------------- |
| `id`                  | `Long`          | PK auto-gerado                     |
| `fromWalletId`        | `FK`            | Carteira de origem                 |
| `toWalletId`          | `FK`            | Carteira de destino                |
| `debitTransactionId`  | `FK`            | Transação de saque (origem)      |
| `creditTransactionId` | `FK`            | Transação de depósito (destino) |
| `amount`              | `BigDecimal`    | Valor transferido                  |
| `timestamp`           | `LocalDateTime` | Data/hora da operação            |

---

## Fluxo de uma Transação

```
1.  Cliente → POST /deposit
2.  WalletController valida o request
3.  TransactionService monta TransactionCommand
4.  KafkaTransactionEventPublisher publica mensagem no tópico wallet-transactions
5.  API retorna TransactionResponse imediatamente (HTTP 200)

          — A partir daqui o processamento é assíncrono —

6.  KafkaTransactionConsumer recebe a mensagem
7.  RedisIdempotencyAdapter verifica se idempotencyId já foi processado
    ├── Se duplicado → descarta a mensagem silenciosamente
    └── Se novo → registra como IN_PROGRESS
8.  TransactionStrategyFactory resolve a estratégia correta
    ├── DEPOSIT  → DepositStrategy
    ├── WITHDRAW → WithdrawStrategy
    └── TRANSFER → TransferStrategy (2 operações atômicas)
9.  Estratégia aplica regra de negócio no aggregate Wallet
10. WalletPersistenceAdapter atualiza saldo no PostgreSQL (com lock)
11. TransactionPersistenceAdapter registra a transação com snapshot de saldo
12. Redis marcado como COMPLETED
```

---

## Padrões de Design


| Padrão                          | Onde é aplicado                                                                   |
| -------------------------------- | ---------------------------------------------------------------------------------- |
| **Hexagonal / Ports & Adapters** | Separação completa domain ↔ infrastructure                                      |
| **Strategy**                     | `DepositStrategy`, `WithdrawStrategy`, `TransferStrategy`                          |
| **Factory**                      | `TransactionStrategyFactory` resolve estratégia por `TransactionType`             |
| **Command**                      | `TransactionCommand` como objeto de intenção do caso de uso                      |
| **Domain Event**                 | `TransactionMessage` publicado via Kafka após intenção registrada               |
| **Idempotency**                  | `IdempotencyRepository` → Redis `setIfAbsent` previne duplicatas                  |
| **Optimistic Locking**           | `@Version` em `WalletEntity` detecta atualizações concorrentes                   |
| **Pessimistic Locking**          | `@Lock(PESSIMISTIC_WRITE)` em `JpaWalletRepository` bloqueia leituras concorrentes |

---

## Concorrência e Consistência

O sistema combina três estratégias para garantir consistência em cenários de alta concorrência:

**Pessimistic Locking (`SELECT FOR UPDATE`):** ao buscar uma carteira para processar uma transação, é aplicado um lock exclusivo no banco de dados. Isso garante que nenhuma outra transação possa ler ou modificar o mesmo registro até o commit da transação atual.

**Optimistic Locking via `@Version`:** o campo `version` na entidade `WalletEntity` é incrementado a cada atualização. Se duas transações tentarem atualizar a mesma versão simultaneamente, a segunda receberá uma `OptimisticLockException` e será rejeitada.

**Idempotência via Redis:** o campo `idempotencyId` (UUID enviado pelo cliente) é registrado no Redis com `setIfAbsent` antes de qualquer processamento. Se uma segunda mensagem com o mesmo ID chegar (retry do Kafka, falha de rede, etc.), ela é descartada antes de qualquer operação no banco de dados.

---

## Resiliência com Kafka

- **Processamento FIFO:** Kafka garante ordem dentro de uma partição. Transações de uma mesma carteira são roteadas para a mesma partição, assegurando ordenação.
- **Transações no Producer:** `@Transactional("kafkaTransactionManager")` garante exactly-once delivery — apenas uma mensagem por operação é publicada, mesmo em caso de retry da aplicação.
- **Retry automático com backoff:** `@RetryableTopic` com backoff exponencial reprocessa mensagens em caso de falha transiente (ex: timeout no banco).
- **Dead Letter Queue (DLQ):** após esgotar as tentativas de retry, a mensagem é enviada para um tópico DLQ via `@DltHandler`, permitindo análise e reprocessamento manual.
- **Ack manual:** o consumer só commita o offset após processar com sucesso (`AcknowledgmentMode.MANUAL`), garantindo at-least-once delivery e que nenhuma mensagem seja perdida.

---

## Testes

### Testes Unitários

Localizados em `src/test/.../unit/`. Utilizam **Mockito** para mockar repositórios e serviços, testando a lógica de negócio de forma totalmente isolada de infraestrutura.

### Testes Funcionais

Localizados em `src/test/.../functional/`. Utilizam **Testcontainers** com o `docker-compose.yml` do projeto para subir instâncias reais de PostgreSQL, Kafka e Redis, validando o fluxo completo end-to-end.

Para rodar apenas os testes unitários:

```bash
mvn test -Dtest="**/unit/**"
```

Para rodar todos os testes (incluindo funcionais — requer Docker):

```bash
mvn verify
```

---

## Melhorias Futuras

- **Spring Security** — autenticação e autorização via JWT / OAuth2
- **Rate limiting** — proteção contra abuso dos endpoints (ex: Bucket4j)
- **Observabilidade** — integração com OpenTelemetry / Micrometer / Grafana
- **Notification service** — notificar o usuário após cada transação (email/push)
- **Paginação** no histórico de transações

## Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.
