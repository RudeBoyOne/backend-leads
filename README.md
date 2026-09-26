# Backend-leads

API de gestão de leads imobiliários. Recebe leads de portais externos, envia e-mail de notificação,
consulta insights e gerencia usuários (corretores, gerentes), imóveis, atividades e lead owners.

**Stack:** Kotlin 2.3.21 (JVM 25) · Spring Boot 4.1.1 · Spring Web · Spring Data JPA · Spring Security
(OAuth2 Client + Resource Server) · jjwt 0.12.7 · PostgreSQL 16 · Flyway · Spring Mail · Actuator ·
SpringDoc 3.1.1 · Gradle Wrapper. Sem CI/CD.

## Requisitos e execução

Pré-requisitos: **JDK 25**, **Docker** + **Docker Compose** (o Gradle wrapper `./gradlew` dispensa Gradle local).

```bash
docker network create lead-network   # uma única vez, exigida pelo docker-compose
docker compose up -d                 # sobe PostgreSQL 16 + app em :8080
```

Sem Docker (PostgreSQL acessível em `localhost:5432`):

```bash
./gradlew bootRun
```

### Comandos

```bash
./gradlew compile       # compila (Kotlin) — sempre antes de commitar
./gradlew test          # único teste: contexto da aplicação (exige PostgreSQL no ar)
./gradlew clean build -x test
docker compose down   # parar (use -v para remover o volume do banco)
```

### Variáveis de ambiente

Carregadas do `.env` (gitignored; ver `.env.example`). Obrigatórias:

| Variável | Uso |
| --- | --- |
| `DB_URL`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Conexão PostgreSQL (`DB_URL=postgres:5432` no compose) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP (Hostinger, porta 465) |
| `JWT_SECRET` | Chave HMAC-SHA256 (mín. 32 bytes) para assinatura dos tokens locais |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Credenciais OAuth2 do Google |
| `OAUTH2_ISSUER_URI` | Emissor OAuth2 (ex.: `https://accounts.google.com`) |

Opcionais (default em `application.yml`): `JWT_ISSUER` (claim `iss` dos tokens locais,
`http://backend-leads.local`) e `FRONTEND_URL` (redirect pós-login Google, `http://localhost:3000`).

## Arquitetura

Kotlin em camadas, pacote raiz `com.br.semesperanca.backendleads` (`src/main/kotlin`):

```
application/  controller/ (REST) · model/ (DTOs: data classes)
domain/       model/ (JPA) · repository/ · service/
configuration/ security/ · exception/ · mail · swagger · async
logging/      LoggingFilter (MDC)
```

Regras de separação: controllers são finos (delegam a `domain/service`); DTOs são `data class` em
`application/model/{request,response}`; entidades JPA em `domain/model`; nada de regra de negócio no
controller. Injeção sempre por construtor (`@RequiredArgsConstructor`), nunca por campo.

O contrato HTTP completo está no Swagger (`/swagger-ui.html`, `/v3/api-docs`) — não duplique tabelas de
endpoints aqui. Grupos de rotas: `/auth`, `/leads`, `/managers`, `/real-estate-agents`, `/properties`,
`/branch-owners`, `/activities`.

## Segurança (híbrida) — regras obrigatórias

Duas `SecurityFilterChain` em `configuration/security/OAuth2LoginSecurityConfig.kt`:

1. `oauth2FilterChain` (`@Order(1)`, matcher `/oauth2/**`, `/login/**`, `/error`) — login social no
   browser. Fora dela, `anyRequest().permitAll()`.
2. `apiFilterChain` (`@Order(2)`, **catch-all sem matcher**) — toda rota fora da chain 1 nasce
   protegida por JWT.

Consequências práticas:

- **Rota nova = autenticada por padrão.** Só adicione `permitAll` se for realmente público; caso
  contrário, detalhe explicitamente as roles em `authorizeHttpRequests` (o `anyRequest().authenticated()`
  é o fallback).
- Papéis: `FULL_ADMIN`, `ADMIN`, `MANAGER`, `REAL_ESTATE_AGENT`. Papéis novos exigem seed na migration
  de `role` (V001) — nunca hardcode strings fora da config.
- `GET /actuator/health/**`, `POST /auth/login`, `/swagger-ui/**`, `/v3/api-docs/**` e `/error` são
  públicos. Falta de autenticação → `401` JSON; falta de role → `403` JSON.

**Emissão e validação de tokens**

- Token local: `JwtService` assina HS256 com `JWT_SECRET`; claims `iss` (app), `sub` (userId), `role`,
  `email`, `iat`, `exp`.
- Login Google: `OAuth2SuccessHandler` cria o `RealEstateAgent` quando o e-mail não existe e
  `email_verified == true`, e redireciona para `{app.frontend-url}/auth/callback?token=<id_token>`.
- `CustomJwtDecoder` lê o claim `iss` e roteia: issuer local → HS256 com `JWT_SECRET`; issuer do Google
  → JWKS (carregado lazy, sem rede no boot). Issuer desconhecido → `401`. Google exige `aud` com
  `GOOGLE_CLIENT_ID`.
- `CustomJwtAuthenticationConverter` resolve a autoridade: token local → `ROLE_<claim role>`; token
  Google → role real do banco via `UserRepository.findByEmail` (usuário inexistente ou e-mail não
  verificado cai em `ROLE_REAL_ESTATE_AGENT`; usuário inativo → `401`).

## Convenções

- DTOs: `data class` com validação `jakarta.validation`; entidades JPA: Lombok
  (`@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`).
- Herança JPA: `User` abstrata com `InheritanceType.JOINED`, estendida por `Manager` e
  `RealEstateAgent`.
- Migrações Flyway: SQL puro em `src/main/resources/db/migration/` (`V0NN__descricao.sql`); nunca edite
  migration já aplicada — crie uma nova. `open-in-view: false`: carregue o que precisa de forma explícita.
- `application.yml` referencia variáveis de ambiente via `${...}`; nada de valor secret no repositório.
- HTTP externo via `RestClient` (a partir do `RestClient.Builder` autoconfigurado); tarefas assíncronas
  via `@Async` + `ThreadPoolTaskExecutor` de `AsyncConfiguration`.
- Exceções: `GlobalExceptionHandler` (`@RestControllerAdvice`) trata Runtime, AccessDenied, Auth,
  Validation e 500 — prefira lançar exceção tipada a montar `ResponseEntity` no controller.

## Banco de dados

PostgreSQL 16 gerenciado por Flyway (`V001`–`V013`): `role`, `plan`, `real_estate_agency`,
`subscription`, `"users"`, `real_estate_agent`, `manager`, `team`, `team_agent`, `portal`,
`funnel_stage`, `lead`, `lead_interaction`, `activity`, `property`, `branch_owner` (V008+), além de
triggers de `updated_at` (V006) e seed da agency "Sistema" com dois `FULL_ADMIN` (V007).

## Testes

Um único teste, `BackendLeadsApplicationTests` (@SpringBootTest, apenas carrega o contexto). Exige
PostgreSQL em execução; não há Testcontainers nem perfil H2.

## Documentação da API

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Actuator: `/actuator/health`, `/actuator/info`, `/actuator/metrics`
