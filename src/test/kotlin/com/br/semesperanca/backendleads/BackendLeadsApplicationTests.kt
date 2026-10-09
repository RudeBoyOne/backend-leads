package com.br.semesperanca.backendleads

import com.br.semesperanca.backendleads.support.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.web.SecurityFilterChain
import org.springframework.transaction.PlatformTransactionManager

/**
 * Fumaça do contexto completo: sobe a aplicação contra o PostgreSQL do
 * Testcontainers, roda as 13 migrations do Flyway e constrói as duas
 * SecurityFilterChain. Falha aqui significa que o build inteiro está quebrado.
 */
class BackendLeadsApplicationTests : AbstractIntegrationTest() {
    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `contexto sobe com o banco do Testcontainers`() {
        assertThat(context.beanDefinitionCount).isGreaterThan(0)
    }

    @Test
    fun `as duas filter chains de seguranca estao registradas`() {
        val chains = context.getBeansOfType(SecurityFilterChain::class.java).values
        assertThat(chains).hasSize(2)
    }

    @Test
    fun `o gerenciador de transacoes conversa com o PostgreSQL`() {
        assertThat(transactionManager).isNotNull()
        val dataSource = context.getBean(javax.sql.DataSource::class.java)
        dataSource.connection.use { connection ->
            assertThat(connection.metaData.databaseProductName).isEqualTo("PostgreSQL")
        }
    }
}
