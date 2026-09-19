# AGENTS.md — backend-leads

## Stack

- Java 21, Spring Boot 3.5.3, Maven wrapper (`./mvnw`), PostgreSQL 16
- Flyway (migrations in `src/main/resources/db/migration/`)
- Lombok (`@RequiredArgsConstructor`, `@Builder`, `@Getter/@Setter`)
- Spring Security + OAuth2 Client / Resource Server (Google login + local JWT)
- JWT via `jjwt` 0.12.3 (HMAC-SHA256 local, fallback to Google JWKS)
- OpenAPI/Swagger via SpringDoc 2.8.9 (`/swagger-ui.html`, `/v3/api-docs`)
- Spring Boot Actuator (`/actuator/health`, `/actuator/info`, `/actuator/metrics`)
- Spring Validation (`jakarta.validation`)
- HTTP client via `RestClient` (Spring Web), async via `@EnableAsync` + `ThreadPoolTaskExecutor`
- Spotless/Google Java Format — **applied on `compile` phase** (`spotless:apply` goal bound to `compile`)
- Docker multi-stage build (`maven:3.9.11-eclipse-temurin-21` → `eclipse-temurin:21-jre`)
- No CI/CD workflows in repo

## Quick start
- 1. Create the Docker network (required by docker-compose)
  - docker network create lead-network

- 2. Start PostgreSQL + application
  - docker compose up -d          # app available on :8080


## Required env vars

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | PostgreSQL connection (passed straight through to the app service in compose) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP (hostinger, port 465) |
| `JWT_SECRET` | HMAC-SHA256 key for local JWT signing |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google OAuth2 credentials |
| `OAUTH2_ISSUER_URI` | OAuth2 issuer URI (e.g., `https://accounts.google.com`) |

Env vars are loaded from `.env` (gitignored).
For local dev without Docker, run `./mvnw spring-boot:run`.

## Commands

```bash
./mvnw compile          # compile + auto-format (Spotless runs as part of compile)
./mvnw test             # single test class: BackendLeadsApplicationTests (context load, needs DB)
./mvnw spotless:apply   # format-only
./mvnw spotless:check   # check formatting without writing
./mvnw spring-boot:run  # dev server
./mvnw clean install    # full build (skip tests: -DskipTests)
```

## Architecture

```
com.br.semesperanca.backendleads
├── application/
│   ├── controller/
│   │   ├── LeadController              # /leads/** (receive, search, getById)
│   │   ├── security/
│   │   │   └── AuthController          # /api/auth/** (login, register/full-admin, change-password)
│   │   └── user/
│   │       ├── ManagerController       # /api/managers/** (CRUD + toggle-active)
│   │       └── RealEstateAgentController # /api/real-estate-agents/** (CRUD + toggle-active)
│   └── model/
│       ├── LeadDTO.java                # record for lead response/creation
│       ├── LeadReceivedNotificationDTO # record for incoming lead notification
│       ├── InsightRequest.java         # record for insight API request body (snake_case)
│       ├── InsightResponse.java        # record for insight API response
│       ├── request/                    # LoginRequest, CreateFullAdminRequest, CreateManagerRequest,
│       │                               # CreateRealEstateAgentRequest, UpdateManagerRequest,
│       │                               # UpdateRealEstateAgentRequest, ChangePasswordRequest
│       └── response/                   # LoginResponse, ManagerResponse, RealEstateAgentResponse
├── domain/
│   ├── model/
│   │   ├── Lead.java                  # JPA entity (lead table)
│   │   ├── Role.java                  # JPA entity (role table)
│   │   └── user/
│   │       ├── User.java              # abstract JPA entity (users table, JOINED inheritance, UserDetails)
│   │       ├── Manager.java           # extends User (adds branchOffice)
│   │       └── RealEstateAgent.java   # extends User (adds creci)
│   ├── repository/
│   │   ├── LeadRepository.java        # JpaSpecificationExecutor<Lead>
│   │   ├── RoleRepository.java
│   │   └── user/
│   │       ├── UserRepository.java
│   │       ├── ManagerRepository.java
│   │       └── RealEstateAgentRepository.java
│   └── service/
│       ├── LeadService.java           # receive (email + insight), search (Specifications), getById
│       ├── EmailService.java          # HTML email via template
│       ├── InsightClient.java         # HTTP client for insight API (@Async)
│       ├── security/
│       │   ├── AuthService.java       # login, createFullAdmin, changePassword
│       │   └── JwtService.java        # JWT generation/validation
│       └── user/
│           ├── ManagerService.java    # CRUD + toggleActive
│           └── RealEstateAgentService.java # CRUD + toggleActive
├── configuration/
│   ├── MailProperties.java            # @ConfigurationProperties(prefix = "app.mail")
│   ├── MailConfiguration.java         # enables MailProperties
│   ├── SwaggerConfig.java             # OpenAPI bean with Bearer JWT security scheme
│   ├── AsyncConfiguration.java        # @EnableAsync + ThreadPoolTaskExecutor
│   ├── exception/
│   │   └── GlobalExceptionHandler.java # @RestControllerAdvice (Runtime, AccessDenied, Auth, Validation, 500)
│   └── security/
│       ├── OAuth2LoginSecurityConfig.java     # two SecurityFilterChain beans (API + OAuth2 login), CORS, BCrypt
│       ├── CustomJwtDecoder.java              # local JWT → Google JWKS fallback
│       ├── CustomJwtAuthenticationConverter.java # role extraction (Google → REAL_ESTATE_AGENT)
│       ├── CustomAuthenticationEntryPoint.java # JSON 401
│       ├── CustomAccessDeniedHandler.java     # JSON 403
│       └── OAuth2SuccessHandler.java          # auto-create user from Google, redirect to frontend
└── logging/
    └── LoggingFilter.java            # Filter (MDC ip/user-agent)
```

