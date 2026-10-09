package com.br.semesperanca.backendleads.support

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * PostgreSQL único para a suíte inteira.
 *
 * Duas decisões, ambas forçadas pelo Spring TestContext Framework:
 *
 * **Um container só.** Os testes de repositório (`@DataJpaTest`) e os de
 * contexto completo (`@SpringBootTest`) usam contextos diferentes, e o cache de
 * contexto não é compartilhado entre eles — um container por hierarquia custaria
 * subir o PostgreSQL duas vezes. O campo fica no `companion` desta base, que é
 * uma classe só no bytecode, então as duas hierarquias enxergam a mesma
 * instância.
 *
 * **Ciclo de vida fora do JUnit.** A anotação `@Testcontainers`/@`@Container`
 * para o container no `afterAll` de cada classe de teste — com a instância
 * compartilhada, a primeira classe a terminar mataria o banco das próximas. Por
 * isso o container é iniciado aqui, sob demanda, e ninguém o para: o Ryuk do
 * Testcontainers reaproveita no encerramento da JVM.
 *
 * Static de propósito — o `ServiceConnectionContextCustomizerFactory` lê o campo
 * `@ServiceConnection` ao montar a chave de cache do contexto, ou seja, no
 * `beforeTestClass`, muito antes de existir uma instância de teste.
 */
abstract class AbstractPostgresTest {
    companion object {
        @JvmField
        @ServiceConnection
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:16-alpine").also { it.start() }
    }
}
