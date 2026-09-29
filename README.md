# Backend-leads

API de gestão de leads imobiliários. Recebe leads de portais externos, envia um e-mail de notificação,
consulta um serviço de insights e administra usuários (corretores e gerentes), imóveis, atividades e
responsáveis por ramais.

**Stack:** Kotlin 2.3.21 (JVM 25) · Spring Boot 4.1.1 · Spring Web · Spring Data JPA · Spring Security
(OAuth2 Client + Resource Server) · jjwt 0.12.7 · PostgreSQL 16 · Flyway · Spring Mail · Actuator ·
SpringDoc 3.1.1 · Gradle Wrapper. Não há CI/CD.

## Rodando o projeto

São necessários o **JDK 25** e o **Docker** com Docker Compose. Gradle não precisa ser instalado: o
wrapper (`./gradlew`) resolve a versão sozinho.

O compose depende de uma rede externa chamada `lead-network`, criada uma única vez:

```bash
docker network create lead-network
docker compose up -d
```

O PostgreSQL e a aplicação sobem juntos, e a aplicação responde em `:8080`.

Quem já tiver um PostgreSQL acessível em `localhost:5432` pode rodar sem o Docker:

```bash
./gradlew bootRun
```

### Comandos do dia a dia

```bash
./gradlew compile            # compila (Kotlin) — rode antes de commitar
./gradlew test               # único teste: carrega o contexto (precisa do PostgreSQL no ar)
./gradlew clean build -x test
docker compose down          # derruba os serviços (-v também apaga o volume do banco)
```

### Variáveis de ambiente

O `.env` é carregado no start. Ele é ignorado pelo git, e o modelo está em `.env.example`.

| Variável | Para que serve |
| --- | --- |
| `DB_URL`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Conexão com o PostgreSQL (no compose, `DB_URL=postgres:5432`) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP da Hostinger, na porta 465 |
| `JWT_SECRET` | Chave HMAC-SHA256, com no mínimo 32 bytes, que assina os tokens locais |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Credenciais do OAuth2 do Google |
| `OAUTH2_ISSUER_URI` | Emissor OAuth2, por exemplo `https://accounts.google.com` |

Duas variáveis são opcionais e já têm padrão no `application.yml`: `JWT_ISSUER`, que é o claim `iss`
dos tokens locais (`http://backend-leads.local`), e `FRONTEND_URL`, para onde o usuário vai depois do
login com Google (`http://localhost:3000`).

## Organização do código

O código vive em `src/main/kotlin`, sob o pacote `com.br.semesperanca.backendleads`, em quatro
camadas:

```
application/   controllers REST e DTOs (data classes)
domain/        entidades JPA, repositories e services
configuration/ segurança, tratamento de exceções, e-mail, Swagger, async
logging/       LoggingFilter, que popula o MDC
```

A separação é simples e vale respeitar. Controllers são finos e só delegam para `domain/service`;
regra de negócio mora no service. DTOs são `data class` em `application/model/{request,response}` e
as entidades JPA ficam em `domain/model`. Dependências entram sempre pelo construtor primário do
Kotlin, nunca por campo.

A lista completa de rotas está no Swagger, e é lá que ela deve ser mantida — este README não
duplica essa tabela. Os grupos são `/auth`, `/leads`, `/managers`, `/real-estate-agents`,
`/properties`, `/branch-owners` e `/activities`.

## Segurança

Dois fluxos de autenticação convivem na aplicação: login social com Google, via OAuth2/OIDC, e login
local por e-mail e senha, que devolve um JWT. A API aceita tokens dos dois.

Na prática isso são duas `SecurityFilterChain`, em `configuration/security/OAuth2LoginSecurityConfig.kt`:

- `oauth2FilterChain`, com `@Order(1)`, responde apenas por `/oauth2/**`, `/login/**` e `/error`. É o
  login social no browser, e dentro dela qualquer rota é liberada.
- `apiFilterChain`, com `@Order(2)`, não declara matcher nenhum, então funciona como catch-all:
  tudo que não caiu na primeira chain é protegido por JWT.

