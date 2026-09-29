package com.br.semesperanca.backendleads.domain.model

import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Os callbacks de ciclo de vida sao `@PrePersist`/`@PreUpdate` — so o JPA os
 * chama, no flush. Aqui sao chamados direto, que e o que o provider faz. O que
 * interessa e a regra de cada um, e sobretudo a diferenca entre eles:
 *
 * - `User` sobrescreve `createdAt` sempre, mesmo num `@PrePersist` de objeto que
 *   ja tinha timestamp;
 * - `Property`, `BranchOwner` e `Activity` so preenchem `createdAt` quando ele
 *   ainda e nulo.
 */
class EntityLifecycleTest {
    private val ANTES = LocalDateTime.now().minusSeconds(1)

    private val ANTIGO = LocalDateTime.of(2020, 1, 1, 8, 0)

    @Test
    fun `User prePersist sobrescreve createdAt mesmo se ja vinher preenchido`() {
        val user = TestFixtures.realEstateAgent(createdAt = ANTIGO)

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
        val branchOwner = TestFixtures.branchOwner(createdAt = ANTIGO, updatedAt = ANTIGO)

        branchOwner.prePersist()

        assertThat(branchOwner.createdAt).isEqualTo(ANTIGO)
        assertThat(branchOwner.updatedAt).isAfter(ANTES)
    }

    @Test
    fun `BranchOwner prePersist preenche createdAt quando vem nulo`() {
        val branchOwner = TestFixtures.branchOwner(createdAt = null, updatedAt = null)

        branchOwner.prePersist()

        assertThat(branchOwner.createdAt).isAfter(ANTES)
        assertThat(branchOwner.updatedAt).isAfter(ANTES)
    }

    @Test
    fun `BranchOwner preUpdate so mexe em updatedAt`() {
        val branchOwner = TestFixtures.branchOwner(createdAt = ANTIGO, updatedAt = ANTIGO)

        branchOwner.preUpdate()

        assertThat(branchOwner.createdAt).isEqualTo(ANTIGO)
        assertThat(branchOwner.updatedAt).isAfter(ANTES)
    }
}
