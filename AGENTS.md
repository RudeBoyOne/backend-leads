# AGENTS.md — backend-leads

## Stack

- Java 21, Spring Boot 3.5.3, Maven wrapper (`./mvnw`), PostgreSQL 16
- Flyway (migrations in `src/main/resources/db/migration/`)
- Lombok (`@RequiredArgsConstructor`, `@Builder`, `@Getter/@Setter`)
- Spring Security + OAuth2 Client / Resource Server (Google login + local JWT)
- JWT: emissão/parsing local via `jjwt` 0.12.3 (HS256, claim `iss`); decodificação no resource server via Spring Security `NimbusJwtDecoder` com roteamento por `iss` (local HS256 + Google JWKS), validação de `iss`/`aud`
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

Optional (defaults in `application.yml`): `JWT_ISSUER` (iss claim of local tokens, default `http://backend-leads.local`), `FRONTEND_URL` (OAuth2 success redirect, default `http://localhost:3000`).

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
│   │   ├── LeadController              # /leads/** (receive, search, getById, delete)
│   │   ├── security/
│   │   │   └── AuthController          # /api/auth/** (login, register/full-admin, change-password)
│   │   └── user/
│   │       ├── ManagerController       # /api/managers/** (CRUD + toggle-active)
│   │       └── RealEstateAgentController # /api/real-estate-agents/** (CRUD + toggle-active)
│   └── model/
│       ├── LeadDTO.kt                # record for lead response/creation
│       ├── LeadReceivedNotificationDTO # record for incoming lead notification
│       ├── InsightRequest.kt         # record for insight API request body (snake_case)
│       ├── InsightResponse.kt        # record for insight API response
│       ├── request/                    # LoginRequest, CreateFullAdminRequest, CreateManagerRequest,
│       │                               # CreateRealEstateAgentRequest, UpdateManagerRequest,
│       │                               # UpdateRealEstateAgentRequest, ChangePasswordRequest
│       └── response/                   # LoginResponse, ManagerResponse, RealEstateAgentResponse
├── domain/
│   ├── model/
│   │   ├── Lead.kt                  # JPA entity (lead table)
│   │   ├── Role.kt                  # JPA entity (role table)
│   │   └── user/
│   │       ├── User.kt              # abstract JPA entity (users table, JOINED inheritance, UserDetails)
│   │       ├── Manager.kt           # extends User (adds branchOffice)
│   │       └── RealEstateAgent.kt   # extends User (adds creci)
│   ├── repository/
│   │   ├── LeadRepository.kt        # JpaSpecificationExecutor<Lead>
│   │   ├── RoleRepository.kt
│   │   └── user/
│   │       ├── UserRepository.kt
│   │       ├── ManagerRepository.kt
│   │       └── RealEstateAgentRepository.kt
│   └── service/
│       ├── LeadService.kt           # receive (email + insight), search (Specifications), getById
│       ├── EmailService.kt          # HTML email via template
│       ├── InsightClient.kt         # HTTP client for insight API (@Async)
│       ├── security/
│       │   ├── AuthService.kt       # login, createFullAdmin, changePassword
│       │   └── JwtService.kt        # JWT generation/validation
│       └── user/
│           ├── ManagerService.kt    # CRUD + toggleActive
│           └── RealEstateAgentService.kt # CRUD + toggleActive
├── configuration/
│   ├── MailProperties.kt            # @ConfigurationProperties(prefix = "app.mail")
│   ├── MailConfiguration.kt         # enables MailProperties
│   ├── SwaggerConfig.kt             # OpenAPI bean with Bearer JWT security scheme
│   ├── AsyncConfiguration.kt        # @EnableAsync + ThreadPoolTaskExecutor
│   ├── exception/
│   │   └── GlobalExceptionHandler.kt # @RestControllerAdvice (Runtime, AccessDenied, Auth, Validation, 500)
│   └── security/
│       ├── OAuth2LoginSecurityConfig.kt      # 2 chains: OAuth2 login (estreita) + API catch-all, CORS, BCrypt
│       ├── CustomJwtDecoder.kt               # delegating JwtDecoder por claim iss (local HMAC + Google JWKS), valida iss/aud
│       ├── CustomJwtAuthenticationConverter.kt # issuer-aware: local → claim role; Google → role do banco (UserRepository)
│       ├── CustomAuthenticationEntryPoint.kt # JSON 401
│       ├── CustomAccessDeniedHandler.kt     # JSON 403
│       └── OAuth2SuccessHandler.kt          # auto-cria usuário Google (email_verified), repassa ID token, redirect via app.frontend-url
└── logging/
    └── LoggingFilter.kt            # Filter (MDC ip/user-agent)