Dessa come as rotas novas já nascem autenticadas, sem configuração. Se a rota for realmente pública,
ela precisa aparecer explicitamente em `permitAll`. Nos demais casos, as roles vão em
`authorizeHttpRequests` e o `anyRequest().authenticated()` cobre o que sobrar.

Os papéis reconhecidos são `FULL_ADMIN`, `ADMIN`, `MANAGER` e `REAL_ESTATE_AGENT`. Um papel novo
precisa de seed na migration que cria a tabela `role` (V001). Hoje são públicos o
`GET /actuator/health/**`, o `POST /auth/login`, o Swagger, o `/v3/api-docs/**` e o `/error`. Falta
de autenticação devolve `401` em JSON; falta de papel, `403` em JSON.

### Emissão e validação dos tokens

O token local é assinado pela `JwtService` com HS256 e `JWT_SECRET`, e carrega `iss` (a aplicação),
`sub` (o id do usuário), `role`, `email`, `iat` e `exp`.

No login com Google, a `OAuth2SuccessHandler` cria um `RealEstateAgent` quando o e-mail ainda não
existe e `email_verified` é verdadeiro, e redireciona para
`{app.frontend-url}/auth/callback?token=<id_token>`.

A `CustomJwtDecoder` olha o claim `iss` e escolhe o decoder: para o issuer local, HS256 com
`JWT_SECRET`; para o issuer do Google, JWKS, carregado sob demanda e sem tocar a rede durante a
subida. Um issuer desconhecido resulta em `401`. No caso do Google, o `aud` precisa conter
`GOOGLE_CLIENT_ID`.

A `CustomJwtAuthenticationConverter` define a autoridade a partir do token. Token local usa o claim
`role`; token do Google busca a role no banco, via `UserRepository.findByEmail` — usuário inexistente
ou e-mail não verificado cai em `ROLE_REAL_ESTATE_AGENT`, e usuário inativo resulta em `401`.

## Convenções de código

**DTOs e entidades.** DTOs são `data class` com validação `jakarta.validation`. As entidades JPA são
classes comuns de Kotlin, com construtor primário e valores padrão, e **não declaram `open class` nem
`open var`** — quem abre classe e membros é o plugin `all-open`, configurado no `build.gradle.kts`
para `@Entity`, `@MappedSuperclass` e `@Embeddable`.

Isso não é cosmético. O Hibernate 7 recusa getters e setters `final` com o erro `Getter methods of
lazy classes cannot be final`, e em Kotlin `open class` sozinho não abriria os accessors de uma
`var`: seria preciso marcar cada propriedade, e esquecer uma passaria despercebida até a primeira
navegação lazy.

O `all-open` é configurado sem o plugin `noarg` de propósito. `Activity.lead` não tem valor padrão,
então as entidades dependem do construtor canônico, e um construtor vazio gerado automaticamente
mudaria a estratégia de instanciação do Hibernate.

**Herança de usuários.** `User` é abstrata, com `InheritanceType.JOINED`, e concentra o que é comum
a qualquer usuário. `Manager` acrescenta `branch_office` e `RealEstateAgent` acrescenta `creci`,
cada uma em sua própria tabela filha.

**Migrações.** SQL puro em `src/main/resources/db/migration/`, no formato `V0NN__descricao.sql`.
Migration já aplicada não se edita: cria-se uma nova. Como `open-in-view` está `false`, o que a
requisição precisa tem de ser carregado explicitamente.

**Configuração.** O `application.yml` referencia variáveis de ambiente com `${...}`, e nenhum valor
secreto entra no repositório.

**Integrações.** Chamadas HTTP externas usam `RestClient`. O `RestClient.Builder` vem do
`spring-boot-starter-restclient`, que não vem mais junto do starter web no Boot 4. Tarefas assíncronas
usam `@Async` com o `ThreadPoolTaskExecutor` definido em `AsyncConfiguration`.

**Jackson.** Versão 3: os imports ficam em `tools.jackson.*` e o `ObjectMapper` em
`tools.jackson.databind`. As anotações continuam em `com.fasterxml.jackson.annotation`.

**Exceções.** A `GlobalExceptionHandler` (`@RestControllerAdvice`) trata Runtime, AccessDenied, Auth,
Validation e 500. Preferir lançar uma exceção tipada a montar um `ResponseEntity` no controller.

