package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.support.AbstractRepositoryTest
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.util.UUID

/**
 * `role` é a única tabela que o `V001` popula. O teste garante que o seed
 * sobreviveu às 13 migrations e que o `findByName` — usado pelo
 * `OAuth2SuccessHandler` e pelos serviços de usuário — resolve.
 */
class RoleRepositoryTest : AbstractRepositoryTest() {
    @Autowired
    private lateinit var repository: RoleRepository

    @Test
    fun `o seed do V001 criou os seis papéis do sistema`() {
        val names = repository.findAll().map { it.name }.toSet()

        assertThat(names).containsExactlyInAnyOrder(
            "FULL_ADMIN",
            "ADMIN",
            "MANAGER",
            "REAL_ESTATE_AGENT",
            "TEAM_AGENT",
            "USER",
        )
    }

    @Test
    fun `findByName acha o papel pelo nome exato`() {
        val role = repository.findByName("FULL_ADMIN")

        assertThat(role).isPresent
        assertThat(role.get().id).isNotNull()
    }

    @Test
    fun `findByName com nome desconhecido devolve vazio e nao exception`() {
        assertThat(repository.findByName("NAO_EXISTE")).isEmpty
    }

    @Test
    fun `findByName diferencia maiusculas de minusculas`() {
        assertThat(repository.findByName("manager")).isEmpty
    }

    @Test
    fun `save persiste e o id gerado volta preenchido`() {
        val saved = repository.save(TestFixtures.role(name = "AUDITOR"))

        assertThat(saved.id).isNotNull()
        assertThat(repository.findByName("AUDITOR")).isPresent
    }

    /**
     * `Role` é `@GeneratedValue`: passar um id à mão faz o `save` virar `merge` e
     * o Hibernate procurar uma linha que não existe. A armadilha está documentada
     * em `TestFixtures.role` e vale para toda entidade com id gerado.
     */
    @Test
    fun `save com id preenchido à mão falha porque o Hibernate trata como detached`() {
        val detached = TestFixtures.role(name = "FANTASMA", id = UUID.randomUUID())

        assertThatThrownBy { repository.save(detached) }
            .isInstanceOf(ObjectOptimisticLockingFailureException::class.java)
    }
}
