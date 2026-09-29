package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.support.AbstractRepositoryTest
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.util.UUID

/**
 * `LeadRepository` carrega a única chave de negócio do domínio:
 * `origin_lead_id` é o identificador do portal e a idempotência do
 * `LeadService.receiveLead` depende de `existsByOriginLeadId`.
 *
 * `lead.status` tem `CHECK IN ('new','in_progress','converted','lost','archived')`
 * — um teste que passe um status fora da lista pega na hora se essa constraint
 * sumir de uma migration.
 */
class LeadRepositoryTest : AbstractRepositoryTest() {
    @Autowired
    private lateinit var repository: LeadRepository

    private lateinit var agencyId: UUID

    @BeforeEach
    fun setUp() {
        agencyId = insertAgency("Imob Leads")
    }

    /**
     * `lead.created_at` e `lead.updated_at` são `NOT NULL DEFAULT NOW()`, mas o
     * `Lead` não tem `@PrePersist` nenhum — e o Hibernate sempre manda a coluna
     * no `INSERT`, mesmo nula, o que anula o `DEFAULT`. Quem grava lead por
     * JPA precisa preencher os dois na mão.
     *
     * Nada no serviço faz isso hoje (`receiveLead` só dispara e-mail e insight;
     * a tabela é alimentada pelo `insights_app`, outro serviço, direto no
     * Postgres), então é só isso que está registrado: um `save` de `Lead` cru
     * estoura. O comportamento está fixado no teste logo abaixo.
     */
    private fun novo(
        originLeadId: String = "OL-1",
        status: String = "new",
        name: String = "Maria Silva",
        leadOrigin: String = "Zapimoveis",
        temperature: String = "hot",
        source: String? = "portal",
    ): Lead =
        TestFixtures
            .lead(
                id = null,
                originLeadId = originLeadId,
                status = status,
                name = name,
                leadOrigin = leadOrigin,
                temperature = temperature,
            ).apply {
                realEstateAgencyId = agencyId
                this.source = source
                updatedAt = Instant.parse("2026-01-15T10:00:00Z")
            }

    @Test
    fun `save preenche o id e persiste a agência como FK`() {
        val saved = repository.save(novo())

        assertThat(saved.id).isNotNull()
        assertThat(saved.realEstateAgencyId).isEqualTo(agencyId)
    }

    /**
     * Fica registrado porque é uma armadilha do schema, não um detalhe do teste:
     * a coluna tem `DEFAULT NOW()`, então o `INSERT` parece ter tudo certo, mas o
     * Hibernate sobrescreve o default com o `NULL` do campo. Sem `@PrePersist`
     * em `Lead`, o `save` estoura `DataIntegrityViolationException`.
     */
    @Test
    fun `save com updatedAt nulo estoura porque o Hibernate anula o DEFAULT do banco`() {
        val semTimestamp = novo(originLeadId = "OL-sem-data").apply { updatedAt = null }

        assertThatThrownBy { repository.saveAndFlush(semTimestamp) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
            .hasMessageContaining("updated_at")
    }

    @Test
    fun `existsByOriginLeadId distingue lead novo de lead já recebido`() {
        repository.save(novo(originLeadId = "OL-42"))

        assertThat(repository.existsByOriginLeadId("OL-42")).isTrue()
        assertThat(repository.existsByOriginLeadId("OL-99")).isFalse()
    }

    @Test
    fun `findByOriginLeadId devolve o lead e o absent devolve Optional vazio`() {
        repository.save(novo(originLeadId = "OL-7", name = "Joana"))

        val encontrado = repository.findByOriginLeadId("OL-7")

        assertThat(encontrado).isPresent
        assertThat(encontrado.get().name).isEqualTo("Joana")
        assertThat(repository.findByOriginLeadId("OL-inexistente")).isEmpty
    }

    /**
     * `lead.status` tem `CHECK IN ('new','in_progress','converted','lost','archived')`.
     * A validação do `UpdateLeadRequest` no controller cobre metade do problema;
     * isto cobre a outra metade — nada entra no banco com status fora da lista,
     * nem por SQL direto.
     */
    @Test
    fun `status fora do CHECK é recusado pelo próprio banco`() {
        assertThatThrownBy { repository.saveAndFlush(novo(originLeadId = "OL-ruim", status = "nao-existe")) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `status dentro do CHECK é aceito`() {
        repository.saveAndFlush(novo(originLeadId = "OL-ok", status = "converted"))

        assertThat(repository.findByOriginLeadId("OL-ok").get().status).isEqualTo("converted")
    }

    @Test
    fun `Specification por status e origem compõe os dois predicados`() {
        repository.save(novo(originLeadId = "A", status = "new", leadOrigin = "Zapimoveis"))
        repository.save(novo(originLeadId = "B", status = "new", leadOrigin = "Site"))
        repository.save(novo(originLeadId = "C", status = "converted", leadOrigin = "Zapimoveis"))

        val spec = statusSpec("new").and(originSpec("Zapimoveis"))

        assertThat(repository.findAll(spec).map { it.originLeadId }).containsExactly("A")
    }

    @Test
    fun `Specification por temperatura com paginação e ordenação`() {
        repository.save(novo(originLeadId = "A", temperature = "hot"))
        repository.save(novo(originLeadId = "B", temperature = "hot"))
        repository.save(novo(originLeadId = "C", temperature = "cold"))

        val spec = temperatureSpec("hot")
        val pagina = repository.findAll(spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "originLeadId")))

        assertThat(pagina.content.map { it.originLeadId }).containsExactly("A", "B")
        assertThat(pagina.totalElements).isEqualTo(2)
    }

    @Test
    fun `busca textual por nome ignora maiúsculas e minúsculas`() {
        repository.save(novo(originLeadId = "A", name = "Maria Aparecida"))
        repository.save(novo(originLeadId = "B", name = "João"))

        val spec = Specification<Lead> { root, _, cb -> cb.like(cb.lower(root.get<String>("name")), "%maria%") }

        assertThat(repository.findAll(spec).map { it.originLeadId }).containsExactly("A")
    }

    /**
     * `LeadService.findLeads` sempre começa de `cb.conjunction()`, nunca de null
     * — e o Spring Data 4 rejeita null na borda do repositório. Passar null
     * "para pegar tudo" não funciona mais; `findAll(pageable)` é o caminho.
     */
    @Test
    fun `spec nulo é rejeitado pelo JpaSpecificationExecutor`() {
        repository.save(novo(originLeadId = "A"))
        repository.save(novo(originLeadId = "B"))

        assertThatThrownBy { repository.findAll(null as Specification<Lead>?, PageRequest.of(0, 10)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("must not be null")
    }

    @Test
    fun `findAll sem spec devolve a lead inteira`() {
        repository.save(novo(originLeadId = "A"))
        repository.save(novo(originLeadId = "B"))

        assertThat(repository.findAll(PageRequest.of(0, 10)).totalElements).isEqualTo(2)
    }

    @Test
    fun `deleteById remove o lead`() {
        val saved = repository.save(novo())

        repository.deleteById(saved.id!!)

        assertThat(repository.existsById(saved.id!!)).isFalse()
    }

    private fun statusSpec(status: String): Specification<Lead> =
        Specification { root, _, cb -> cb.equal(root.get<String>("status"), status) }

    private fun originSpec(leadOrigin: String): Specification<Lead> =
        Specification { root, _, cb -> cb.equal(root.get<String>("leadOrigin"), leadOrigin) }

    private fun temperatureSpec(temperature: String): Specification<Lead> =
        Specification { root, _, cb -> cb.equal(root.get<String>("temperature"), temperature) }
}