## Banco de dados

PostgreSQL 16, gerenciado por Flyway. As migrations criam `role`, `plan`, `real_estate_agency` e
`subscription` (V001); `"users"`, `real_estate_agent`, `manager`, `team` e `team_agent` (V002);
`portal` e `funnel_stage` (V003); `lead` (V004); `lead_interaction` (V005); `activity` (V008);
`property` (V009) e `branch_owner` (V010).

A V006 instala os triggers de `updated_at`, e a V007 faz o seed da agency "Sistema" com dois
`FULL_ADMIN`. As V011 a V013 ajustam `activity`, `property` e `branch_owner`.

## Testes

**440 testes em 68 classes, 99,9% de cobertura de linha** — e o build falha se cair abaixo de 90%.
O `./gradlew test` sobe tudo sozinho: só precisa de Docker.

```bash
./gradlew test                                  # roda tudo
./gradlew build                                 # inclui a verificação de cobertura
./gradlew test jacocoTestReport                 # relatório em build/reports/jacoco/test/html
./gradlew test --tests '*LeadRepositoryTest'    # só uma classe
```

### Como os testes se dividem

| Tipo | Quantos | O que exercita |
| --- | --- | --- |
| Unitário (serviço) | 10 classes | Lógica de negócio com repositório, e-mail e insight mockados. As `Specification` são avaliadas contra um `CriteriaBuilder` instrumentado (`support/SpecProbe.kt`), o que verifica quantos filtros foram montados e com quais valores, sem subir banco. |
| Slice de controller | 7 classes | Os sete controllers via `@WebMvcTest`, com a camada web real (Jackson 3, validação de bean, `GlobalExceptionHandler`) e serviço mockado. |
| Repositório | 8 classes | `@DataJpaTest` contra o PostgreSQL de verdade: as 13 migrations do Flyway rodam e o `ddl-auto: validate` garante que o mapeamento bate com o schema. Cobre as queries derivadas, a composição de `Specification`, paginação e as constraints do banco. |
| Integração | 2 classes | Contexto completo: sobe a aplicação inteira e a matriz de segurança (401/403/público) com JWT assinado de verdade. |
| Entidade e DTO | 2 classes | Callbacks `@PrePersist`/`@PreUpdate`, o contrato de `UserDetails` e o `LeadDTO.toLead`/`fromLead`. |

### Banco de teste

O PostgreSQL vem do Testcontainers, não do `docker-compose.yml`. Isso não é preferência: o serviço
`postgres` do compose **não publica a porta 5432**, então um `DB_URL=postgres:5432` do `.env` — que
serve ao container da aplicação — não é alcançável de dentro do container de teste.

Há um container só para a suíte inteira, em `support/AbstractPostgresTest.kt`, compartilhado entre
os testes de repositório e os de contexto completo. Ele é iniciado à mão, sem `@Testcontainers`:
com a instância compartilhada, o `afterAll` da primeira classe de teste mataria o banco das
seguintes.

O `src/test/resources/application-test.yml` (perfil `test`) resolve os placeholders que o
`application.yml` interpola — `DB_*`, `JWT_SECRET`, `GOOGLE_*`, `MAIL_*` — sem nenhuma variável de
ambiente. Ele também define `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`, que é o que
impede o `JwtDecoder` de fazer um GET nos metadados do Google no start-up.

### Cobertura

O JaCoCo roda em todo `./gradlew build` e o `jacocoTestCoverageVerification` derruba o build abaixo
de **90% de linha**:

| Métrica | Cobertura |
| --- | --- |
| Linha | 99,9% (1347/1349) |
| Instrução | 98,5% |
| Ramo | 94,6% |
| Complexidade | 95,3% |
| Método | 96,5% |
| Classe | 98,5% |

Quatorze dos quinze pacotes estão em 100% de linha. A única exceção é o `main` de
`BackendLeadsApplication` (duas linhas), deixado de fora de propósito: exercitá-lo exigiria um
segundo contexto completo com um `DataSource` vivo, e ele não tem nenhuma linha própria para
verificar. Todas as classes e repositórios do domínio estão cobertos.

### Bugs encontrados e fixados em teste