## API

### Auth (`/api/auth`)
| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Local login (email + password), returns JWT |
| `POST` | `/api/auth/register/full-admin` | `FULL_ADMIN` | Create new super admin |
| `PATCH` | `/api/auth/change-password` | Authenticated | Change own password |

### Real Estate Agents (`/api/real-estate-agents`)
| Method | Path | Role | Description |
|---|---|---|---|
| `GET` | `/api/real-estate-agents` | `FULL_ADMIN`, `ADMIN` | List all |
| `POST` | `/api/real-estate-agents` | `FULL_ADMIN`, `ADMIN` | Create |
| `GET` | `/api/real-estate-agents/{id}` | `FULL_ADMIN`, `ADMIN`, `REAL_ESTATE_AGENT` | Get by ID |
| `PUT` | `/api/real-estate-agents/{id}` | `FULL_ADMIN`, `ADMIN` | Update |
| `PATCH` | `/api/real-estate-agents/{id}/toggle-active` | `FULL_ADMIN`, `ADMIN` | Activate/deactivate |

### Managers (`/api/managers`)
| Method | Path | Role | Description |
|---|---|---|---|
| `GET` | `/api/managers` | `FULL_ADMIN`, `ADMIN` | List all |
| `POST` | `/api/managers` | `FULL_ADMIN`, `ADMIN` | Create |
| `GET` | `/api/managers/{id}` | `FULL_ADMIN`, `ADMIN`, `MANAGER` | Get by ID |
| `PUT` | `/api/managers/{id}` | `FULL_ADMIN`, `ADMIN`, `MANAGER` | Update |
| `PATCH` | `/api/managers/{id}/toggle-active` | `FULL_ADMIN`, `ADMIN` | Activate/deactivate |

### Leads (`/leads`)
| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/leads/receive` | Authenticated | Receive lead notification (email + async insight API call) |
| `GET` | `/leads/search` | Authenticated | Filters: `status`, `corretorId`, `origin`, `search` (name/email LIKE). Pagination via Spring's `Pageable` |
| `GET` | `/leads/search/{id}` | Authenticated | Returns `LeadDTO` by UUID (includes `insightId`) |

### OAuth2
| Method | Path | Description |
|---|---|---|
| `GET` | `/login/oauth2/code/google` | Google callback — auto-creates user, redirects to `http://localhost:3000/auth/callback?token=...` |

### Actuator
| Path | Description |
|---|---|
| `/actuator/health` | Health check |
| `/actuator/info` | App info |
| `/actuator/metrics` | Metrics |

### Swagger
| Path | Description |
|---|---|
| `/swagger-ui.html` | Swagger UI |
| `/v3/api-docs` | OpenAPI JSON |

## Database tables (via Flyway)

| Migration | Tables created |
|---|---|
| `V001__create_table_role.sql` | `role`, `plan`, `real_estate_agency`, `subscription` + seeds roles |
| `V002__create_table_real_estate_agency.sql` | `"users"`, `real_estate_agent`, `manager`, `team`, `team_agent` |
| `V003__create_table_portal.sql` | `portal`, `funnel_stage` |
| `V004__create_table_leads.sql` | `lead` (status: new/in_progress/converted/lost/archived, insight_id) |
| `V005__create_table_lead_interaction.sql` | `lead_interaction` |
| `V006__create_triggers.sql` | `set_updated_at()` function + triggers |
| `V007__seed_system_agency_and_admins.sql` | Seeds "Sistema" agency + two FULL_ADMIN users |

## Code conventions

- Google Java Format (enforced by Spotless on `compile` — **always run `./mvnw compile` before committing**)
- Constructor injection via `@RequiredArgsConstructor` (no field injection)
- Records for DTOs (request, response); JPA entities use Lombok (`@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`)
- JPA inheritance: `User` is abstract with `InheritanceType.JOINED`, extended by `Manager` and `RealEstateAgent`
- Flyway migrations are plain SQL in `src/main/resources/db/migration/`
- Application properties in `application.yml` use env var placeholders (`${DB_URL}`, etc.)
- Security: two `SecurityFilterChain` beans — API (`/api/**`, `/swagger-ui/**`, `/v3/api-docs/**`) and OAuth2 login fallback
- External HTTP calls via `RestClient` (built from auto-configured `RestClient.Builder`); async operations via `@Async` with dedicated `ThreadPoolTaskExecutor`

## Testing notes

- Only one test class exists: `BackendLeadsApplicationTests` (@SpringBootTest, context loads)
- Tests require a running PostgreSQL — no Testcontainers or H2 profile configured
