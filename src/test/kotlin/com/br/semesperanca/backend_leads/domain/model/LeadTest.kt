package com.br.semesperanca.backendleads.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Teste de unidade da entidade [Lead] (camada de dominio): callbacks de ciclo de vida.
 * Sem mocks: a entidade e instanciada diretamente e os @Test sao os drivers que
 * chamam prePersist/preUpdate (o JPA os chamaria em producao).
 */
class LeadTest {
    @Test
    fun `prePersist preenche createdAt e updatedAt quando nulos`() {
        val lead = Lead()

        lead.prePersist()

        assertNotNull(lead.createdAt)
        assertNotNull(lead.updatedAt)
        assertEquals(lead.createdAt, lead.updatedAt)
    }

    @Test
    fun `prePersist preserva createdAt e updatedAt ja informados`() {
        val criado = Instant.parse("2025-01-01T00:00:00Z")
        val atualizado = Instant.parse("2025-06-01T00:00:00Z")
        val lead = Lead(createdAt = criado, updatedAt = atualizado)

        lead.prePersist()

        assertEquals(criado, lead.createdAt)
        assertEquals(atualizado, lead.updatedAt)
    }

    @Test
    fun `prePersist preenche apenas o campo que estiver nulo`() {
        val criado = Instant.parse("2025-01-01T00:00:00Z")
        val lead = Lead(createdAt = criado, updatedAt = null)

        lead.prePersist()

        assertEquals(criado, lead.createdAt)
        assertNotNull(lead.updatedAt)
    }

    @Test
    fun `preUpdate atualiza updatedAt`() {
        val antigo = Instant.parse("2025-01-01T00:00:00Z")
        val lead = Lead(updatedAt = antigo)

        lead.preUpdate()

        assertTrue(lead.updatedAt!!.isAfter(antigo))
    }

    @Test
    fun `lead novo nasce sem insightId e sem atividades`() {
        val lead = Lead()

        assertEquals(null, lead.insightId)
        assertTrue(lead.activities.isEmpty())
    }
}
