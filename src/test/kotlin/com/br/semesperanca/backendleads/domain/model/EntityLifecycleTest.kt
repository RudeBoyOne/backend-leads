package com.br.semesperanca.backendleads.domain.model

import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime

/**
 * Os callbacks de ciclo de vida sao `@PrePersist`/`@PreUpdate` — so o JPA os
 * chama, no flush. Aqui sao chamados direto, que e o que o provider faz. O que
 * interessa e a regra de cada um, e sobretudo a diferenca entre eles:
 *
 * - `User` sobrescreve `createdAt` sempre, mesmo num `@PrePersist` de objeto que
 *   ja tinha timestamp;
 * - `Property`, `BranchOwner`, `Activity` e `Lead` so preenchem `createdAt`
 *   quando ele ainda e nulo.
 *
 * `Lead` e a unica entidade com `Instant` no lugar de `LocalDateTime`, porque
 * a coluna e `TIMESTAMPTZ` — os callbacks usam `Instant.now()` por causa disso.
 */
class EntityLifecycleTest {
    private val ANTES = LocalDateTime.now().minusSeconds(1)

    private val ANTIGO = LocalDateTime.of(2020, 1, 1, 8, 0)

    private val ANTES_INSTANT = Instant.now().minusSeconds(1)

    private val ANTIGO_INSTANT = Instant.parse("2020-01-01T08:00:00Z")

    /**
     * `createdAt` e `updatable = false`: sobrescrevê-lo no `@PrePersist` faria o
     * banco recusar a linha, ou o Hibernate guardar uma data diferente da que a
     * Migration gravou. O callback só preenche quando vem nulo, como em `Property`
     * e `BranchOffice`, e é isso que o teste fixa.
     */
    @Test
    fun `User prePersist preserva createdAt e renova updatedAt`() {
        val user = TestFixtures.realEstateAgent(createdAt = ANTIGO)

        user.prePersist()

        assertThat(user.createdAt).isEqualTo(ANTIGO)
        assertThat(user.updatedAt).isAfter(ANTES)
    }

    @Test
    fun `User prePersist preenche createdAt quando vem nulo`() {
        val user = TestFixtures.realEstateAgent().apply { createdAt = null }

        user.prePersist()

        assertThat(user.createdAt).isAfter(ANTES)
        assertThat(user.updatedAt).isAfter(ANTES)
    }

    @Test
    fun `User preUpdate so mexe em updatedAt`() {
        val user = TestFixtures.realEstateAgent(createdAt = ANTIGO).apply { updatedAt = ANTIGO }

        user.preUpdate()

        assertThat(user.createdAt).isEqualTo(ANTIGO)
        assertThat(user.updatedAt).isAfter(ANTES)
    }

    @Test
    fun `Property prePersist preserva um createdAt que ja veio preenchido`() {
        val property = TestFixtures.property(createdAt = ANTIGO, updatedAt = ANTIGO)

        property.prePersist()

        assertThat(property.createdAt).isEqualTo(ANTIGO)
        assertThat(property.updatedAt).isAfter(ANTES)
    }

    @Test
    fun `Property prePersist preenche createdAt quando vem nulo`() {
        val property = TestFixtures.property(createdAt = null, updatedAt = null)

        property.prePersist()

        assertThat(property.createdAt).isAfter(ANTES)
        assertThat(property.updatedAt).isAfter(ANTES)
    }

    @Test
    fun `Property preUpdate so mexe em updatedAt`() {
        val property = TestFixtures.property(createdAt = ANTIGO, updatedAt = ANTIGO)

        property.preUpdate()

        assertThat(property.createdAt).isEqualTo(ANTIGO)
        assertThat(property.updatedAt).isAfter(ANTES)
    }

    @Test
    fun `Activity so tem prePersist e preserva um createdAt que ja veio preenchido`() {
        val activity = TestFixtures.activity(createdAt = ANTIGO)

        activity.prePersist()

        assertThat(activity.createdAt).isEqualTo(ANTIGO)
    }

    @Test
    fun `Activity prePersist preenche createdAt quando vem nulo`() {
        val activity = TestFixtures.activity(createdAt = null)

        activity.prePersist()

        assertThat(activity.createdAt).isAfter(ANTES)
    }

    @Test
    fun `BranchOwner prePersist preserva um createdAt que ja veio preenchido`() {
        val branchOwner = TestFixtures.branchOwner(createdAt = ANTIGO).apply { updatedAt = ANTIGO }

        branchOwner.prePersist()

        assertThat(branchOwner.createdAt).isEqualTo(ANTIGO)
        assertThat(branchOwner.updatedAt).isAfter(ANTES)
    }

    @Test
    fun `BranchOwner prePersist preenche createdAt quando vem nulo`() {
        val branchOwner = TestFixtures.branchOwner(createdAt = null).apply { updatedAt = null }

        branchOwner.prePersist()

        assertThat(branchOwner.createdAt).isAfter(ANTES)
        assertThat(branchOwner.updatedAt).isAfter(ANTES)
    }

    @Test
    fun `BranchOwner preUpdate so mexe em updatedAt`() {
        val branchOwner = TestFixtures.branchOwner(createdAt = ANTIGO).apply { updatedAt = ANTIGO }

        branchOwner.preUpdate()

        assertThat(branchOwner.createdAt).isEqualTo(ANTIGO)
        assertThat(branchOwner.updatedAt).isAfter(ANTES)
    }

    @Test
    fun `Lead prePersist preserva um createdAt que ja veio preenchido e so completa o updatedAt`() {
        val lead = TestFixtures.lead(createdAt = ANTIGO_INSTANT)

        lead.prePersist()

        assertThat(lead.createdAt).isEqualTo(ANTIGO_INSTANT)
        assertThat(lead.updatedAt).isAfter(ANTES_INSTANT)
    }

    @Test
    fun `Lead prePersist preenche createdAt e updatedAt quando vem nulo`() {
        val lead = TestFixtures.lead(createdAt = null).apply { updatedAt = null }

        lead.prePersist()

        assertThat(lead.createdAt).isAfter(ANTES_INSTANT)
        assertThat(lead.updatedAt).isAfter(ANTES_INSTANT)
    }

    @Test
    fun `Lead preUpdate so mexe em updatedAt`() {
        val lead = TestFixtures.lead(createdAt = ANTIGO_INSTANT).apply { updatedAt = ANTIGO_INSTANT }

        lead.preUpdate()

        assertThat(lead.createdAt).isEqualTo(ANTIGO_INSTANT)
        assertThat(lead.updatedAt).isAfter(ANTES_INSTANT)
    }
}
