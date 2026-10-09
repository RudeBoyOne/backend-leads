package com.br.semesperanca.backendleads.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Base dos testes que sobem o **contexto completo** contra o PostgreSQL do
 * Testcontainers: as 13 migrations do Flyway rodam, os dois `SecurityFilterChain`
 * são construídos e todo bean real existe.
 *
 * O profile "test" (`src/test/resources/application-test.yml`) resolve os
 * placeholders restantes: `JWT_SECRET`, `GOOGLE_*`, `OAUTH2_ISSUER_URI`,
 * `MAIL_*`. Sem ele o `application.yml` principal nem interpola.
 *
 * O container está em [AbstractPostgresTest], compartilhado com os testes de
 * repositório.
 */
@SpringBootTest
@ActiveProfiles("test")
abstract class AbstractIntegrationTest : AbstractPostgresTest()
