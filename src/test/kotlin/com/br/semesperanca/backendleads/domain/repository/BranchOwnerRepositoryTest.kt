package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.BranchOwner
import com.br.semesperanca.backendleads.domain.model.Role
import com.br.semesperanca.backendleads.support.AbstractRepositoryTest
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDateTime
import java.util.UUID

/**
 * `BranchOwner` ganhou a coluna `active` no `V013`, depois de a tabela já estar
 * em produção — os dois filtros que importam são o `active` e a busca por nome,
 * e é isso que o `PropertyService`/`BranchOwnerService` exercem.
 *
 * O `updated_at` é sobrescrito pelo `@PreUpdate` mesmo que o teste passe um
 * valor, então a data antiga não sobrevive ao `save` de uma entidade já
 * gerenciada — comportamento fixado no fim do arquivo.
 */
class BranchOwnerRepositoryTest : AbstractRepositoryTest() {
    @Autowired
    private lateinit var repository: BranchOwnerRepository

    private lateinit var agencyId: UUID

    private lateinit var outraAgencyId: UUID

    /**
     * A `V020` semeia a Marina Reis como branch owner da Filial Centro, então a
     * tabela nunca começa vazia. Os testes que contam comparam com este número em
     * vez de assumirem zero — assim eles continuam válidos se o seed crescer.
     */
    private var semeados: Long = 0

    @BeforeEach
    fun setUp() {
        agencyId = insertAgency("Imob A")
        outraAgencyId = insertAgency("Imob B")
        semeados = repository.count()
    }

    /**
     * `users.email` é `UNIQUE`, e o responsável é um `User`. O e-mail padrão é
     * único por sufixo aleatório de propósito: vários `novo()` no mesmo teste
     *colidiriam no índice único, e a falha apareceria como `users_email_key`
     * em vez de um nome duplicado, que é o que o teste está checando.
     */
    private fun novo(
        agency: UUID = agencyId,
        name: String = "Carlos Souza",
        email: String? = "carlos-${UUID.randomUUID()}@example.com",
        phone: String? = "11912345678",
        isActive: Boolean = true,
    ): BranchOwner =
        TestFixtures.branchOwner(
            id = null,
            realEstateAgencyId = agency,
            name = name,
            email = email,
            phone = phone,
            // `users.role_id` é NOT NULL. O responsável carrega ADMIN — é essa role
            // que faz `hasRole("ADMIN")` significar "branch owner" na autorização.
            role = insertRole(Role.ADMIN),
            isActive = isActive,
            createdAt = null,
        ).apply { updatedAt = null }

    @Test
    fun `save preenche o id e os dois timestamps`() {
        val saved = repository.save(novo())

        assertThat(saved.id).isNotNull()
        assertThat(saved.createdAt).isNotNull()
        assertThat(saved.updatedAt).isNotNull()
    }

    @Test
    fun `filtro por agência devolve só os donos dela`() {
        repository.save(novo(name = "Carlos"))
        repository.save(novo(name = "Fernanda"))
        repository.save(novo(agency = outraAgencyId, name = "De outra"))

        val spec = Specification<BranchOwner> { root, _, cb -> cb.equal(root.get<UUID>("realEstateAgencyId"), agencyId) }

        assertThat(repository.findAll(spec).map { it.name }).containsExactlyInAnyOrder("Carlos", "Fernanda")
    }

    /**
     * A agency entra no filtro junto com `active`, e não por acaso: o `V020`
     * semeia Marina Reis como responsável ativo, então um `containsExactly("Ativo")`
     * sem filtro de agência veria duas linhas e o teste passaria a falhar por causa
     * de uma seed, não por causa do filtro.
     */
    @Test
    fun `filtro por active esconde o desativado`() {
        repository.save(novo(name = "Ativo", isActive = true))
        repository.save(novo(name = "Inativo", isActive = false))

        val spec =
            Specification<BranchOwner> { root, _, cb ->
                cb.and(
                    cb.equal(root.get<UUID>("realEstateAgencyId"), agencyId),
                    cb.isTrue(root.get("isActive")),
                )
            }

        assertThat(repository.findAll(spec).map { it.name }).containsExactly("Ativo")
    }

    @Test
    fun `busca textual por nome e telefone compõe os dois campos`() {
        repository.save(novo(name = "Carlos Souza", phone = "11912345678"))
        repository.save(novo(name = "Fernanda Lima", phone = "11999998888"))

        val spec =
            Specification<BranchOwner> { root, query, cb ->
                val pattern = "%" + "carlos".lowercase() + "%"
                query.distinct(true)
                cb.or(
                    cb.like(cb.lower(root.get<String>("name")), pattern),
                    cb.like(cb.lower(root.get<String>("phone")), pattern),
                )
            }

        assertThat(repository.findAll(spec).map { it.name }).containsExactly("Carlos Souza")
    }

    @Test
    fun `Specification que nenhum dono satisfaz devolve página vazia`() {
        repository.save(novo(name = "Carlos"))

        val spec = Specification<BranchOwner> { root, _, cb -> cb.equal(root.get<String>("name"), "Inexistente") }

        assertThat(repository.findAll(spec)).isEmpty()
    }

    @Test
    fun `o preUpdate sobrescreve o updatedAt que já vinha preenchido`() {
        val antigo = LocalDateTime.of(2020, 1, 1, 8, 0)
        val salvo = repository.save(novo().apply { updatedAt = antigo })
        entityManager.flush()
        entityManager.clear()

        val relido = repository.findById(salvo.id!!).get()
        relido.phone = "11900001111"
        val atualizado = repository.save(relido)
        entityManager.flush()
        entityManager.clear()

        val final = repository.findById(salvo.id!!).get()
        assertThat(final.phone).isEqualTo("11900001111")
        assertThat(final.updatedAt).isNotNull().isAfter(antigo)
        assertThat(atualizado.id).isEqualTo(salvo.id)
    }

    @Test
    fun `excluir a agência leva os donos junto por causa do ON DELETE CASCADE`() {
        repository.save(novo())

        entityManager.createNativeQuery("DELETE FROM real_estate_agency WHERE id = :id").setParameter("id", agencyId).executeUpdate()
        entityManager.flush()
        entityManager.clear()

        // A cascata vem de `users.real_estate_agency_id ON DELETE CASCADE`, e o
        // responsável é uma linha de `users`: ela morre com a agência.
        assertThat(repository.count()).isEqualTo(semeados)
    }

    @Test
    fun `count e existsById refletem o estado real da tabela`() {
        assertThat(repository.count()).isEqualTo(semeados)

        val salvo = repository.save(novo())
        entityManager.flush()

        assertThat(repository.count()).isEqualTo(semeados + 1)
        assertThat(repository.existsById(salvo.id!!)).isTrue()
    }
}
