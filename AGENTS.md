# AGENTS.md — backend-leads

## Stack

- Java 21, Spring Boot 3.5.3, Maven wrapper (`./mvnw`), PostgreSQL 16
- Flyway (migrations in `src/main/resources/db/migration/`)
- Lombok (`@RequiredArgsConstructor`, `@Builder`, `@Getter/@Setter`)
- Spotless/Google Java Format — **applied on `compile` phase** (`spotless:apply` goal bound to `compile`)
- No CI/CD workflows in repo

## Quick start

```bash
cp .env.example .env                          # create env file with dev defaults
docker compose up -d                          # start PostgreSQL + app on :8080
```

## Required env vars

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_NAME`, `USER_NAME`, `PASSWORD` | PostgreSQL connection |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP (hostinger, port 465) |

Env vars are loaded from `.env` (gitignored).
For local dev without Docker, run `./mvnw spring-boot:run`.

## Commands

```bash
./mvnw compile          # compile + auto-format (Spotless runs as part of compile)
./mvnw test             # single test class: BackendLeadsApplicationTests (context load, needs DB)
./mvnw spotless:apply   # format-only
./mvnw spotless:check   # check formatting without writing
./mvnw spring-boot:run  # dev server
./mvnw clean install    # full build
```

## Architecture

```
com.br.semesperanca.backendleads
├── application/controller/   # REST endpoints
│   └── LeadController        # POST /leads/receive, GET /leads/search, GET /leads/search/{id}
├── application/model/        # Request DTOs
├── domain/model/             # JPA entity (Lead), response DTO (LeadDTO)
├── domain/repository/        # JPA + Specification executor
├── domain/service/           # LeadService, EmailService
├── configuration/            # MailProperties, MailConfiguration
└── logging/                  # LoggingFilter (MDC ip/user-agent)
```

## API

- `POST /leads/receive` — expects `LeadReceivedNotificationDTO`. Currently only sends email notification; does **not** persist to DB.
- `GET /leads/search` — filters: `status`, `corretorId`, `origin`, `search` (name/email LIKE). Pagination via Spring's `Pageable`.
- `GET /leads/search/{id}` — returns `LeadDTO` by UUID.

## Code conventions

- Google Java Format (enforced by Spotless on `compile` — **always run `./mvnw compile` before committing**)
- Constructor injection via `@RequiredArgsConstructor` (no field injection)
- Records for DTOs, JPA entities use Lombok (`@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`)
- Flyway migrations are plain SQL in `src/main/resources/db/migration/`
- Application properties in `application.yml` use env var placeholders (`${DB_URL}`, etc.)

## Testing notes

- Only one test class exists: `BackendLeadsApplicationTests` (@SpringBootTest, context loads)
- Tests require a running PostgreSQL — no testcontainers or H2 profile configured