```

## Autenticação e autorização (híbrida)

Dois fluxos de autenticação convivem na aplicação: **OAuth2/OpenID Connect (Google)** para login de browser e **JWT local (HS256)** para login por e-mail/senha. A API aceita tokens de ambas as origens.

### Filter chains (Spring Security)

| Bean | Order | Matcher | Responsabilidade |
|---|---|---|---|
| `oauth2FilterChain` | 1 | `/oauth2/**`, `/login/**`, `/error` | Fluxo OAuth2 do browser (login page + callback Google) |
| `apiFilterChain` | 2 | catch-all (sem matcher) | Proteção JWT de **todas** as demais rotas — rota nova não exige configuração |

- `apiFilterChain` é catch-all: qualquer rota fora da chain 1 cai aqui e é protegida por JWT automaticamente.
- Públicas (permitAll): `OPTIONS /**`, `POST /api/auth/login`, `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health/**`, `/error`.
- Não autenticado → `401` JSON (`CustomAuthenticationEntryPoint`); sem permissão de role → `403` JSON (`CustomAccessDeniedHandler`).

### Emissão de tokens

- Token local (`JwtService`): claims `iss = app.jwt.issuer`, `sub = userId`, `role` e `email`, `iat`/`exp`; assinado **HS256** (`Jwts.SIG.HS256`) com `JWT_SECRET`.
- Login Google: `OAuth2SuccessHandler` repassa o **ID token do Google** ao frontend (`{app.frontend-url}/auth/callback?token=<id_token>`); esse mesmo token é aceito pela API.

### Decodificação por issuer (`CustomJwtDecoder`)

O decoder lê o claim `iss` do token (sem verificar assinatura) e roteia para o decoder correto. Issuer desconhecido ou token malformado → `BadJwtException` → `401`.

| `iss` | Decoder | Validações |
|---|---|---|
| `app.jwt.issuer` (ex.: `http://backend-leads.local`) | `NimbusJwtDecoder.withSecretKey(JWT_SECRET)`, HS256 | `JwtValidators.createDefaultWithIssuer(app.jwt.issuer)` (iss + exp) |
| `OAUTH2_ISSUER_URI` (ex.: `https://accounts.google.com`) | `NimbusJwtDecoder.withJwkSetUri(...)` — JWKS carregado de forma **lazy** (sem rede no boot) | `createDefaultWithIssuer(OAUTH2_ISSUER_URI)` + `aud` deve conter `GOOGLE_CLIENT_ID` |

### Autorização por token (`CustomJwtAuthenticationConverter`)

- Token local (`iss` = app): autoridade `ROLE_<claim role>` (a role veio do banco na emissão).
- Token Google: a role é resolvida **no banco** via `UserRepository.findByEmail`, exigindo `email_verified`:

| Situação | Autoridade |
|---|---|
| Usuário encontrado e ativo | `ROLE_<role>` real do banco (`role`/`"users"`) |
| Usuário inativo | `401` (`BadCredentialsException`) |
| Usuário inexistente ou e-mail não verificado | `ROLE_REAL_ESTATE_AGENT` (regra do auto-create via Google) |

### Fluxo OAuth2 (browser)

1. Browser acessa `/oauth2/authorization/google`.
2. Callback `GET /login/oauth2/code/google`.
3. `OAuth2SuccessHandler`: se o e-mail não existe e `email_verified == true`, cria `RealEstateAgent` com role `REAL_ESTATE_AGENT`.
4. Redireciona para `{app.frontend-url}/auth/callback?token=<id_token>`.

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
| `DELETE` | `/leads/{id}` | Authenticated | Delete lead by UUID |

### OAuth2
| Method | Path | Description |
|---|---|---|
| `GET` | `/login/oauth2/code/google` | Google callback — auto-creates user (if `email_verified`), redirects to `{app.frontend-url}/auth/callback?token=<id_token>` |

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
- Security: two `SecurityFilterChain` beans (`oauth2FilterChain` estreita + `apiFilterChain` catch-all) — ver seção «Autenticação e autorização (híbrida)»
- External HTTP calls via `RestClient` (built from auto-configured `RestClient.Builder`); async operations via `@Async` with dedicated `ThreadPoolTaskExecutor`

## Testing notes

- Only one test class exists: `BackendLeadsApplicationTests` (@SpringBootTest, context loads)
- Tests require a running PostgreSQL — no Testcontainers or H2 profile configured