A suíte não só mede cobertura — ela encontrou defeitos reais, que estão fixados em testes que
documentam o problema em vez de escondê-lo. A maioria está anotada no próprio teste:

- `AuthController.login` e `createFullAdmin` não têm `@Valid` no `@RequestBody`, então payload
  inválido volta com 200/201 em vez de 400.
- Rota inexistente devolve **500** em vez de 404: o `GlobalExceptionHandler` não estende
  `ResponseEntityExceptionHandler` e engole o `NoResourceFoundException` no
  `@ExceptionHandler(Exception::class)`.
- `Lead` não tem `@PrePersist`, e `lead.created_at`/`updated_at` são `NOT NULL DEFAULT NOW()`: o
  Hibernate sempre manda a coluna, anulando o default, então um `save` de `Lead` cru estoura
  `DataIntegrityViolationException`.
- `real_estate_agent.creci` é `unique = true` na entidade mas o `V002` criou a coluna sem `UNIQUE`, e
  `ddl-auto: validate` não checa constraint — dois corretores podem ficar com o mesmo CRECI. A
  única barreira é o `existsByCreci` do serviço, que é checagem de aplicação e corre em corrida.
- `Lead.activities` não pode ser carregada: `Activity` não tem construtor no-arg (o parâmetro
  `lead` não tem default, então o Kotlin não gera o sintético), e a coleção passa pelo
  `CollectionLoaderSingleKey`, que precisa instanciar a entidade pela chave. Nenhum serviço usa a
  coleção — `ActivityService` filtra por `Specification` — então o defeito está latente.
- `ROLE_null`: um `User` sem papel vira a autoridade `"ROLE_null"`. Feio e inofensivo, mas fixado.

### Armadilhas conhecidas da suíte

Coisas que quebram de formas não óbvias e que valem saber antes de escrever o próximo teste:

- **Não dá para meta-anotar o setup de slice.** `@WebMvcTest` já é meta-anotado com
  `@AutoConfigureMockMvc`, e o bootstrapper converte os atributos da anotação em *inlined
  properties*, que vencem `@TestPropertySource` e `application*.yml`. Por isso
  `@AutoConfigureMockMvc(addFilters = false)` vai **direto na classe** de teste: pedir `false` por
  qualquer um dos outros dois caminhos continua devolvendo 302 para o login do Google.
- **`@ImportAutoConfiguration(exclude = ...)` não funciona em `@WebMvcTest`.** O recorte injeta o
  que precisa pelo `ImportsContextCustomizer`, cujo `ImportSelector` ignora exclusões. O que
  desliga a segurança no slice é o `application-test.yml`.
- **Com `addFilters = false`, `@AuthenticationPrincipal Jwt` chega nulo.**
  `SecurityMockMvcRequestPostProcessors.authentication(...)` guarda o contexto num repositório de
  request que o `SecurityContextHolderFilter` só lê. Daí o `support/JwtPrincipal.kt`, que monta o
  `Jwt` na mão.
- **Id preenchido à mão em entidade `@GeneratedValue` quebra o `save`.** O Hibernate trata o objeto
  como *detached*, o `save` vira `merge` e estoura `ObjectOptimisticLockingFailureException`. Por
  isso `TestFixtures.role()` e `id = null` nos repositórios.
- **Ler o campo de uma coleção LAZY não dispara nada.** O `SELECT` só sai na primeira operação que
  toca os elementos, o que faz um bug de carga preguiçosa passar despercebido num teste.
- **`Specification` é `(root, query, cb)`.** A ordem é o inverso do `CriteriaBuilder` e o engano
  compila como "unresolved reference" em `equal`/`like`, não como erro de tipo.
- **Kotlin importa método estático como função de topo**, então `mvc.get("/x")` não resolve —
  use `MockMvcRequestBuilders.request(HttpMethod.GET, "/x")`.
- **Nome de teste em crase não aceita `/` nem `:`.** Custa uma reescrita da função inteira.
- `any()` do mockito-kotlin é `any<T : Any>()` e não casa com `null`; para parâmetro anulável use
  `anyOrNull()`.
- `Jwt.issuer` devolve `URL?`, não `String`.

## Documentação da API

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Actuator: `/actuator/health`, `/actuator/info`, `/actuator/metrics`
