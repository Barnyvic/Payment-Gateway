# Payment Gateway

Spring Boot payment gateway with command/query APIs, idempotency, and an outbox worker for bank mutations.

## Run

```bash
mvn spring-boot:run
```

## Card data handling

PCI-minded rules implemented in this service:

- **Only** `POST /v1/payments/authorize` accepts raw card fields (`number`, `cvv`, `expiryMonth`, `expiryYear`).
- Card data is used **in memory** to call the bank synchronously during the authorize request. It is **not** stored in the outbox, database, or idempotency response snapshots.
- Query APIs (`GET` receipts by order/customer/payment ref) return amount and payment state only — never PAN or CVV.
- Idempotency fingerprints for authorize use order, customer, amount, card last-four, and expiry — not full PAN or CVV.
- Application logs run through a sanitizing Logback converter that masks PAN-like sequences and CVV JSON fields.

Capture, void, and refund continue to use the outbox asynchronously with bank tokens (`authorizationId` / `captureId`) only.

## Logging

The app uses **SLF4J** (the standard Java logging facade) with **Logback** via Spring Boot. Configure levels in `application.properties`, for example:

```properties
logging.level.com.paymentgateway=DEBUG
```

Log lines are passed through `SensitiveDataSanitizer` (see `logback-spring.xml`) to mask PAN and CVV patterns.

## Tests

```bash
mvn test
```

Integration tests use Testcontainers PostgreSQL and a stub `BankClient`.
