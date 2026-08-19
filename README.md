# eTIMS OSCU Spring PoC

A proof-of-concept Spring Boot microservice for integrating with the Kenya Revenue Authority (KRA) eTIMS OSCU API via the GavaConnect developer sandbox. This PoC is designed as an isolated, standalone microservice to validate the full integration lifecycle before embedding eTIMS compliance into a production SaaS platform.

---

## Table of Contents
1. [Project Background](#project-background)
2. [Architecture Decisions](#architecture-decisions)
3. [Tech Stack](#tech-stack)
4. [Project Structure](#project-structure)
5. [Getting Started](#getting-started)
6. [Environment Variables](#environment-variables)
7. [API Endpoints](#api-endpoints)
8. [Resilience and Async Queue](#resilience-and-async-queue)
9. [Alerting and Audit System](#alerting-and-audit-system)
10. [Environments](#environments)
11. [Roadmap](#roadmap)

---

## Project Background

### Registering with developer.go.ke
Access to the KRA eTIMS OSCU sandbox requires registration on the [Kenya Digital Economy Portal (developer.go.ke)](https://developer.go.ke). After approval, you receive an API consumer key and consumer secret for the **GavaConnect** gateway.

This PoC targets the **eTIMS Sandbox OSCU Automated Testing API** hosted on GavaConnect. The authentication flow is OAuth 2.0 `client_credentials`, meaning every request must be prefixed with a valid Bearer token obtained from the `/token/generate` endpoint.

### Sandbox Discovery and KRA Downtime
During the initial integration phase, I encountered a live service interruption formally announced on the developer portal:

> *"The eTIMS Sandbox OSCU Automated Testing API on the GavaConnect is currently experiencing a service interruption. We apologize for any inconvenience and appreciate your patience as we work to restore the service. GavaConnect Team"*

Rather than blocking development on a third-party dependency, **this downtime became the catalyst for building a more resilient system**. I used that time to implement the asynchronous PostgreSQL retry queue described below.
---

## Architecture Decisions

| Decision | Rationale |
|---|---|
| **Standalone Microservice** | Isolates the eTIMS blast radius from the main SaaS modular monolith. If KRA goes down, only this service degrades while user-facing features stay healthy. |
| **Async PostgreSQL Retry Queue** | Invoices are never lost to a KRA outage. They queue in PostgreSQL and are automatically synced when KRA recovers. |
| **`.env` for All Secrets** | Zero hardcoding. All API keys, DB credentials, and webhook URLs live in a `.env` file that is excluded from version control. |
| **No Spring `dotenv` Plugin** | Spring Boot's native `spring.config.import: optional:file:.env[.properties]` is used to read the `.env` file. This avoids third-party dependency risk and works identically. |
| **Manual Jackson Parsing** | KRA's GavaConnect gateway returns JSON responses with a `Content-Type: application/x-www-form-urlencoded` header. Spring's built-in REST client rejects this mismatch, so the raw response body is read as a `String` and parsed manually with `ObjectMapper`. |

---

## Tech Stack

| Component | Technology |
|---|---|
| **Language** | Java 25 |
| **Framework** | Spring Boot 4.1.0 |
| **Build Tool** | Apache Maven (via `mvnw` wrapper) |
| **Database** | PostgreSQL 18.6 |
| **ORM** | Spring Data JPA / Hibernate |
| **HTTP Client** | Spring `RestClient` (native, no external HTTP client library) |
| **JSON** | Jackson `ObjectMapper` (explicit dependency) |
| **Boilerplate Reduction** | Lombok |
| **Scheduling** | Spring `@Scheduled` |

### Maven Coordinates
```xml
<groupId>com.muad</groupId>
<artifactId>etims-oscu-spring-poc</artifactId>
<version>0.0.1-SNAPSHOT</version>
<packaging>jar</packaging>
```

### Spring Initializr Dependencies Added
- `spring-boot-starter-web` for the web layer and Jackson JSON
- `spring-boot-starter-data-jpa` for ORM and Hibernate
- `postgresql` (runtime) for the JDBC driver
- `lombok` (optional) for boilerplate reduction
- `com.fasterxml.jackson.core:jackson-databind` for explicit ObjectMapper injection

---

## Project Structure

```
src/main/java/com/muad/etims/
├── EtimsOscuSpringPocApplication.java    # Entry point with @EnableScheduling
│
├── config/
│   └── MockDataSeeder.java               # Seeds a PENDING transaction on startup (PoC only)
│
├── controller/
│   └── EtimsTestController.java          # Manual HTTP endpoints for testing the auth flow
│
├── dto/
│   └── response/
│       └── KraAuthResponse.java          # Java record for the KRA OAuth token response
│
├── entity/
│   ├── Transaction.java                  # Core invoice entity with KRA compliance fields
│   ├── EtimsStatus.java                  # Enum: PENDING, SYNCED, FAILED_RETRY, FATAL_ERROR
│   ├── KraSystemAudit.java               # Permanent DB log of downtime and recovery events
│   └── KraSystemEventType.java           # Enum: DOWNTIME_DETECTED, SYSTEM_RECOVERED
│
├── repository/
│   ├── TransactionRepository.java        # JPA queries for pending transaction batches
│   └── KraSystemAuditRepository.java     # JPA repository for the audit log
│
└── service/
    ├── KraAuthService.java               # OAuth 2.0 client_credentials token fetcher
    ├── KraAlertService.java              # Tracks failures and triggers DB audit plus Slack/Email alerts
    └── EtimsSyncWorker.java              # Scheduled background worker running every 30 seconds
```

---

## Getting Started

### Prerequisites
- Java 25+
- Apache Maven or the bundled `./mvnw` wrapper
- PostgreSQL 18.6 running on `127.0.0.1:5432`
- KRA GavaConnect API credentials from [developer.go.ke](https://developer.go.ke)

### 1. Create the Database
```bash
createdb -h 127.0.0.1 -U postgres etims_poc_db
```

### 2. Configure Environment Variables
Copy the `.env.example` file and populate it with your real values:
```bash
cp .env.example .env
```

### 3. Run the Application
```bash
./mvnw spring-boot:run
```

Hibernate will automatically create all necessary tables on first startup using `ddl-auto: update`.

---

## Environment Variables

All secrets are stored exclusively in a `.env` file at the project root. **This file is gitignored and must never be committed.**

```env
# PostgreSQL
DB_URL=jdbc:postgresql://127.0.0.1:5432/etims_poc_db
DB_USERNAME=postgres
DB_PASSWORD=your_password_here

# KRA GavaConnect API Credentials
KRA_API_BASE_URL=https://sbx.kra.go.ke/v1
KRA_CONSUMER_KEY=your_consumer_key_here
KRA_CONSUMER_SECRET=your_consumer_secret_here
KRA_PIN=your_kra_pin_here
KRA_DEVICE_SERIAL=your_device_serial_here
KRA_BRANCH_ID=00

# Alerting (Phase 2, uncomment and populate when ready)
# SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
# ALERT_EMAIL_SMTP_HOST=smtp.sendgrid.net
# ALERT_EMAIL_USER=apikey
# ALERT_EMAIL_PASS=your_smtp_key_here
```

---

## API Endpoints

### Test Controller (PoC only, to be removed before production)

| Method | Path | Description |
|---|---|---|
| `GET` | `/test/kra/auth` | Manually triggers a KRA OAuth2 token fetch and returns the result |

**Example:**
```bash
curl -s http://localhost:8080/test/kra/auth | jq .
```

---

## Resilience and Async Queue

The core architectural feature of this PoC is the **asynchronous PostgreSQL retry queue**.

### How It Works

```
User/SaaS -> Save Invoice (PENDING) -> PostgreSQL
                                           |
                         EtimsSyncWorker (every 30s)
                                           |
                             KRA API Available?
                            /                   \
                          YES                    NO
                           |                      |
                   Mark SYNCED            Increment retryCount
                   Store kraReceiptNumber  Stay FAILED_RETRY
                   Store qrCodeUrl        KraAlertService fires
                                          (if threshold hit)
```

### Transaction Status States

| Status | Meaning |
|---|---|
| `PENDING` | Invoice queued and not yet attempted |
| `SYNCED` | Successfully confirmed by KRA |
| `FAILED_RETRY` | Failed but scheduled for another retry |
| `FATAL_ERROR` | Max retries (5) exhausted and requires manual review |

### Transaction Entity Key Fields

| Field | Purpose |
|---|---|
| `tenantId` | Multi-tenant SaaS isolation |
| `invoiceNumber` | Internal unique reference |
| `customerPin` | KRA PIN for B2B tax claim validation |
| `taxableAmount`, `taxAmount`, `totalAmount` | KRA-compliant tax breakdown |
| `itemsPayload` | JSON line items required by KRA for itemized tax |
| `kraReceiptNumber` | KRA confirmation number, populated on SYNCED |
| `controlCode` | KRA control code for the QR stamp |
| `qrCodeUrl` | Printable QR code for the invoice |

---

## Alerting and Audit System

### Proactive Monitoring
`KraAlertService` tracks consecutive KRA API failures. Once the threshold is reached (default: 3 failures in a row), it does two things:

1. **Writes a `DOWNTIME_DETECTED` record** to the `kra_system_audit` PostgreSQL table.
2. **Dispatches an alert** via Slack Webhook (currently simulated via logs, with full webhook integration planned in the roadmap).

When KRA recovers and the next sync succeeds:
1. **Writes a `SYSTEM_RECOVERED` record** to `kra_system_audit`.
2. **Dispatches a recovery notification**.

### Why PostgreSQL Audit Log in Addition to Slack?
Slack's free tier has a 90-day message retention limit. The `kra_system_audit` table serves as a permanent, queryable ledger of every KRA outage and recovery event. Historical reliability data is always available for auditing, client SLA reporting, or dispute resolution, regardless of how long ago the event occurred.

---

## Environments

This project follows a three-environment lifecycle before any code reaches production.

| Environment | Branch | Purpose |
|---|---|---|
| `dev` | `dev` | Active development, experimental features, local testing |
| `staging` | `staging` | Pre-production validation, integration tests against sandbox |
| `production` | `main` | Live client traffic, nothing goes here without passing staging |

---

## Roadmap

- [ ] **Slack Webhook Integration** - Wire `KraAlertService.sendSlackAlert()` to a real Slack Incoming Webhook URL stored in `.env`.
- [ ] **Email Alerting** - Integrate SMTP via AWS SES or Brevo for downtime email notifications.
- [ ] **Full eTIMS Initialization Endpoint** - Implement the KRA device initialization and invoice submission flow once GavaConnect restores service.
- [ ] **Exponential Backoff** - Implement smart retry delays instead of fixed 30-second polling.
- [ ] **Admin Audit Endpoint** - Expose the `kra_system_audit` log via a secured REST endpoint for visibility like an internal dashboard.
- [ ] **Remove MockDataSeeder** - Replace the test seeder with a real invoice submission controller before staging promotion.
- [ ] **Docker Compose** - Add container-ready setup for clean environment parity across dev, staging, and production.
- [ ] **Infrastructure as Code** - Implement Infrastructure as Code for easier access and management.
